package co.prodly.sflocalstack.service;

import co.prodly.sflocalstack.model.MetadataCatalogEntry;
import co.prodly.sflocalstack.model.MetadataDeployJob;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MetadataService {

    private final Map<String, MetadataDeployJob> deployJobs = new ConcurrentHashMap<>();
    private final List<MetadataCatalogEntry> catalog = List.of(
            new MetadataCatalogEntry("CustomField", "Account.Type", "objects/Account.object", "objects", false, true, Instant.parse("2026-03-19T20:00:00Z")),
            new MetadataCatalogEntry("StandardValueSet", "AccountType", "standardValueSets/AccountType.standardValueSet", "standardValueSets", false, true, Instant.parse("2026-03-19T20:00:00Z")),
            new MetadataCatalogEntry("GlobalValueSet", "CustomerPriority", "globalValueSets/CustomerPriority.globalValueSet", "globalValueSets", false, true, Instant.parse("2026-03-19T20:00:00Z")),
            new MetadataCatalogEntry("CustomApplication", "SalesConsole", "applications/SalesConsole.app", "applications", false, true, Instant.parse("2026-03-19T20:00:00Z")),
            new MetadataCatalogEntry("FlowDefinition", "LoginFlow", "flowDefinitions/LoginFlow.flowDefinition", "flowDefinitions", false, true, Instant.parse("2026-03-19T20:00:00Z")),
            new MetadataCatalogEntry("DecisionTable", "RoutingDecision", "decisionTables/RoutingDecision.decisionTable", "decisionTables", false, true, Instant.parse("2026-03-19T20:00:00Z"))
    );

    public List<MetadataCatalogEntry> describeMetadata() {
        return catalog;
    }

    public List<MetadataCatalogEntry> listMetadata(String type, String folder) {
        return catalog.stream()
                .filter(entry -> entry.type().equals(type))
                .filter(entry -> folder == null || folder.isBlank() || entry.fileName().startsWith(folder + "/") || entry.fullName().startsWith(folder + "/"))
                .toList();
    }

    public List<ReadMetadataRecord> readMetadata(String type, List<String> fullNames) {
        return fullNames.stream()
                .map(fullName -> new ReadMetadataRecord(type, fullName, fullName.contains(".") ? fullName.substring(fullName.indexOf('.') + 1) : fullName))
                .toList();
    }

    public MetadataDeployJob deploy(String zipFile) {
        String id = "0Af" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        MetadataDeployJob job = new MetadataDeployJob(id, true, true, "Succeeded", 3, 3, 0);
        deployJobs.put(id, job);
        return job;
    }

    public MetadataDeployJob checkDeployStatus(String id) {
        return deployJobs.computeIfAbsent(id, missing -> new MetadataDeployJob(missing, true, true, "Succeeded", 0, 0, 0));
    }

    public MetadataDeployJob cancelDeploy(String id) {
        MetadataDeployJob existing = deployJobs.get(id);
        if (existing == null) {
            throw new NoSuchElementException("Unknown deploy id: " + id);
        }
        MetadataDeployJob canceled = new MetadataDeployJob(id, true, true, "Canceled", existing.numberComponentsTotal(), existing.numberComponentsDeployed(), existing.numberComponentErrors());
        deployJobs.put(id, canceled);
        return canceled;
    }

    public void reset() {
        deployJobs.clear();
    }

    public record ReadMetadataRecord(String xmlType, String fullName, String label) {
    }
}
