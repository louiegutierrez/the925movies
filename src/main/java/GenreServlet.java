import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.google.gson.Gson;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

@WebServlet(name = "GenreServlet", urlPatterns = "/api/genres")
public class GenreServlet extends HttpServlet {
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

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<Genre> genres = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT name FROM genres ORDER BY name ASC";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    genres.add(new Genre(rs.getString("name")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error fetching genres: " + e.getMessage());
            return;
        }

        // Convert genres list to JSON
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print(new Gson().toJson(genres));
        out.flush();

        System.out.println("GenreServlet: Genres fetched successfully.");
    }
}

class Genre {
    private String name;

    public Genre(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}