package co.prodly.sflocalstack.controller;

import co.prodly.sflocalstack.model.RequestLogEntry;
import co.prodly.sflocalstack.service.RequestLogService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final RequestLogService requestLogService;

    public DashboardController(RequestLogService requestLogService) {
        this.requestLogService = requestLogService;
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() {
        return requestLogService.newEmitter();
    }

    @GetMapping("/requests")
    public ResponseEntity<List<RequestLogEntry>> requests(
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(requestLogService.getRecent(limit));
    }
}
