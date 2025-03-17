package common;

import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter("/*") // Apply this filter to all URLs
public class LoginFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();

        // Prevent caching of protected pages
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
        httpResponse.setHeader("Pragma", "no-cache"); // HTTP 1.0
        httpResponse.setHeader("Expires", "0"); // Proxies

        // Allow static resources to pass through
        if (requestURI.startsWith(contextPath + "/static/") ||
                requestURI.endsWith(".css") ||
                requestURI.endsWith(".js") ||
                requestURI.endsWith(".png") ||
                requestURI.endsWith(".jpg")) {
            chain.doFilter(request, response);
            return;
        }

        String loginURI = contextPath + "/api/login";
        boolean isLoginRequest = requestURI.equals(loginURI);

        // Allow login requests to pass through
        if (isLoginRequest) {
            chain.doFilter(request, response);
            return;
        }

        // Retrieve JWT from cookies
        String jwtToken = JwtUtil.getCookieValue(httpRequest, "jwtToken");
        Claims claims = JwtUtil.validateToken(jwtToken);

        if (claims != null) {
            // Extract user role if needed
            String userRole = claims.get("role", String.class);
            boolean isDashboardRequest = requestURI.endsWith("_dashboard.html");

            if (isDashboardRequest && !"employee".equals(userRole)) {
                httpResponse.sendRedirect(contextPath + "/browse.html");
                return;
            }

            chain.doFilter(request, response); // Allow request to proceed
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
