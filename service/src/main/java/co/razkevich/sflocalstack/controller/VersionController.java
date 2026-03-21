package co.razkevich.sflocalstack.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class VersionController {

    @GetMapping("/services/data/")
    public ResponseEntity<List<Map<String, String>>> versions() {
        return ResponseEntity.ok(List.of(
                version("50.0", "Winter '20"),
                version("51.0", "Spring '21"),
                version("52.0", "Summer '21"),
                version("53.0", "Winter '22"),
                version("54.0", "Spring '22"),
                version("55.0", "Summer '22"),
                version("56.0", "Winter '23"),
                version("57.0", "Spring '23"),
                version("58.0", "Summer '23"),
                version("59.0", "Winter '24"),
                version("60.0", "Winter '25")
        ));
    }

    @GetMapping("/services/data")
    public ResponseEntity<List<Map<String, String>>> versionsWithoutTrailingSlash() {
        return versions();
    }

    @GetMapping("/data")
    public ResponseEntity<List<Map<String, String>>> versionsAlias() {
        return versions();
    }

    @GetMapping("/services/data/{apiVersion:.+}")
    public ResponseEntity<Map<String, String>> resources() {
        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("sobjects", "/services/data/v60.0/sobjects");
        resources.put("query", "/services/data/v60.0/query");
        resources.put("queryAll", "/services/data/v60.0/queryAll");
        resources.put("tooling", "/services/data/v60.0/tooling");
        resources.put("jobs", "/services/data/v60.0/jobs");
        resources.put("recent", "/services/data/v60.0/recent");
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/data/{apiVersion:.+}")
    public ResponseEntity<Map<String, String>> resourcesAlias() {
        return resources();
    }

    private Map<String, String> version(String version, String label) {
        return Map.of(
                "version", version,
                "label", label,
                "url", "/services/data/v" + version
        );
    }
}
