
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
        System.out.println("getting" + previousMovies.size() + " movies");

        JsonObject moviesJsonObject = new JsonObject();
        for (Map.Entry<String, Integer> entry : previousMovies.entrySet()) {
            moviesJsonObject.addProperty(entry.getKey(), entry.getValue());
        }

        responseJsonObject.add("previousMovies", moviesJsonObject);

        // query
        System.out.println("getting movie price");
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT price FROM movies WHERE id = ?";
            JsonArray pricesJsonArray = new JsonArray();

            for (Map.Entry<String, Integer> entry : previousMovies.entrySet()) {
                PreparedStatement preparedStatement = conn.prepareStatement(query);
                preparedStatement.setString(1, entry.getKey());
                ResultSet resultSet = preparedStatement.executeQuery();
                resultSet.next();
                pricesJsonArray.add(resultSet.getDouble("price"));
            }
            responseJsonObject.add("prices", pricesJsonArray);
            out.write(responseJsonObject.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            out.close();
        }

    }

    /**
     * handles POST requests to add and show the item list information
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String item = request.getParameter("movieId");
        Integer quantity = Integer.parseInt(request.getParameter("quantity") == null ? "1" : request.getParameter("quantity"));
        System.out.println(item);
        HttpSession session = request.getSession();

        Map<String, Integer> previousMovies = (Map<String, Integer>) session.getAttribute("previousMovies");
        if (previousMovies == null) {
            previousMovies = new HashMap<>();
        }

        synchronized (previousMovies) { // Ensure thread safety
            previousMovies.put(item, previousMovies.get(item) + quantity);
        }

        session.setAttribute("previousMovies", previousMovies);

        JsonObject responseJsonObject = new JsonObject();

        JsonObject moviesJsonObject = new JsonObject();
        for (Map.Entry<String, Integer> entry : previousMovies.entrySet()) {
            moviesJsonObject.addProperty(entry.getKey(), entry.getValue());
        }

        responseJsonObject.add("previousMovies", moviesJsonObject);

        response.getWriter().write(responseJsonObject.toString());
    }
}
