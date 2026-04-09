package co.razkevich.sflocalstack.metadata.controller;

import co.razkevich.sflocalstack.metadata.model.MetadataResource;
import co.razkevich.sflocalstack.metadata.service.MetadataService;
import co.razkevich.sflocalstack.model.SalesforceError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin/metadata/resources")
public class MetadataAdminController {

    private final MetadataService metadataService;

    public MetadataAdminController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping
    public ResponseEntity<List<MetadataResource>> list() {
        return ResponseEntity.ok(metadataService.listResources());
    }

    @PostMapping
    public ResponseEntity<MetadataResource> create(@RequestBody MetadataResource resource) {
        return ResponseEntity.status(HttpStatus.CREATED).body(metadataService.createResource(resource));
    }

    @PutMapping("/{type}/{fullName:.+}")
    public ResponseEntity<MetadataResource> update(
            @PathVariable String type,
            @PathVariable String fullName,
            @RequestBody MetadataResource resource) {
        return ResponseEntity.ok(metadataService.updateResource(type, fullName, resource));
    }

    @DeleteMapping("/{type}/{fullName:.+}")
    public ResponseEntity<Void> delete(@PathVariable String type, @PathVariable String fullName) {
        metadataService.deleteResource(type, fullName);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<List<SalesforceError>> notFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(List.of(new SalesforceError(ex.getMessage(), "NOT_FOUND")));
    }
}
