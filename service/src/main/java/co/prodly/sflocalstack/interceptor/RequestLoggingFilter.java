package co.prodly.sflocalstack.interceptor;

import co.prodly.sflocalstack.model.RequestLogEntry;
import co.prodly.sflocalstack.service.RequestLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_BYTES = 8192;

    private final RequestLogService requestLogService;

    public RequestLoggingFilter(RequestLogService requestLogService) {
        this.requestLogService = requestLogService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!shouldLog(path)) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, MAX_BODY_BYTES);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startNanos = System.nanoTime();
        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;

            String requestBody = new String(wrappedRequest.getContentAsByteArray(), StandardCharsets.UTF_8);
            String responseBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);

            requestLogService.log(new RequestLogEntry(
                    UUID.randomUUID().toString(),
                    Instant.now().toString(),
                    request.getMethod(),
                    path,
                    wrappedResponse.getStatus(),
                    durationMs,
                    requestBody,
                    responseBody
            ));

            wrappedResponse.copyBodyToResponse();
        }
    }

    private boolean shouldLog(String path) {
        return path.startsWith("/services/") || path.startsWith("/oauth/");
    }
}
