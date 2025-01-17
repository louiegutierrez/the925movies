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
import java.io.Serial;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/movie")
public class SingleMovieServlet extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;

    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        String id = request.getParameter("id");
        request.getServletContext().log("Getting id: " + id);
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            String query = """
                    SELECT
                        m.id AS movie_id,
                        m.title as title,
                        m.year as year,
                        m.director as director,
                        GROUP_CONCAT(DISTINCT g.name SEPARATOR ', ') AS all_genres,
                        GROUP_CONCAT(DISTINCT s.id SEPARATOR ', ') AS all_star_ids,
                        GROUP_CONCAT(DISTINCT s.name SEPARATOR ', ') AS all_star_names,
                        r.rating as rating
                    FROM movies m
                    JOIN ratings r ON r.movieId = m.id
                    LEFT JOIN genres_in_movies gim ON gim.movieId = m.id
                    LEFT JOIN genres g ON g.id = gim.genreId
                    LEFT JOIN stars_in_movies sim ON sim.movieId = m.id
                    LEFT JOIN stars s ON s.id = sim.starId
                    WHERE m.id = ?
                    GROUP BY m.id,
                             m.title,
                             m.year,
                             m.director,
                             r.rating;
                    """;

            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            JsonArray jsonArray = new JsonArray();

            while (rs.next()) {
                String movie_id = rs.getString(1);
                String title = rs.getString(2);
                String year = rs.getString(3);
                String director = rs.getString(4);
                String all_genres = rs.getString(5);
                String all_star_ids = rs.getString(6);
                String all_star_names = rs.getString(7);
                String rating = rs.getString(8);

                // Create a JsonObject based on the data we retrieve from rs
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

            request.getServletContext().log("getting " + jsonArray.size() + " results");

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