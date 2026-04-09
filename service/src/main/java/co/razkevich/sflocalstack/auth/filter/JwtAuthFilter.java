package co.razkevich.sflocalstack.auth.filter;

import co.razkevich.sflocalstack.auth.service.JwtService;
import co.razkevich.sflocalstack.auth.store.UserStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Set<String> SKIP_EXACT = Set.of(
            "/services/oauth2/token",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/register",
            "/login",
            "/register"
    );

    private static final List<String> SKIP_PREFIXES = List.of(
            "/assets/",
            "/h2-console",
            "/api/dashboard/events"
    );

    private final JwtService jwtService;
    private final UserStore userStore;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtService jwtService, UserStore userStore, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userStore = userStore;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip auth for whitelisted paths
        if (shouldSkip(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip auth if no users registered yet (first-run mode)
        if (!userStore.hasUsers()) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(request, response, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = jwtService.validateToken(token);

            // Reject refresh tokens used as access tokens
            if ("refresh".equals(claims.get("type", String.class))) {
                sendError(request, response, "Refresh tokens cannot be used for API access");
                return;
            }

            request.setAttribute("userId", claims.get("userId", String.class));
            request.setAttribute("username", claims.get("username", String.class));
            request.setAttribute("role", claims.get("role", String.class));

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            sendError(request, response, "Session expired or invalid");
        }
    }

    private boolean shouldSkip(String path) {
        // Only enforce auth on API paths — let static content through
        if (!path.startsWith("/services/") && !path.startsWith("/api/") && !path.equals("/reset")) {
            return true;
        }
        if (SKIP_EXACT.contains(path)) {
            return true;
        }
        for (String prefix : SKIP_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void sendError(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        String path = request.getRequestURI();
        if (path.startsWith("/services/")) {
            // Salesforce-compatible error format
            objectMapper.writeValue(response.getOutputStream(),
                    List.of(Map.of("message", "Session expired or invalid", "errorCode", "INVALID_SESSION_ID")));
        } else {
            objectMapper.writeValue(response.getOutputStream(),
                    Map.of("error", "Unauthorized", "message", message));
        }
    }
}
