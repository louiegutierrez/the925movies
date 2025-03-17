package login;

import common.JwtUtil;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.jasypt.util.password.StrongPasswordEncryptor;

@WebServlet(name = "login.FormServlet", urlPatterns = "/api/login")
public class FormServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb_slave");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String username = request.getParameter("email");
        String password = request.getParameter("password");

        JsonObject responseJsonObject = new JsonObject();

        try (Connection conn = dataSource.getConnection()) {
            String userRole = "customer";

            // Check if the user is an employee
            String query = "SELECT * FROM employees WHERE email = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next() && new StrongPasswordEncryptor().checkPassword(password, rs.getString("password"))) {
                userRole = "employee";
            } else {
                // Check if the user is a customer
                query = "SELECT * FROM customers WHERE email = ?";
                ps = conn.prepareStatement(query);
                ps.setString(1, username);
                rs = ps.executeQuery();
                if (!(rs.next() && new StrongPasswordEncryptor().checkPassword(password, rs.getString("password")))) {
                    responseJsonObject.addProperty("status", "fail");
                    responseJsonObject.addProperty("message", "Incorrect email or password.");
                    out.write(responseJsonObject.toString());
                    out.close();
                    return;
                }
            }

            // Generate JWT Token
            Map<String, Object> claims = new HashMap<>();
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            claims.put("loginTime", dateFormat.format(new Date()));
            claims.put("role", userRole);
            String token = JwtUtil.generateToken(username, claims);
            JwtUtil.updateJwtCookie(request, response, token);

            // Return success response
            responseJsonObject.addProperty("status", "success");
            responseJsonObject.addProperty("message", "success");
            responseJsonObject.addProperty("role", userRole);
            out.write(responseJsonObject.toString());
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