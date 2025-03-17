package common;

import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter("/*") // Apply this filter to all URLs
public class LoginFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest  = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // You can still capture contextPath if needed
        // but we'll remove it from the path checks for now
        // String contextPath = httpRequest.getContextPath();

        String requestURI = httpRequest.getRequestURI();

        // Prevent caching of protected pages
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
        httpResponse.setHeader("Pragma", "no-cache"); // HTTP 1.0
        httpResponse.setHeader("Expires", "0"); // Proxies

        // Allow static resources to pass through
        if (requestURI.startsWith("/static/") ||
                requestURI.endsWith(".css") ||
                requestURI.endsWith(".js") ||
                requestURI.endsWith(".png") ||
                requestURI.endsWith(".jpg")) {
            chain.doFilter(request, response);
            return;
        }

        // We'll assume the login page is served at /login.html
        // and the login API is at /api/login
        String loginURI = "/login.html";

        // Compare the *actual* requestURI with the above known paths
        boolean isLoginRequest  = requestURI.equals(loginURI);
        boolean isLoginPage     = requestURI.endsWith("/login.html");
        boolean isApiEndpoint   = requestURI.startsWith("/api/login");

        // If we're requesting the login page or hitting the API to log in, allow
        if (isLoginRequest || isLoginPage || isApiEndpoint) {
            chain.doFilter(request, response);
            return;
        }

        // Retrieve JWT from cookies
        String jwtToken = JwtUtil.getCookieValue(httpRequest, "jwtToken");
        Claims claims   = JwtUtil.validateToken(jwtToken);

        if (claims != null) {
            // Extract user role if needed
            String userRole = claims.get("role", String.class);

            // For example, if your app has a /employee_dashboard.html for employees
            boolean isDashboardRequest = requestURI.endsWith("_dashboard.html");

            // If an employee-only page is requested but the user is not employee, redirect
            if (isDashboardRequest && !"employee".equals(userRole)) {
                httpResponse.sendRedirect("/browse.html");
                return;
            }

            // Otherwise, user is authorized
            chain.doFilter(request, response);
        } else {
            // Redirect to login if JWT is invalid or missing
            httpResponse.sendRedirect(loginURI);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization logic (if needed)
    }

    @Override
    public void destroy() {
        // Cleanup logic (if needed)
    }
}
