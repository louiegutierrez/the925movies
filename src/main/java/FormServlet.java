import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.jasypt.util.password.PasswordEncryptor;
import org.jasypt.util.password.StrongPasswordEncryptor;

@WebServlet(name = "FormServlet", urlPatterns = "/api/login")
public class FormServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
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
            // first check if it's an employee
            String employeeQuery = "SELECT * FROM employees WHERE email = ?";
            PreparedStatement employeePs = conn.prepareStatement(employeeQuery);
            employeePs.setString(1, username);
            ResultSet employeeRs = employeePs.executeQuery();
            if (employeeRs.next() && new StrongPasswordEncryptor().checkPassword(password, employeeRs.getString("password"))) {
                HttpSession session = request.getSession();
                System.out.println("Employee logged in");
                session.setAttribute("user", "employee");
                session.setAttribute("role", "employee");

                responseJsonObject.addProperty("status", "success");
                responseJsonObject.addProperty("message", "success");
                responseJsonObject.addProperty("role", "employee");
                out.write(responseJsonObject.toString());
                out.close();
                return;
            }
            // customer if not employee
            String query = "SELECT * FROM customers WHERE email = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next() && new StrongPasswordEncryptor().checkPassword(password, rs.getString("password"))) {
                // Set session attribute
                HttpSession session = request.getSession();
                session.setAttribute("user", rs.getInt("id"));
                session.setAttribute("role", "customer");

                // Return success response
                responseJsonObject.addProperty("status", "success");
                responseJsonObject.addProperty("message", "success");
                responseJsonObject.addProperty("role", "customer");
            } else {
                // Return error response
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Incorrect email or password.");
            }

            // Write JSON response
            out.write(responseJsonObject.toString());
        } catch (Exception e) {
            // Handle database errors
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