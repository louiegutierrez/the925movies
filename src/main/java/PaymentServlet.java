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
import jakarta.servlet.http.HttpSession;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;


@WebServlet(name = "PaymentServlet", urlPatterns = "/api/payment")
public class PaymentServlet extends HttpServlet {
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

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession();
        String sessionId = session.getId();

        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String cc = request.getParameter("cc");
        String date = request.getParameter("date");

        System.out.println("Data: ");
        System.out.println(firstName + " " + lastName + " " + cc + " " + date);

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT * FROM creditcards WHERE id = ? AND firstName = ? AND lastName = ? AND cc = ? AND expiration = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, cc);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setString(4, date);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("GOT A MATCH!");

                // query update to sales
                // for every movie in session add it to the sales table

            }

        } catch (Exception e) {

        } finally {
            out.close();
        }
    }
}
