package co.razkevich.sflocalstack.observability.controller;

import co.razkevich.sflocalstack.metadata.model.MetadataResourceEntity;
import co.razkevich.sflocalstack.metadata.repository.MetadataResourceRepository;
import co.razkevich.sflocalstack.observability.model.DashboardOverview;
import co.razkevich.sflocalstack.observability.model.RequestLogEntry;
import co.razkevich.sflocalstack.data.service.OrgStateService;
import co.razkevich.sflocalstack.observability.service.RequestLogService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final RequestLogService requestLogService;
    private final OrgStateService orgStateService;
    private final MetadataResourceRepository metadataResourceRepository;

    public DashboardController(RequestLogService requestLogService, OrgStateService orgStateService,
                               MetadataResourceRepository metadataResourceRepository) {
        this.requestLogService = requestLogService;
        this.orgStateService = orgStateService;
        this.metadataResourceRepository = metadataResourceRepository;
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
        // Start with all standard objects at 0, then overlay actual counts
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String std : List.of("Account", "Contact", "Lead", "Opportunity", "Case", "User", "Task", "Event")) {
            counts.put(std, 0);
        }
        counts.putAll(orgStateService.countByObjectType());

        // Add custom objects from metadata that may have 0 records
        for (MetadataResourceEntity resource : metadataResourceRepository.findByType("CustomObject")) {
            String fullName = resource.getFullName();
            String apiName = fullName.endsWith("__c") ? fullName : fullName + "__c";
            counts.putIfAbsent(apiName, 0);
        }

        List<DashboardOverview.ObjectCount> objectCounts = counts.entrySet().stream()
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
