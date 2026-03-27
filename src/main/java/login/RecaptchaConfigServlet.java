package login;

import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "login.RecaptchaConfigServlet", urlPatterns = "/api/recaptcha-site-key")
public class RecaptchaConfigServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        JsonObject responseJson = new JsonObject();

        String siteKey = System.getenv("RECAPTCHA_SITE_KEY");
        if (siteKey == null || siteKey.isBlank()) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseJson.addProperty("status", "fail");
            responseJson.addProperty("message", "Missing required environment variable: RECAPTCHA_SITE_KEY");
        } else {
            responseJson.addProperty("status", "success");
            responseJson.addProperty("siteKey", siteKey);
        }

        try (PrintWriter out = response.getWriter()) {
            out.write(responseJson.toString());
        }
    }
}
