package common;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter("/*") // Apply this filter to all URLs
public class LoginFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        // Prevent caching of protected pages
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
        httpResponse.setHeader("Pragma", "no-cache"); // HTTP 1.0
        httpResponse.setHeader("Expires", "0"); // Proxies

        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();

        // Allow static resources to pass through
        if (requestURI.startsWith(contextPath + "/static/") ||
                requestURI.endsWith(".css") ||
                requestURI.endsWith(".js") ||
                requestURI.endsWith(".png") ||
                requestURI.endsWith(".jpg")) {
            chain.doFilter(request, response);
            return;
        }

        String loginURI = contextPath + "/login.html";
        boolean isLoginRequest = requestURI.equals(loginURI);
        boolean isLoginPage = requestURI.endsWith("login.html");
        boolean isApiEndpoint = requestURI.startsWith(contextPath + "/api/login");

        // Allow login-related requests to pass through
        if (isLoginRequest || isLoginPage || isApiEndpoint) {
            chain.doFilter(request, response);
            return;
        }

        // Check if the user is logged in
        boolean isLoggedIn = (session != null && session.getAttribute("user") != null);

        if (isLoggedIn) {
            // Check if the user is trying to access _dashboard.html
            boolean isDashboardRequest = requestURI.endsWith("_dashboard.html");

            if (isDashboardRequest) {
                // Check if the user is an employee
                String userRole = (String) session.getAttribute("role");
                if ("employee".equals(userRole)) {
                    // Allow access to _dashboard.html for employees
                    chain.doFilter(request, response);
                } else {
                    // Redirect non-employees to browse.html
                    httpResponse.sendRedirect(contextPath + "/browse.html");
                }
            } else {
                // Allow access to other pages for logged-in users
                chain.doFilter(request, response);
            }
        } else {
            // Redirect unauthenticated users to the login page
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