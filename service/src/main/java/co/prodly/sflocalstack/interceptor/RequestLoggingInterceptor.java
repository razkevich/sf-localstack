package co.prodly.sflocalstack.interceptor;

import co.prodly.sflocalstack.model.RequestLogEntry;
import co.prodly.sflocalstack.service.RequestLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.UUID;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private final RequestLogService requestLogService;

    public RequestLoggingInterceptor(RequestLogService requestLogService) {
        this.requestLogService = requestLogService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("startTime", System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        String path = request.getRequestURI();
        if (path.contains("/api/dashboard/events") || path.startsWith("/h2-console")) {
            return;
        }

        Long startTime = (Long) request.getAttribute("startTime");
        long durationMs = startTime != null
                ? (System.nanoTime() - startTime) / 1_000_000
                : 0L;

        RequestLogEntry entry = new RequestLogEntry(
                UUID.randomUUID().toString(),
                Instant.now().toString(),
                request.getMethod(),
                path,
                response.getStatus(),
                durationMs,
                "",
                ""
        );
        requestLogService.log(entry);
    }
}
