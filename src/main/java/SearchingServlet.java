import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "SearchingServlet", urlPatterns = "/api/search")
public class SearchingServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String title = request.getParameter("title");
        String year = request.getParameter("year");
        String director = request.getParameter("director");
        String actor = request.getParameter("star");

        request.getServletContext().log("Getting Query");
        try (Connection conn = dataSource.getConnection()) {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT m.id, m.title, m.year, m.director, ");
            queryBuilder.append("GROUP_CONCAT(DISTINCT g.name ORDER BY g.name ASC LIMIT 3) AS genres, ");
            queryBuilder.append("GROUP_CONCAT(DISTINCT s.id ORDER BY (SELECT COUNT(*) FROM stars_in_movies WHERE starId = s.id) DESC, s.name ASC LIMIT 3) AS star_ids, ");
            queryBuilder.append("GROUP_CONCAT(DISTINCT s.name ORDER BY (SELECT COUNT(*) FROM stars_in_movies WHERE starId = s.id) DESC, s.name ASC LIMIT 3) AS stars, ");
            queryBuilder.append("COALESCE(r.rating, 0.0) AS rating ");
            queryBuilder.append("FROM movies m ");
            queryBuilder.append("LEFT JOIN genres_in_movies gm ON m.id = gm.movieId ");
            queryBuilder.append("LEFT JOIN genres g ON gm.genreId = g.id ");
            queryBuilder.append("LEFT JOIN stars_in_movies sm ON m.id = sm.movieId ");
            queryBuilder.append("LEFT JOIN stars s ON sm.starId = s.id ");
            queryBuilder.append("LEFT JOIN ratings r ON m.id = r.movieId ");
            queryBuilder.append("WHERE 1=1 ");

            if (title != null && !title.isEmpty()) {
                queryBuilder.append("AND m.title LIKE ? ");
            }
            if (year != null && !year.isEmpty()) {
                queryBuilder.append("AND m.year = ? ");
            }
            if (director != null && !director.isEmpty()) {
                queryBuilder.append("AND m.director LIKE ? ");
            }
            if (actor != null && !actor.isEmpty()) {
                queryBuilder.append("AND s.name LIKE ? ");
            }

            queryBuilder.append("GROUP BY m.id, m.title, m.year, m.director, r.rating");

            String query = queryBuilder.toString();
            request.getServletContext().log("Query: " + query);

            PreparedStatement statement = conn.prepareStatement(query);
            int paramIndex = 1;

            if (title != null && !title.isEmpty()) {
                statement.setString(paramIndex++, title + "%");
            }
            if (year != null && !year.isEmpty()) {
                statement.setInt(paramIndex++, Integer.parseInt(year));
            }
            if (director != null && !director.isEmpty()) {
                statement.setString(paramIndex++, director + "%");
            }
            if (actor != null && !actor.isEmpty()) {
                statement.setString(paramIndex++, actor + "%");
            }

            ResultSet resultSet = statement.executeQuery();

            JsonArray jsonArray = new JsonArray();
            while (resultSet.next()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movie_id", resultSet.getString("movie_id"));
                jsonObject.addProperty("title", resultSet.getString("title"));
                jsonObject.addProperty("year", resultSet.getInt("year"));
                jsonObject.addProperty("director", resultSet.getString("director"));
                jsonObject.addProperty("three_genres", resultSet.getString("genres"));
                jsonObject.addProperty("three_stars", resultSet.getString("stars"));
                jsonObject.addProperty("three_star_ids", resultSet.getInt("star_ids"));
                jsonObject.addProperty("rating", resultSet.getFloat("rating"));

                jsonArray.add(jsonObject);
            }

            out.write(jsonArray.toString());
            response.setStatus(200);
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