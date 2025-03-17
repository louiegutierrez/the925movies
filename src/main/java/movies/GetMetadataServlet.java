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
import jakarta.servlet.http.HttpSession;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
//import java.io.Serial;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet(name = "movies.GetMetadataServlet", urlPatterns = "/api/get_metadata")
public class GetMetadataServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb_slave");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // Check employee session
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role").equals("customer")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement psTables = conn.prepareStatement("SELECT table_name FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'moviedb'");
            ResultSet rsTables = psTables.executeQuery();

            JsonArray tablesArray = new JsonArray(); // using e.g. GSON or JSON-P
            while (rsTables.next()) {
                String tableName = rsTables.getString("table_name");

                // For each table, fetch columns
                PreparedStatement psCols = conn.prepareStatement(
                        "SELECT column_name, column_type FROM INFORMATION_SCHEMA.COLUMNS " +
                                "WHERE TABLE_SCHEMA = 'moviedb' AND TABLE_NAME = ?");
                psCols.setString(1, tableName);
                ResultSet rsCols = psCols.executeQuery();

                JsonArray columnsArray = new JsonArray();
                while (rsCols.next()) {
                    JsonObject colObj = new JsonObject();
                    colObj.addProperty("name", rsCols.getString("column_name"));
                    colObj.addProperty("type", rsCols.getString("column_type"));
                    columnsArray.add(colObj);
                }
                rsCols.close();
                psCols.close();

                JsonObject tableObj = new JsonObject();
                tableObj.addProperty("name", tableName);
                tableObj.add("columns", columnsArray);

                tablesArray.add(tableObj);
            }
            rsTables.close();
            psTables.close();

            JsonObject result = new JsonObject();
            result.add("tables", tablesArray);

            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.write(result.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}