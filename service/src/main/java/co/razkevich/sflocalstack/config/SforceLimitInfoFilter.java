package co.razkevich.sflocalstack.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds the {@code Sforce-Limit-Info} header to all responses for {@code /services/*} endpoints,
 * matching the header format returned by the real Salesforce REST API.
 */
@Component
public class SforceLimitInfoFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, response);
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/services/")) {
            response.setHeader("Sforce-Limit-Info", "api-usage=0/15000");
        }
    }
}
