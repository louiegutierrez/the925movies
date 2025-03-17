package java.movies;

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
import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.PrintWriter;

@WebServlet(name = "movies.AddMovieServlet", urlPatterns = "/api/add_movie")
public class AddMovieServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb_master");
            System.out.println("Got cart datasource");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        String title = request.getParameter("title");
        String yearStr = request.getParameter("year");
        String starName = request.getParameter("star_name");
        String genreName = request.getParameter("genre_name");
        String director = request.getParameter("director");

        System.out.println("Star name: " + starName + " movies.Genre name: " + genreName +
                " Director: " + director + " Title: " + title + " Year: " + yearStr);

        if (title == null || yearStr == null || director == null || genreName == null || starName == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required fields");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {

            CallableStatement cs = conn.prepareCall("{CALL add_movie(?, ?, ?, ?, ?)}");
            cs.setString(1, title);
            cs.setInt(2, Integer.parseInt(yearStr));
            cs.setString(3, director);
            cs.setString(4, starName);
            cs.setString(5, genreName);

            System.out.println("Calling stored procedure");

            boolean hasResult = cs.execute();
            String message = "";
            if (hasResult) {
                ResultSet rs = cs.getResultSet();
                if (rs.next()) {
                    message = rs.getString("message");
                }
                rs.close();
            }

            cs.close();

            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.println("{\"message\": \"" + message + "\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
