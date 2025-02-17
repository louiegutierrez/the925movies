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
import java.sql.*;

@WebServlet(name = "InsertStarServlet", urlPatterns = "/api/insert_star")
public class InsertStarServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // Only allow if employee is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role").equals("customer")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized access");
            return;
        }

        String starName = request.getParameter("star_name");
        String birthYearStr = request.getParameter("birth_year");
        Integer birthYear = null;
        if (birthYearStr != null && !birthYearStr.trim().isEmpty()) {
            birthYear = Integer.valueOf(birthYearStr);
        }

        try (Connection conn = dataSource.getConnection()) {
            String newStarId = generateNewStarId(conn);
            System.out.println("New star ID: " + newStarId);
            String insertSql = "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, newStarId);
                ps.setString(2, starName);
                if (birthYear != null) {
                    ps.setInt(3, birthYear);
                } else {
                    ps.setNull(3, Types.INTEGER);
                }
                ps.executeUpdate();
            }

            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.println("{\"message\":\"Star added successfully with ID " + newStarId + "\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private String generateNewStarId(Connection conn) throws SQLException {
        String query = "SELECT MAX(id) FROM stars";
        System.out.println("Query: " + query);

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.executeQuery();
            // get int ID and add one to it and add nm to beginning when returning
            ResultSet rs = ps.getResultSet();
            System.out.println("Result set: " + rs);
            if (!rs.next()) {
                throw new SQLException("Failed to generate new star ID");
            } else {
                System.out.println("Result " + rs.getString(1));
                System.out.println("Result " + rs.getString(1).substring(2));
                int maxId = Integer.parseInt(rs.getString(1).substring(2)) + 1;
                return "nm" + maxId;
            }
        } catch (SQLException e) {
            throw new SQLException("Failed to generate new star ID", e);
        }
    }
}
