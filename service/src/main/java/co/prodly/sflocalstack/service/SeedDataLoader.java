package co.prodly.sflocalstack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class SeedDataLoader {

    private static final Logger log = LoggerFactory.getLogger(SeedDataLoader.class);

    private final OrgStateService orgStateService;
    private final ObjectMapper yamlMapper;

    @Value("${sf-localstack.seed-file:classpath:seed/default-seed.yml}")
    private Resource seedFile;

    public SeedDataLoader(OrgStateService orgStateService) {
        this.orgStateService = orgStateService;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        load();
    }

    @SuppressWarnings("unchecked")
    public void load() {
        try (InputStream is = seedFile.getInputStream()) {
            Map<String, Object> root = yamlMapper.readValue(is, Map.class);
            List<Map<String, Object>> objects = (List<Map<String, Object>>) root.get("objects");
            if (objects == null) return;

            int total = 0;
            for (Map<String, Object> obj : objects) {
                String type = (String) obj.get("type");
                List<Map<String, Object>> records = (List<Map<String, Object>>) obj.get("records");
                if (records == null) continue;
                for (Map<String, Object> fields : records) {
                    orgStateService.create(type, new java.util.HashMap<>(fields));
                    total++;
                }
            }
            log.info("Loaded {} seed records from {}", total, seedFile.getDescription());
        } catch (Exception e) {
            log.warn("Failed to load seed data: {}", e.getMessage());
        }
    }
}
