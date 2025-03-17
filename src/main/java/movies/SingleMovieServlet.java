package movies;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
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

@WebServlet(name = "movies.SingleMovieServlet", urlPatterns = "/api/movie")
public class SingleMovieServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb_master");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        String id = request.getParameter("id");
        System.out.println("Getting id: " + id);
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            String query =
                    "SELECT m.id AS movie_id, " +
                            "       m.title AS title, " +
                            "       m.year AS year, " +
                            "       m.director AS director, " +
                            "       GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ', ') AS all_genres, " +
                            "       GROUP_CONCAT(DISTINCT s.id ORDER BY star_counts.star_count DESC, s.name ASC SEPARATOR ', ') AS all_star_ids, " +
                            "       GROUP_CONCAT(DISTINCT s.name ORDER BY star_counts.star_count DESC, s.name ASC SEPARATOR ', ') AS all_star_names, " +
                            "       r.rating AS rating " +
                            "FROM movies m " +
                            "LEFT JOIN ratings r ON r.movieId = m.id " +
                            "LEFT JOIN genres_in_movies gim ON gim.movieId = m.id " +
                            "LEFT JOIN genres g ON g.id = gim.genreId " +
                            "LEFT JOIN stars_in_movies sim ON sim.movieId = m.id " +
                            "LEFT JOIN stars s ON s.id = sim.starId " +
                            "LEFT JOIN ( " +
                            "    SELECT starId, COUNT(*) AS star_count " +
                            "    FROM stars_in_movies " +
                            "    GROUP BY starId " +
                            ") AS star_counts ON star_counts.starId = s.id " +
                            "WHERE m.id = ? " +
                            "GROUP BY m.id, m.title, m.year, m.director, r.rating;";

            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            JsonArray jsonArray = new JsonArray();

            while (rs.next()) {
                String movie_id = rs.getString("movie_id");
                String title = rs.getString("title");
                String year = rs.getString("year");
                String director = rs.getString("director");
                String all_genres = rs.getString("all_genres");
                String all_star_ids = rs.getString("all_star_ids");
                String all_star_names = rs.getString("all_star_names");
                String rating = rs.getString("rating");

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movie_id", movie_id);
                jsonObject.addProperty("title", title);
                jsonObject.addProperty("year", year);
                jsonObject.addProperty("director", director);
                jsonObject.addProperty("all_genres", all_genres);
                jsonObject.addProperty("all_star_ids", all_star_ids);
                jsonObject.addProperty("all_star_names", all_star_names);
                jsonObject.addProperty("rating", rating);


                jsonArray.add(jsonObject);
            }
            rs.close();
            ps.close();

            System.out.println("getting " + jsonArray.size() + " results");

            out.write(jsonArray.toString());
            response.setStatus(200);

        }catch (Exception e) {
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