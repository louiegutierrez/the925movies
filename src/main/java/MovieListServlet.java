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
import java.sql.ResultSet;
import java.sql.Statement;

@WebServlet(name = "MovieListServlet", urlPatterns = "/api/movielist")
public class MovieListServlet extends HttpServlet {
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
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            Statement statement = conn.createStatement();
            String query = """
                SELECT
                    m.id AS movie_id,
                    m.title AS title,
                    m.year AS year,
                    m.director AS director,
            
                    SUBSTRING_INDEX(
                        GROUP_CONCAT(DISTINCT g.name SEPARATOR ', '),
                        ', ',
                        3
                    ) AS three_genres,
            
                    SUBSTRING_INDEX(
                        GROUP_CONCAT(DISTINCT s.name SEPARATOR ', '),
                        ', ',
                        3
                    ) AS three_stars,
            
                    SUBSTRING_INDEX(
                        GROUP_CONCAT(DISTINCT s.id SEPARATOR ', '),
                        ', ',
                        3
                    ) AS three_star_ids,
            
                    r.rating AS rating
                FROM movies m
                JOIN ratings r ON r.movieId = m.id
                LEFT JOIN genres_in_movies gim ON gim.movieId = m.id
                LEFT JOIN genres g ON g.id = gim.genreId
                LEFT JOIN stars_in_movies sim ON sim.movieId = m.id
                LEFT JOIN stars s ON s.id = sim.starId
                GROUP BY m.id, m.title, m.year, m.director, r.rating
                ORDER BY r.rating DESC
                LIMIT 20;
            """;


            // Perform the query
            ResultSet rs = statement.executeQuery(query);
            JsonArray jsonArray = new JsonArray();

            while (rs.next()) {
                String movieId = rs.getString("movie_id");
                String title = rs.getString("title");
                String year = rs.getString("year");
                String director = rs.getString("director");
                String three_genres = rs.getString("three_genres");
                String three_stars = rs.getString("three_stars");
                String three_star_ids = rs.getString("three_star_ids");
                String rating = rs.getString("rating");

                // Create a JsonObject based on the data we retrieve from rs
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movie_id", movieId);
                jsonObject.addProperty("title", title);
                jsonObject.addProperty("year", year);
                jsonObject.addProperty("director", director);
                jsonObject.addProperty("three_genres", three_genres);
                jsonObject.addProperty("three_stars", three_stars);
                jsonObject.addProperty("three_star_ids", three_star_ids);
                jsonObject.addProperty("rating", rating);

                jsonArray.add(jsonObject);
            }
            rs.close();
            statement.close();

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