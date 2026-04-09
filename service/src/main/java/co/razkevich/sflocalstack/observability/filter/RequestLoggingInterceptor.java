package co.razkevich.sflocalstack.observability.filter;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Kept for WebConfig wiring compatibility.
 * Request logging (including body capture) is handled by {@link RequestLoggingFilter}.
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {
}
