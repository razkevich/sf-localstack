package co.prodly.sflocalstack.controller;

import co.prodly.sflocalstack.model.SObjectRecord;
import co.prodly.sflocalstack.model.SalesforceError;
import co.prodly.sflocalstack.service.OrgStateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/services/data/{apiVersion}/sobjects/{objectType}")
public class SObjectController {

    private final OrgStateService orgStateService;

    public SObjectController(OrgStateService orgStateService) {
        this.orgStateService = orgStateService;
    }

    @GetMapping("/describe")
    public ResponseEntity<Map<String, Object>> describe(@PathVariable String objectType) {
        return ResponseEntity.ok(Map.of(
                "name", objectType,
                "label", objectType,
                "queryable", true,
                "createable", true,
                "updateable", true,
                "deleteable", true,
                "fields", List.of()
        ));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@PathVariable String objectType) {
        List<SObjectRecord> records = orgStateService.findByType(objectType);
        List<Map<String, Object>> fields = records.stream()
                .map(r -> orgStateService.fromJson(r.getFieldsJson()))
                .toList();
        return ResponseEntity.ok(Map.of(
                "totalSize", fields.size(),
                "done", true,
                "records", fields
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String objectType, @PathVariable String id) {
        Optional<SObjectRecord> record = orgStateService.findById(id);
        if (record.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(List.of(new SalesforceError("Record not found", "NOT_FOUND")));
        }
        return ResponseEntity.ok(orgStateService.fromJson(record.get().getFieldsJson()));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @PathVariable String objectType,
            @RequestBody Map<String, Object> fields) {
        SObjectRecord record = orgStateService.create(objectType, fields);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", record.getId(), "success", true, "errors", List.of()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable String objectType,
            @PathVariable String id,
            @RequestBody Map<String, Object> fields) {
        Optional<SObjectRecord> updated = orgStateService.update(id, fields);
        if (updated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(List.of(new SalesforceError("Record not found", "NOT_FOUND")));
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String objectType, @PathVariable String id) {
        boolean deleted = orgStateService.delete(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(List.of(new SalesforceError("Record not found", "NOT_FOUND")));
        }
        return ResponseEntity.noContent().build();
    }
}
