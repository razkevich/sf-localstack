package co.razkevich.sflocalstack.service;

import co.razkevich.sflocalstack.observability.model.RequestLogEntry;
import co.razkevich.sflocalstack.observability.service.RequestLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLogServiceTest {

    private RequestLogService requestLogService;

    @BeforeEach
    void setUp() {
        requestLogService = new RequestLogService(new ObjectMapper());
    }

    @Test
    void logAddsEntryToRecentList() {
        RequestLogEntry entry = new RequestLogEntry(
                "id-1", "2026-01-01T00:00:00Z", "GET", "/api/test", 200, 42L, null, null);
        requestLogService.log(entry);
        assertThat(requestLogService.getRecent(10)).containsExactly(entry);
    }

    @Test
    void resetClearsAllEntries() {
        requestLogService.log(new RequestLogEntry("id-1", "2026-01-01T00:00:00Z", "POST", "/api/foo", 201, 10L, "{}", "{}"));
        requestLogService.log(new RequestLogEntry("id-2", "2026-01-01T00:00:01Z", "GET", "/api/bar", 200, 5L, null, null));
        requestLogService.reset();
        assertThat(requestLogService.getRecent(10)).isEmpty();
        assertThat(requestLogService.size()).isZero();
    }

    @Test
    void newEmitterReturnsNonNull() {
        assertThat(requestLogService.newEmitter()).isNotNull();
    }
}
