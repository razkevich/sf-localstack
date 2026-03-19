package co.prodly.sflocalstack.controller;

import co.prodly.sflocalstack.model.SalesforceError;
import co.prodly.sflocalstack.service.SoqlEngine;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/services/data/{apiVersion}")
public class QueryController {

    private final SoqlEngine soqlEngine;

    public QueryController(SoqlEngine soqlEngine) {
        this.soqlEngine = soqlEngine;
    }

    @GetMapping("/query")
    public ResponseEntity<?> query(@RequestParam("q") String soql) {
        try {
            List<Map<String, Object>> records = soqlEngine.execute(soql);
            Map<String, Object> result = Map.of(
                    "totalSize", records.size(),
                    "done", true,
                    "records", records
            );
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(List.of(new SalesforceError(ex.getMessage(), "MALFORMED_QUERY")));
        }
    }
}
