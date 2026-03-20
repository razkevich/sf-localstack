package co.prodly.sflocalstack.controller;

import co.prodly.sflocalstack.model.SalesforceError;
import co.prodly.sflocalstack.service.MetadataToolingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping({"/services/data/{apiVersion}/tooling", "/data/{apiVersion}/tooling"})
public class MetadataRestController {

    private final MetadataToolingService metadataToolingService;

    public MetadataRestController(MetadataToolingService metadataToolingService) {
        this.metadataToolingService = metadataToolingService;
    }

    @GetMapping("/sobjects/{objectName}/describe")
    public ResponseEntity<?> describeToolingObject(@PathVariable String objectName) {
        List<Map<String, Object>> fields = switch (objectName) {
            case "SourceMember" -> List.of(
                    field("Id", "id"), field("MemberName", "string"),
                    field("MemberType", "string"), field("IsNameObsolete", "boolean"),
                    field("RevisionCounter", "int"), field("ChangedBy", "string")
            );
            default -> List.of(field("Id", "id"), field("Name", "string"));
        };
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", objectName);
        result.put("label", objectName);
        result.put("queryable", true);
        result.put("fields", fields);
        result.put("urls", Map.of("sobject", "/services/data/v60.0/tooling/sobjects/" + objectName));
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> field(String name, String type) {
        return Map.of("name", name, "type", type, "nillable", true, "updateable", false, "createable", false);
    }

    @GetMapping("/query")
    public ResponseEntity<?> query(@RequestParam("q") String soql) {
        try {
            List<Map<String, Object>> records = metadataToolingService.executeToolingQuery(soql);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("size", records.size());
            response.put("totalSize", records.size());
            response.put("done", true);
            response.put("queryLocator", null);
            response.put("entityTypeName", metadataToolingService.inferEntityTypeName(soql));
            response.put("records", records);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(List.of(new SalesforceError(ex.getMessage(), "MALFORMED_QUERY")));
        }
    }
}
