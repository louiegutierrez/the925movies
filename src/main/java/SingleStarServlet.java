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
//import java.io.Serial;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "SingleStarServlet", urlPatterns = "/api/star")
public class SingleStarServlet extends HttpServlet {

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
        request.getServletContext().log("Getting id: " + id);
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            String query =
                    "SELECT "
                    + "  s.name AS star_name, "
                    + "  IF(s.birthYear IS NULL, 'N/A', CAST(s.birthYear AS CHAR)) AS birth_year, "
                    + "  m.id AS movie_id, "
                    + "  m.title AS movie_title, "
                    + "  m.year AS movie_year "
                    + "FROM stars s "
                    + "LEFT JOIN stars_in_movies sim ON sim.starId = s.id "
                    + "LEFT JOIN movies m ON m.id = sim.movieId "
                    + "WHERE s.id = ? "
                    + "ORDER BY m.year DESC, m.title;";


            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            JsonArray jsonArray = new JsonArray();

            while (rs.next()) {
                String name = rs.getString("star_name");
                String birthYear = rs.getString("birth_year");
                String movieId = rs.getString("movie_id");
                String movieTitle = rs.getString("movie_title");


                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("name", name);
                jsonObject.addProperty("birth_year", birthYear);
                jsonObject.addProperty("movie_id", movieId);
                jsonObject.addProperty("movie_title", movieTitle);
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