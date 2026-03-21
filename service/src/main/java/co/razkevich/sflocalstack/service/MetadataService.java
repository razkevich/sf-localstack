package co.razkevich.sflocalstack.service;

import co.razkevich.sflocalstack.model.MetadataCatalogEntry;
import co.razkevich.sflocalstack.model.MetadataDeployJob;
import co.razkevich.sflocalstack.model.MetadataResource;
import co.razkevich.sflocalstack.model.MetadataRetrieveJob;
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
        int total = existing != null ? existing.numberComponentsTotal() : 0;
        int deployed = existing != null ? existing.numberComponentsDeployed() : 0;
        int errors = existing != null ? existing.numberComponentErrors() : 0;
        MetadataDeployJob canceled = new MetadataDeployJob(id, false, false, "Canceling", total, deployed, errors);
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
        return List.of();
    }
}
