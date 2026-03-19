package co.prodly.sflocalstack.controller;

import co.prodly.sflocalstack.model.DashboardOverview;
import co.prodly.sflocalstack.model.RequestLogEntry;
import co.prodly.sflocalstack.service.OrgStateService;
import co.prodly.sflocalstack.service.RequestLogService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final RequestLogService requestLogService;
    private final OrgStateService orgStateService;

    public DashboardController(RequestLogService requestLogService, OrgStateService orgStateService) {
        this.requestLogService = requestLogService;
        this.orgStateService = orgStateService;
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

    @GetMapping("/overview")
    public ResponseEntity<DashboardOverview> overview() {
        List<DashboardOverview.ObjectCount> objectCounts = orgStateService.countByObjectType().entrySet().stream()
                .map(entry -> new DashboardOverview.ObjectCount(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(DashboardOverview.ObjectCount::objectType))
                .toList();

        int totalRecords = objectCounts.stream()
                .mapToInt(DashboardOverview.ObjectCount::count)
                .sum();

        return ResponseEntity.ok(new DashboardOverview(
                "sf-localstack",
                "ok",
                "v60.0",
                totalRecords,
                requestLogService.size(),
                objectCounts
        ));
    }
}
