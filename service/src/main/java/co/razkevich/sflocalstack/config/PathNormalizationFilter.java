package co.razkevich.sflocalstack.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class PathNormalizationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String normalizedUri = normalize(request.getRequestURI());
        if (normalizedUri.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher(normalizedUri);
        dispatcher.forward(request, response);
    }

    private String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalized = path.replaceAll("/{2,}", "/");
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }
}
