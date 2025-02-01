
import com.google.gson.JsonObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "CartServlet", urlPatterns = "/api/cart")
public class CartServlet extends HttpServlet {
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

        // write all the data into the jsonObject
        response.getWriter().write(responseJsonObject.toString());
    }

    /**
     * handles POST requests to add and show the item list information
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String item = request.getParameter("movieId");
        System.out.println(item);
        HttpSession session = request.getSession();

        Map<String, Integer> previousMovies = (Map<String, Integer>) session.getAttribute("previousMovies");
        if (previousMovies == null) {
            previousMovies = new HashMap<>();
        }

        synchronized (previousMovies) { // Ensure thread safety
            previousMovies.put(item, previousMovies.getOrDefault(item, 0) + 1);
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
