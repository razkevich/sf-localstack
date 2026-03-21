package co.prodly.sflocalstack.controller;

import co.prodly.sflocalstack.service.OrgStateService;
import co.prodly.sflocalstack.service.BulkJobService;
import co.prodly.sflocalstack.service.MetadataService;
import co.prodly.sflocalstack.service.RequestLogService;
import co.prodly.sflocalstack.service.SeedDataLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ResetController {

    private final OrgStateService orgStateService;
    private final SeedDataLoader seedDataLoader;
    private final BulkJobService bulkJobService;
    private final MetadataService metadataService;
    private final RequestLogService requestLogService;
    private final ObjectMapper objectMapper;

    public ResetController(OrgStateService orgStateService, SeedDataLoader seedDataLoader, BulkJobService bulkJobService, MetadataService metadataService, RequestLogService requestLogService, ObjectMapper objectMapper) {
        this.orgStateService = orgStateService;
        this.seedDataLoader = seedDataLoader;
        this.bulkJobService = bulkJobService;
        this.metadataService = metadataService;
        this.requestLogService = requestLogService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/reset", consumes = {"application/json", "*/*"})
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, String>> reset(@RequestBody(required = false) String body) {
        orgStateService.reset();
        bulkJobService.reset();
        metadataService.reset();
        requestLogService.reset();
        seedDataLoader.load();

        if (body != null && !body.isBlank()) {
            try {
                Map<String, Object> request = objectMapper.readValue(body, Map.class);
                Object overridesRaw = request.get("seedOverrides");
                if (overridesRaw instanceof Map<?, ?> overridesMap) {
                    overridesMap.forEach((typeKey, fieldsRaw) -> {
                        if (fieldsRaw instanceof Map<?, ?> fieldOverrides) {
                            orgStateService.findByType(String.valueOf(typeKey)).forEach(record -> {
                                Map<String, Object> updates = new java.util.LinkedHashMap<>();
                                fieldOverrides.forEach((k, v) -> updates.put(String.valueOf(k), v));
                                orgStateService.update(record.getId(), updates);
                            });
                        }
                    });
                }
            } catch (Exception ignored) {
                // malformed JSON body is silently ignored — reset still succeeds
            }
        }

        return ResponseEntity.ok(Map.of("status", "ok", "message", "Org state reset"));
    }
}
