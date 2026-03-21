package co.razkevich.sflocalstack.controller;

import co.razkevich.sflocalstack.model.SalesforceError;
import co.razkevich.sflocalstack.service.MetadataToolingService;
import co.razkevich.sflocalstack.service.SoqlEngine;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping({"/services/data/{apiVersion}", "/data/{apiVersion}"})
public class QueryController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QueryController.class);

    private final SoqlEngine soqlEngine;
    private final MetadataToolingService metadataToolingService;

    public QueryController(SoqlEngine soqlEngine, MetadataToolingService metadataToolingService) {
        this.soqlEngine = soqlEngine;
        this.metadataToolingService = metadataToolingService;
    }

    @GetMapping("/query")
    public ResponseEntity<?> query(
            @RequestParam("q") String soql,
            @RequestParam(name = "columns", defaultValue = "false") boolean includeColumns) {
        try {
            List<Map<String, Object>> records = soqlEngine.execute(soql);
            if (records.isEmpty()) {
                List<Map<String, Object>> metadataRecords = metadataToolingService.executeStandardMetadataQuery(soql);
                if (!metadataRecords.isEmpty()) {
                    records = metadataRecords;
                }
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalSize", records.size());
            result.put("done", true);
            result.put("records", records);
            if (includeColumns) {
                result.put("columnMetadata", buildColumnMetadata(records));
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(List.of(new SalesforceError(ex.getMessage(), "MALFORMED_QUERY")));
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildColumnMetadata(List<Map<String, Object>> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        Map<String, Object> first = new LinkedHashMap<>(records.getFirst());
        first.remove("attributes");
        return first.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> metadata = new LinkedHashMap<>();
                    metadata.put("columnName", entry.getKey());
                    metadata.put("displayName", entry.getKey());
                    metadata.put("aggregate", false);
                    if (entry.getValue() instanceof Map<?, ?> nested) {
                        metadata.put("joinColumns", ((Map<String, Object>) nested).entrySet().stream()
                                .map(child -> Map.<String, Object>of(
                                        "columnName", child.getKey(),
                                        "displayName", child.getKey(),
                                        "aggregate", false,
                                        "joinColumns", List.of()
                                ))
                                .toList());
                    } else {
                        metadata.put("joinColumns", List.of());
                    }
                    return metadata;
                })
                .toList();
    }
}
