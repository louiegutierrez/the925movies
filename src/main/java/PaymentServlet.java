import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

@WebServlet(name = "PaymentServlet", urlPatterns = "/api/payment")
public class PaymentServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb_master");
            System.out.println("DataSource initialized successfully.");
        } catch (NamingException e) {
            e.printStackTrace();
            System.err.println("Failed to initialize DataSource: " + e.getMessage());
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession();

        Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute("quantities");
        if (cart == null || cart.isEmpty()) {
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("errorMessage", "Your cart is empty.");
            out.write(errorJson.toString());
            response.setStatus(400);
            return;
        }

        Object totalObj = session.getAttribute("totalPrice");
        if (totalObj == null) {
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("errorMessage", "No total price found in session.");
            out.write(errorJson.toString());
            response.setStatus(400);
            return;
        }

        double totalPrice = (double) totalObj;


        Object custObj = session.getAttribute("user");
        if (custObj == null) {
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("errorMessage", "No customer ID found in session.");
            out.write(errorJson.toString());
            response.setStatus(400);
            return;
        }
        int customerId = (int) custObj;

        JsonObject responseJson = new JsonObject();
        JsonArray salesArray = new JsonArray();

        System.out.println(customerId);
        System.out.println(cart);

        try (Connection conn = dataSource.getConnection()) {
            String lastSaleQuery =
                    "SELECT s.id AS sale_id, m.title AS movie_title, s.quantity " +
                            "FROM sales s " +
                            "JOIN movies m ON s.movieId = m.id " +
                            "WHERE s.customerId = ? AND s.movieId = ? " +
                            "ORDER BY s.id DESC " +
                            "LIMIT 1";
            System.out.println("Query: " + lastSaleQuery);
            try (PreparedStatement ps = conn.prepareStatement(lastSaleQuery)) {
                for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                    String movieId = entry.getKey();
                    ps.setInt(1, customerId);
                    ps.setString(2, movieId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int saleId = rs.getInt("sale_id");
                            String movieTitle = rs.getString("movie_title");
                            int quantity = rs.getInt("quantity");
                            JsonObject saleJson = new JsonObject();
                            saleJson.addProperty("saleID", saleId);
                            saleJson.addProperty("movieTitle", movieTitle);
                            saleJson.addProperty("quantity", quantity);
                            salesArray.add(saleJson);
                        } else {
                            System.out.println("no sale found!????");
                        }
                    }
                }
            }

            responseJson.add("sales", salesArray);
            responseJson.addProperty("totalPrice", totalPrice);

            // Remove cart and totalPrice from the session
            session.removeAttribute("quantities");
            session.removeAttribute("totalPrice");


            responseJson.addProperty("status", "success");
            out.write(responseJson.toString());
            response.setStatus(200);

        } catch (SQLException e) {
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("errorMessage", "SQL error: " + e.getMessage());
            out.write(errorJson.toString());
            request.getServletContext().log("Error during payment retrieval:", e);
            response.setStatus(500);
        } catch (Exception e) {
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("errorMessage", "An unexpected error occurred. Please try again later.");
            out.write(errorJson.toString());
            request.getServletContext().log("Error during payment retrieval:", e);
            response.setStatus(500);
        } finally {
            out.close();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession();

        String firstName = request.getParameter("first_name");
        String lastName = request.getParameter("last_name");
        String cc = request.getParameter("card_number");
        String date = request.getParameter("expiration_date");

        System.out.println(firstName + " " + lastName + " " + cc + " " + date);

        Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute("quantities");
        if (cart == null || cart.isEmpty()) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", "Your cart is empty.");
            out.write(jsonObject.toString());
            response.setStatus(400); // Bad request
            return;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM creditcards WHERE id = ? AND firstName = ? AND lastName = ? AND expiration = ?");
             PreparedStatement customerPs = conn.prepareStatement("SELECT id FROM customers WHERE ccId = ?")) {

            // Validate credit card
            ps.setString(1, cc);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setString(4, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Credit card validated successfully.");

                    // Find customer ID
                    customerPs.setString(1, cc);
                    try (ResultSet customerRs = customerPs.executeQuery()) {
                        if (customerRs.next()) {
                            int customerId = customerRs.getInt("id");

                            // Insert sales records
                            String insertQuery = "INSERT INTO sales (customerId, movieId, quantity, saleDate) VALUES (?, ?, ?, NOW())";
                            try (PreparedStatement insertPs = conn.prepareStatement(insertQuery)) {
                                for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                                    String movieId = entry.getKey();
                                    int quantity = entry.getValue();

                                    insertPs.setInt(1, customerId);
                                    insertPs.setString(2, movieId);
                                    insertPs.setInt(3, quantity);
                                    insertPs.addBatch();
                                }

                                int[] batchResults = insertPs.executeBatch();
                                for (int result : batchResults) {
                                    if (result == PreparedStatement.EXECUTE_FAILED) {
                                        throw new SQLException("Failed to insert one or more sales records.");
                                    }
                                }

                                JsonObject jsonObject = new JsonObject();
                                jsonObject.addProperty("status", "success");
                                jsonObject.addProperty("message", "Order placed successfully!");
                                jsonObject.addProperty("customerID", "Order placed successfully!");
                                out.write(jsonObject.toString());
                            }
                        } else {
                            JsonObject jsonObject = new JsonObject();
                            jsonObject.addProperty("errorMessage", "No customer found for the provided credit card.");
                            out.write(jsonObject.toString());
                            response.setStatus(400);
                        }
                    }
                } else {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("errorMessage", "Invalid credit card information. Please try again.");
                    out.write(jsonObject.toString());
                    response.setStatus(400);
                }
            }
        } catch (Exception e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", "An unexpected error occurred. Please try again later.");
            out.write(jsonObject.toString());

            // Log the full error on the server side
            request.getServletContext().log("Error during payment processing:", e);
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}