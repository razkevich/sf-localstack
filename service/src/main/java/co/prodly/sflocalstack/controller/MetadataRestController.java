package co.prodly.sflocalstack.controller;

import co.prodly.sflocalstack.model.SalesforceError;
import co.prodly.sflocalstack.service.MetadataToolingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/services/data/{apiVersion}/tooling")
public class MetadataRestController {

    private final MetadataToolingService metadataToolingService;

    public MetadataRestController(MetadataToolingService metadataToolingService) {
        this.metadataToolingService = metadataToolingService;
    }

    @GetMapping("/query")
    public ResponseEntity<?> query(@RequestParam("q") String soql) {
        try {
            List<Map<String, Object>> records = metadataToolingService.executeToolingQuery(soql);
            return ResponseEntity.ok(Map.of("totalSize", records.size(), "done", true, "records", records));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(List.of(new SalesforceError(ex.getMessage(), "MALFORMED_QUERY")));
        }
    }
}
