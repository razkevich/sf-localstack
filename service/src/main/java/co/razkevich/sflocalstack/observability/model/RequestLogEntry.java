package co.razkevich.sflocalstack.observability.model;

public record RequestLogEntry(
    String id,
    String timestamp,
    String method,
    String path,
    int statusCode,
    long durationMs,
    String requestBody,
    String responseBody
) {}
