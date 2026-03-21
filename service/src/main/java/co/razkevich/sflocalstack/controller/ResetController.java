package co.razkevich.sflocalstack.controller;

import co.razkevich.sflocalstack.service.OrgStateService;
import co.razkevich.sflocalstack.service.BulkJobService;
import co.razkevich.sflocalstack.service.MetadataService;
import co.razkevich.sflocalstack.service.RequestLogService;
import co.razkevich.sflocalstack.service.SeedDataLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ResetController {

    private final OrgStateService orgStateService;
    private final SeedDataLoader seedDataLoader;
    private final BulkJobService bulkJobService;
    private final MetadataService metadataService;
    private final RequestLogService requestLogService;

    public ResetController(OrgStateService orgStateService, SeedDataLoader seedDataLoader, BulkJobService bulkJobService, MetadataService metadataService, RequestLogService requestLogService) {
        this.orgStateService = orgStateService;
        this.seedDataLoader = seedDataLoader;
        this.bulkJobService = bulkJobService;
        this.metadataService = metadataService;
        this.requestLogService = requestLogService;
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        orgStateService.reset();
        bulkJobService.reset();
        metadataService.reset();
        requestLogService.reset();
        seedDataLoader.load();
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Org state reset"));
    }
}
