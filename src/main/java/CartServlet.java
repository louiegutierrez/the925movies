import com.google.gson.JsonArray;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "CartServlet", urlPatterns = "/api/cart")
public class CartServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
            System.out.println("Got cart datasource");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        String sessionId = session.getId();
        long lastAccessTime = session.getLastAccessedTime();

        JsonObject responseJsonObject = new JsonObject();
        responseJsonObject.addProperty("sessionID", sessionId);
        responseJsonObject.addProperty("lastAccessTime", new Date(lastAccessTime).toString());

        Map<String, Integer> previousMovies = (Map<String, Integer>) session.getAttribute("previousMovies");
        if (previousMovies == null) {
            previousMovies = new HashMap<>();
        }

        // Log to localhost log
        System.out.println("getting " + previousMovies.size() + " movies");

        // Calculate total price and fetch prices
        PriceResult priceResult = calculateTotalPrice(previousMovies);
        double totalPrice = priceResult.getTotalPrice();
        Map<String, Double> moviePrices = priceResult.getMoviePrices();

        // Add movie prices and total price to the response
        JsonObject pricesJsonObject = new JsonObject();
        for (Map.Entry<String, Double> entry : moviePrices.entrySet()) {
            pricesJsonObject.addProperty(entry.getKey(), entry.getValue());
        }
        responseJsonObject.add("prices", pricesJsonObject);
        responseJsonObject.addProperty("total", totalPrice);

        // Write the response
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

        Map<String, Integer> previousMovies = (Map<String, Integer>) session.getAttribute("previousMovies");
        if (previousMovies == null) {
            previousMovies = new HashMap<>();
        }

        synchronized (previousMovies) { // Ensure thread safety
            previousMovies.put(item, previousMovies.getOrDefault(item, 0) + quantity);
        }

        session.setAttribute("previousMovies", previousMovies);

        PriceResult priceResult = calculateTotalPrice(previousMovies);
        double totalPrice = priceResult.getTotalPrice();
        Map<String, Double> moviePrices = priceResult.getMoviePrices();

        JsonObject responseJsonObject = new JsonObject();
        JsonObject moviesJsonObject = new JsonObject();
        for (Map.Entry<String, Integer> entry : previousMovies.entrySet()) {
            moviesJsonObject.addProperty(entry.getKey(), entry.getValue());
        }
        responseJsonObject.add("previousMovies", moviesJsonObject);

        // Add movie prices and total price to the response
        JsonObject pricesJsonObject = new JsonObject();
        for (Map.Entry<String, Double> entry : moviePrices.entrySet()) {
            pricesJsonObject.addProperty(entry.getKey(), entry.getValue());
        }
        responseJsonObject.add("prices", pricesJsonObject);
        responseJsonObject.addProperty("total", totalPrice);

        // Write the response
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.write(responseJsonObject.toString());
        out.close();
    }

    private PriceResult calculateTotalPrice(Map<String, Integer> cart) {
        double total = 0.0;
        Map<String, Double> moviePrices = new HashMap<>();

        if (cart.isEmpty()) {
            return new PriceResult(total, moviePrices);
        }

        try (Connection conn = dataSource.getConnection()) {
            // Build a query to fetch prices for all movies in the cart
            StringBuilder queryBuilder = new StringBuilder("SELECT id, price FROM movies WHERE id IN (");
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
                int quantity = cart.get(movieId);
                total += price * quantity;
                moviePrices.put(movieId, price);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate total price", e);
        }
        return new PriceResult(total, moviePrices);
    }

    private static class PriceResult {
        private final double totalPrice;
        private final Map<String, Double> moviePrices;

        public PriceResult(double totalPrice, Map<String, Double> moviePrices) {
            this.totalPrice = totalPrice;
            this.moviePrices = moviePrices;
        }

        public double getTotalPrice() {
            return totalPrice;
        }

        public Map<String, Double> getMoviePrices() {
            return moviePrices;
        }
    }
}