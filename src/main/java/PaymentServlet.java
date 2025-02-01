import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
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
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
            System.out.println("DataSource initialized successfully.");
        } catch (NamingException e) {
            e.printStackTrace();
            System.err.println("Failed to initialize DataSource: " + e.getMessage());
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession();
        String sessionId = session.getId();

        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String cc = request.getParameter("cc");
        String date = request.getParameter("date");

        Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute("quantities");
        if (cart == null || cart.isEmpty()) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", "Your cart is empty.");
            out.write(jsonObject.toString());
            response.setStatus(400); // Bad request
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT * FROM creditcards WHERE id = ? AND firstName = ? AND lastName = ? AND expiration = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, cc);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setString(4, date);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("Credit card validated successfully.");
                String customerQuery = "SELECT id FROM customers WHERE ccId = ?";
                PreparedStatement customerPs = conn.prepareStatement(customerQuery);
                customerPs.setString(1, cc);
                ResultSet customerRs = customerPs.executeQuery();

                if (customerRs.next()) {
                    int customerId = customerRs.getInt("id");

                    String insertQuery = "INSERT INTO sales (customerId, movieId, quantity, saleDate) VALUES (?, ?, ?, NOW())";
                    PreparedStatement insertPs = conn.prepareStatement(insertQuery);

                    for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                        String movieId = entry.getKey();
                        int quantity = entry.getValue();

                        insertPs.setInt(1, customerId);
                        insertPs.setString(2, movieId);
                        insertPs.setInt(3, quantity);
                        insertPs.addBatch();
                    }

                    insertPs.executeBatch();

                    session.removeAttribute("quantities");

                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("status", "success");
                    jsonObject.addProperty("message", "Order placed successfully!");
                    out.write(jsonObject.toString());
                } else {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("errorMessage", "No customer found for the provided credit card.");
                    out.write(jsonObject.toString());
                    response.setStatus(400);
                }
            } else {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("errorMessage", "Invalid credit card information. Please try again.");
                out.write(jsonObject.toString());
                response.setStatus(400);
            }
        } catch (Exception e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            request.getServletContext().log("Error:", e);
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}
