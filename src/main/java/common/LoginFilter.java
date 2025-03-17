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

        String requestURI   = httpRequest.getRequestURI();
        String contextPath  = httpRequest.getContextPath();

        // Prevent caching of protected pages
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
        httpResponse.setHeader("Pragma", "no-cache"); // HTTP 1.0
        httpResponse.setHeader("Expires", "0");       // Proxies

        // 1. Allow known static resources (CSS, JS, images) to pass through
        if (requestURI.startsWith(contextPath + "/static/") ||
                requestURI.endsWith(".css") ||
                requestURI.endsWith(".js")  ||
                requestURI.endsWith(".png") ||
                requestURI.endsWith(".jpg")) {
            chain.doFilter(request, response);
            return;
        }

        // 2. The login page and script references
        String loginURI       = "/login.html";
        boolean isLoginRequest = requestURI.equals(loginURI);
        boolean isLoginPage    = requestURI.endsWith("login.html");
        boolean isLoginJs      = requestURI.endsWith("login.js");
        boolean isApiEndpoint  = requestURI.contains("/api/login");

        // 3. If this is specifically the login page, the login script, or the login API, allow through
        //    (We do NOT allow all GET requests here -- that's the difference from the code that caused issues)
        if (isLoginRequest || isLoginPage || isLoginJs || isApiEndpoint) {
            chain.doFilter(request, response);
            return;
        }

        // 4. Everything else must have a valid JWT
        String jwtToken = JwtUtil.getCookieValue(httpRequest, "jwtToken");
        Claims claims    = JwtUtil.validateToken(jwtToken);

        if (claims != null) {
            // Check for role-based pages if needed
            String userRole          = claims.get("role", String.class);
            boolean isDashboardRequest = requestURI.endsWith("_dashboard.html");

            if (isDashboardRequest && !"employee".equals(userRole)) {
                httpResponse.sendRedirect(contextPath + "/browse.html");
                return;
            }

            // If we pass the role check, allow request
            chain.doFilter(request, response);
        } else {
            // If no valid JWT, go to login.html
            httpResponse.sendRedirect(loginURI);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization logic if needed
    }

    @Override
    public void destroy() {
        // Cleanup logic if needed
    }
}