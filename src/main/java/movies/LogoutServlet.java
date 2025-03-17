package movies;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet(name = "movies.LogoutServlet", urlPatterns = "/api/logout")
public class LogoutServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);

        // Invalidate the session if it exists
        if (session != null) {
            session.invalidate();
            System.out.println("Session invalidated successfully.");
        } else {
            System.out.println("No active session found.");
        }

        // Redirect to the login page
        response.sendRedirect(request.getContextPath() + "/login.html?cachebuster=" + System.currentTimeMillis());
    }
}