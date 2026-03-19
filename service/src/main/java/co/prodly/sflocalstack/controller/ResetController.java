package co.prodly.sflocalstack.controller;

import co.prodly.sflocalstack.service.OrgStateService;
import co.prodly.sflocalstack.service.SeedDataLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ResetController {

    private final OrgStateService orgStateService;
    private final SeedDataLoader seedDataLoader;

    public ResetController(OrgStateService orgStateService, SeedDataLoader seedDataLoader) {
        this.orgStateService = orgStateService;
        this.seedDataLoader = seedDataLoader;
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        orgStateService.reset();
        seedDataLoader.load();
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Org state reset"));
    }
}
