package co.prodly.sflocalstack.service;

import co.prodly.sflocalstack.model.MetadataCatalogEntry;
import co.prodly.sflocalstack.model.MetadataDeployJob;
import co.prodly.sflocalstack.model.MetadataResource;
import co.prodly.sflocalstack.model.MetadataRetrieveJob;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MetadataService {

    private final Map<String, MetadataDeployJob> deployJobs = new ConcurrentHashMap<>();
    private final Map<String, MetadataRetrieveJob> retrieveJobs = new ConcurrentHashMap<>();
    private final Map<String, MetadataResource> resources = new ConcurrentHashMap<>();

    private final MetadataZipService zipService;

    public MetadataService(MetadataZipService zipService) {
        this.zipService = zipService;
        reset();
    }

    public List<MetadataCatalogEntry> describeMetadata() {
        return resources.values().stream()
                .map(MetadataResource::toCatalogEntry)
                .toList();
    }

    public List<MetadataCatalogEntry> listMetadata(String type, String folder) {
        return listMetadata(type == null ? List.of() : List.of(type), folder);
    }

    public List<MetadataCatalogEntry> listMetadata(List<String> types, String folder) {
        return resources.values().stream()
                .map(MetadataResource::toCatalogEntry)
                .filter(entry -> types.isEmpty() || types.contains(entry.type()))
                .filter(entry -> folder == null || folder.isBlank() || entry.fileName().startsWith(folder + "/") || entry.fullName().startsWith(folder + "/"))
                .toList();
    }

    public List<ReadMetadataRecord> readMetadata(String type, List<String> fullNames) {
        return fullNames.stream()
                .map(fullName -> {
                    MetadataResource resource = resources.get(key(type, fullName));
                    String label = resource != null ? resource.label() : (fullName.contains(".") ? fullName.substring(fullName.indexOf('.') + 1) : fullName);
                    Map<String, Object> attributes = resource != null ? resource.attributes() : Map.of();
                    return new ReadMetadataRecord(type, fullName, label, attributes);
                })
                .toList();
    }

    public List<MetadataResource> listResources() {
        return resources.values().stream().toList();
    }

    public MetadataResource createResource(MetadataResource resource) {
        MetadataResource normalized = normalize(resource);
        resources.put(key(normalized.type(), normalized.fullName()), normalized);
        return normalized;
    }

    public MetadataResource updateResource(String type, String fullName, MetadataResource resource) {
        String existingKey = key(type, fullName);
        if (!resources.containsKey(existingKey)) {
            throw new NoSuchElementException("Unknown metadata resource: " + type + " / " + fullName);
        }
        resources.remove(existingKey);
        MetadataResource normalized = normalize(resource);
        resources.put(key(normalized.type(), normalized.fullName()), normalized);
        return normalized;
    }

    public void deleteResource(String type, String fullName) {
        if (resources.remove(key(type, fullName)) == null) {
            throw new NoSuchElementException("Unknown metadata resource: " + type + " / " + fullName);
        }
    }

    public MetadataRetrieveJob retrieve(List<MetadataManifestParser.TypeRequest> typeRequests) {
        String id = "09S" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        List<MetadataResource> matched = resources.values().stream()
                .filter(resource -> typeRequests.stream().anyMatch(req ->
                        req.type().equals(resource.type()) &&
                        (req.members().contains("*") || req.members().contains(resource.fullName()))))
                .toList();
        String zipFileBase64 = zipService.buildZip(typeRequests, resources.values().stream().toList());
        MetadataRetrieveJob job = new MetadataRetrieveJob(id, true, true, "Succeeded", zipFileBase64, matched.size(), matched);
        retrieveJobs.put(id, job);
        return job;
    }

    public MetadataRetrieveJob checkRetrieveStatus(String id) {
        MetadataRetrieveJob job = retrieveJobs.get(id);
        if (job == null) {
            throw new NoSuchElementException("Unknown retrieve id: " + id);
        }
        return job;
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
        retrieveJobs.clear();
        resources.clear();
        defaultResources().forEach(resource -> resources.put(key(resource.type(), resource.fullName()), resource));
    }

    public record ReadMetadataRecord(String xmlType, String fullName, String label, Map<String, Object> attributes) {
    }

    private String key(String type, String fullName) {
        return type + "::" + fullName;
    }

    private MetadataResource normalize(MetadataResource resource) {
        return new MetadataResource(
                resource.type(),
                resource.fullName(),
                resource.fileName(),
                resource.directoryName(),
                resource.inFolder(),
                resource.metaFile(),
                resource.lastModifiedDate() == null ? Instant.now() : resource.lastModifiedDate(),
                resource.label() == null || resource.label().isBlank() ? resource.fullName() : resource.label(),
                resource.attributes() == null ? Map.of() : resource.attributes()
        );
    }

    private List<MetadataResource> defaultResources() {
        Instant seededAt = Instant.parse("2026-03-19T20:00:00Z");
        return List.of(
                new MetadataResource("CustomObject", "Account", "objects/Account.object", "objects", false, true, seededAt, "Account",
                        Map.of("fields", List.of(Map.of("fullName", "Type", "label", "Type", "type", "Text"))), "object"),
                new MetadataResource("CustomField", "Account.Type", "objects/Account.object", "objects", false, true, seededAt, "Type", Map.of("fieldType", "Text"), null),
                new MetadataResource("StandardValueSet", "AccountType", "standardValueSets/AccountType.standardValueSet", "standardValueSets", false, true, seededAt, "Account Type", Map.of("values", List.of("Customer - Direct", "Customer - Channel")), "standardValueSet"),
                new MetadataResource("GlobalValueSet", "CustomerPriority", "globalValueSets/CustomerPriority.globalValueSet", "globalValueSets", false, true, seededAt, "Customer Priority", Map.of("values", List.of("High", "Medium", "Low")), "globalValueSet"),
                new MetadataResource("CustomApplication", "SalesConsole", "applications/SalesConsole.app", "applications", false, true, seededAt, "Sales Console", Map.of(), "app"),
                new MetadataResource("FlowDefinition", "LoginFlow", "flowDefinitions/LoginFlow.flowDefinition", "flowDefinitions", false, true, seededAt, "Login Flow", Map.of(), "flowDefinition"),
                new MetadataResource("DecisionTable", "RoutingDecision", "decisionTables/RoutingDecision.decisionTable", "decisionTables", false, true, seededAt, "Routing Decision", Map.of(), "decisionTable"),
                new MetadataResource("CustomTab", "standard-Account", "tabs/standard-Account.tab", "tabs", false, true, seededAt, "Account", Map.of(), "tab"),
                new MetadataResource("RecordType", "Account.Master", "objects/Account.object", "objects", false, true, seededAt, "Master", Map.of(), null),
                // Entries matching metadata-service WireMock fixtures for migration compatibility
                new MetadataResource("ApexClass", "SBQQ__CalculateCallbackAdapterTests", "classes/SBQQ__CalculateCallbackAdapterTests.cls", "classes", false, true, Instant.parse("2050-01-01T01:01:01.000Z"), "SBQQ__CalculateCallbackAdapterTests", Map.of(), "cls"),
                new MetadataResource("ApexClass", "SBQQ__CalculateCallbackAdapterTests2", "classes/SBQQ__CalculateCallbackAdapterTests2.cls", "classes", false, true, Instant.parse("1950-01-01T01:01:01.000Z"), "SBQQ__CalculateCallbackAdapterTests2", Map.of(), "cls"),
                new MetadataResource("EmailTemplate", "SampleEmailTemplate", "email/SampleEmailTemplate.email", "email", true, true, seededAt, "Sample Email Template", Map.of(), "email"),
                new MetadataResource("CustomObject", "Object2", "objects/Object2.object-meta.xml", "objects", false, true, Instant.parse("2051-01-01T01:01:01.000Z"), "Object2", Map.of(), "object"),
                new MetadataResource("WorkFlow", "WorkFlow1", "flows/WorkFlow1.workflow-meta.xml", "flows", false, true, Instant.parse("2052-01-01T01:01:01.000Z"), "WorkFlow1", Map.of(), "workflow"),
                new MetadataResource("StandardValueSet", "valueSet", "standardValueSets/valueSet.standardValueSet", "standardValueSets", false, true, seededAt, "valueSet", Map.of(), "standardValueSet"),
                new MetadataResource("CustomField", "picklistField", "objects/picklistField.field-meta.xml", "objects", false, true, seededAt, "Lookup Type Field", Map.of("fieldType", "Picklist"), null)
        );
    }
}
