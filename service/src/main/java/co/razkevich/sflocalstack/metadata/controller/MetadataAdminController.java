package co.razkevich.sflocalstack.metadata.controller;

import co.razkevich.sflocalstack.metadata.model.MetadataResource;
import co.razkevich.sflocalstack.metadata.service.MetadataService;
import co.razkevich.sflocalstack.model.SalesforceError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin/metadata")
public class MetadataAdminController {

    private final MetadataService metadataService;

    public MetadataAdminController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping("/resources")
    public ResponseEntity<List<MetadataResource>> list() {
        return ResponseEntity.ok(metadataService.listResources());
    }

    @PostMapping("/resources")
    public ResponseEntity<MetadataResource> create(@RequestBody MetadataResource resource) {
        return ResponseEntity.status(HttpStatus.CREATED).body(metadataService.createResource(resource));
    }

    @PutMapping("/resources/{type}/{fullName:.+}")
    public ResponseEntity<MetadataResource> update(
            @PathVariable String type,
            @PathVariable String fullName,
            @RequestBody MetadataResource resource) {
        return ResponseEntity.ok(metadataService.updateResource(type, fullName, resource));
    }

    @DeleteMapping("/resources/{type}/{fullName:.+}")
    public ResponseEntity<Void> delete(@PathVariable String type, @PathVariable String fullName) {
        metadataService.deleteResource(type, fullName);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/custom-objects")
    public ResponseEntity<?> createCustomObject(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String label = body.get("label");
        String apiName = body.get("apiName");
        String orgId = (String) request.getAttribute("orgId");

        if (label == null || apiName == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "label and apiName are required"));
        }
        if (!apiName.endsWith("__c")) {
            apiName = apiName + "__c";
        }

        MetadataResource resource = new MetadataResource(
                "CustomObject",
                apiName,
                "objects/" + apiName + ".object",
                "objects",
                false,
                false,
                Instant.now(),
                label,
                Map.of(),
                "object"
        );
        MetadataResource created = metadataService.createResource(resource);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<List<SalesforceError>> notFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(List.of(new SalesforceError(ex.getMessage(), "NOT_FOUND")));
    }
}
