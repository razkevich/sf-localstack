package co.prodly.sflocalstack.controller;

import co.prodly.sflocalstack.service.SoqlEngine;
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
    public ResponseEntity<Map<String, Object>> query(@RequestParam("q") String soql) {
        List<Map<String, Object>> records = soqlEngine.execute(soql);
        Map<String, Object> result = Map.of(
                "totalSize", records.size(),
                "done", true,
                "records", records
        );
        return ResponseEntity.ok(result);
    }
}
