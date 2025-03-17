package movies;

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
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "movies.CartServlet", urlPatterns = "/api/cart")
public class CartServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb_slave");
            System.out.println("Got cart datasource");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        String sessionId = session.getId();
        long lastAccessTime = session.getLastAccessedTime();

        Map<String, Integer> quantities = (Map<String, Integer>) session.getAttribute("quantities");
        if (quantities == null) {
            quantities = new HashMap<>();
        }

        session.setAttribute("quantities", quantities);

        PriceResult priceResult = calculateTotalPrice(quantities);
        JsonObject responseJsonObject = getJsonObject(priceResult, quantities);

        session.setAttribute("totalPrice", priceResult.getTotalPrice());

        responseJsonObject.addProperty("sessionID", sessionId);
        responseJsonObject.addProperty("lastAccessTime", lastAccessTime);

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.write(responseJsonObject.toString());
        out.close();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String item = request.getParameter("movieId");
        Integer quantity = Integer.parseInt(request.getParameter("quantity") == null ? "1" : request.getParameter("quantity"));
        System.out.println("Adding item: " + item + " with quantity: " + quantity);
        HttpSession session = request.getSession();

        Map<String, Integer> quantities = (Map<String, Integer>) session.getAttribute("quantities");
        if (quantities == null) {
            quantities = new HashMap<>();
        }

        synchronized (quantities) { // Ensure thread safety
            quantities.put(item, quantities.getOrDefault(item, 0) + quantity);
        }

        if (quantities.get(item) <= 0) {
            quantities.remove(item);
        }
        session.setAttribute("quantities", quantities);

        PriceResult priceResult = calculateTotalPrice(quantities);
        JsonObject responseJsonObject = getJsonObject(priceResult, quantities);

        session.setAttribute("totalPrice", priceResult.getTotalPrice());

        // Write the response
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.write(responseJsonObject.toString());
        out.close();
    }

    private static JsonObject getJsonObject(PriceResult priceResult, Map<String, Integer> quantities) {
        double total = priceResult.getTotalPrice();
        Map<String, Double> pricesMap = priceResult.getPricesMap();
        Map<String, String> namesMap = priceResult.getNamesMap();

        JsonObject responseJsonObject = new JsonObject();
        JsonObject moviesJsonObject = new JsonObject();
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            moviesJsonObject.addProperty(entry.getKey(), entry.getValue());
        }
        responseJsonObject.add("quantities", moviesJsonObject);

        JsonObject pricesJsonObject = new JsonObject();
        for (Map.Entry<String, Double> entry : pricesMap.entrySet()) {
            pricesJsonObject.addProperty(entry.getKey(), entry.getValue());
        }
        responseJsonObject.add("prices", pricesJsonObject);

        JsonObject namesJsonObject = new JsonObject();
        for (Map.Entry<String, String> entry : namesMap.entrySet()) {
            namesJsonObject.addProperty(entry.getKey(), entry.getValue());
        }
        responseJsonObject.add("names", namesJsonObject);

        responseJsonObject.addProperty("total", total);
        return responseJsonObject;
    }

    private PriceResult calculateTotalPrice(Map<String, Integer> cart) {
        double total = 0.0;
        Map<String, Double> pricesMap = new HashMap<>();
        Map<String, String> namesMap = new HashMap<>();

        if (cart.isEmpty()) {
            return new PriceResult(total, pricesMap, namesMap);
        }
        try (Connection conn = dataSource.getConnection()) {
            // Build a query to fetch prices for all movies in the cart
            StringBuilder queryBuilder = new StringBuilder("SELECT id, price, title FROM movies WHERE id IN (");
            for (int i = 0; i < cart.size(); i++) {
                queryBuilder.append("?");
                if (i < cart.size() - 1) {
                    queryBuilder.append(",");
                }
            }
            queryBuilder.append(")");

            PreparedStatement preparedStatement = conn.prepareStatement(queryBuilder.toString());
            int index = 1;
            for (String movieId : cart.keySet()) {
                preparedStatement.setString(index++, movieId);
            }

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String movieId = resultSet.getString("id");
                double price = resultSet.getDouble("price");
                String title = resultSet.getString("title");
                int quantity = cart.get(movieId);
                total += price * quantity;
                pricesMap.put(movieId, price);
                namesMap.put(movieId, title);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate total price", e);
        }
        return new PriceResult(total, pricesMap, namesMap);
    }

    private static class PriceResult {
        private final double totalPrice;
        private final Map<String, Double> pricesMap;
        private final Map<String, String> namesMap;

        public PriceResult(double totalPrice, Map<String, Double> pricesMap, Map<String, String> namesMap) {
            this.totalPrice = totalPrice;
            this.pricesMap = pricesMap;
            this.namesMap = namesMap;
        }

        public double getTotalPrice() {
            return totalPrice;
        }

        public Map<String, Double> getPricesMap() {
            return pricesMap;
        }

        public Map<String, String> getNamesMap() {
            return namesMap;
        }
    }
}