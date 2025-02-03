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

        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();

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

        if (isLoginRequest || isLoginPage || isApiEndpoint) {
            chain.doFilter(request, response);
            return;
        }

        boolean isLoggedIn = (session != null && session.getAttribute("user") != null);

        if (isLoggedIn) {
            httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
            httpResponse.setHeader("Pragma", "no-cache"); // HTTP 1.0
            httpResponse.setHeader("Expires", "0"); // Proxies

            chain.doFilter(request, response);
        } else {
            httpResponse.sendRedirect(loginURI);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}