package co.prodly.sflocalstack.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class VersionController {

    @GetMapping("/services/data/")
    public ResponseEntity<List<Map<String, String>>> versions() {
        return ResponseEntity.ok(List.of(
                Map.of(
                        "version", "60.0",
                        "label", "Winter '25",
                        "url", "/services/data/v60.0"
                )
        ));
    }
}
