package co.razkevich.sflocalstack.metadata.service;

import co.razkevich.sflocalstack.metadata.model.MetadataCatalogEntry;
import co.razkevich.sflocalstack.metadata.model.MetadataDeployJob;
import co.razkevich.sflocalstack.metadata.model.MetadataDeployJobEntity;
import co.razkevich.sflocalstack.metadata.model.MetadataResource;
import co.razkevich.sflocalstack.metadata.model.MetadataResourceEntity;
import co.razkevich.sflocalstack.metadata.model.MetadataRetrieveJob;
import co.razkevich.sflocalstack.metadata.model.MetadataRetrieveJobEntity;
import co.razkevich.sflocalstack.metadata.repository.MetadataDeployJobRepository;
import co.razkevich.sflocalstack.metadata.repository.MetadataResourceRepository;
import co.razkevich.sflocalstack.metadata.repository.MetadataRetrieveJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class MetadataService {

    private final MetadataResourceRepository resourceRepo;
    private final MetadataDeployJobRepository deployJobRepo;
    private final MetadataRetrieveJobRepository retrieveJobRepo;
    private final MetadataZipService zipService;
    private final ObjectMapper objectMapper;

    public MetadataService(MetadataResourceRepository resourceRepo,
                           MetadataDeployJobRepository deployJobRepo,
                           MetadataRetrieveJobRepository retrieveJobRepo,
                           MetadataZipService zipService,
                           ObjectMapper objectMapper) {
        this.resourceRepo = resourceRepo;
        this.deployJobRepo = deployJobRepo;
        this.retrieveJobRepo = retrieveJobRepo;
        this.zipService = zipService;
        this.objectMapper = objectMapper;
        initDefaults();
    }

    public List<MetadataCatalogEntry> describeMetadata() {
        return resourceRepo.findAll().stream()
                .map(this::toRecord)
                .map(MetadataResource::toCatalogEntry)
                .toList();
    }

    public List<MetadataCatalogEntry> listMetadata(String type, String folder) {
        return listMetadata(type == null ? List.of() : List.of(type), folder);
    }

    public List<MetadataCatalogEntry> listMetadata(List<String> types, String folder) {
        return resourceRepo.findAll().stream()
                .map(this::toRecord)
                .map(MetadataResource::toCatalogEntry)
                .filter(entry -> types.isEmpty() || types.contains(entry.type()))
                .filter(entry -> folder == null || folder.isBlank() || entry.fileName().startsWith(folder + "/") || entry.fullName().startsWith(folder + "/"))
                .toList();
    }

    public List<ReadMetadataRecord> readMetadata(String type, List<String> fullNames) {
        return fullNames.stream()
                .map(fullName -> {
                    MetadataResourceEntity entity = resourceRepo.findByTypeAndFullName(type, fullName).orElse(null);
                    MetadataResource resource = entity != null ? toRecord(entity) : null;
                    String label = resource != null ? resource.label() : (fullName.contains(".") ? fullName.substring(fullName.indexOf('.') + 1) : fullName);
                    Map<String, Object> attributes = resource != null ? resource.attributes() : Map.of();
                    return new ReadMetadataRecord(type, fullName, label, attributes);
                })
                .toList();
    }

    public List<MetadataResource> listResources() {
        return resourceRepo.findAll().stream()
                .map(this::toRecord)
                .toList();
    }

    @Transactional
    public MetadataResource createResource(MetadataResource resource) {
        MetadataResource normalized = normalize(resource);
        MetadataResourceEntity entity = toEntity(normalized);
        // If one already exists with the same type+fullName, update it
        resourceRepo.findByTypeAndFullName(normalized.type(), normalized.fullName())
                .ifPresent(existing -> entity.setId(existing.getId()));
        resourceRepo.save(entity);
        return normalized;
    }

    @Transactional
    public MetadataResource updateResource(String type, String fullName, MetadataResource resource) {
        MetadataResourceEntity existing = resourceRepo.findByTypeAndFullName(type, fullName)
                .orElseThrow(() -> new NoSuchElementException("Unknown metadata resource: " + type + " / " + fullName));
        // Remove old entity if type/fullName changed
        resourceRepo.delete(existing);
        resourceRepo.flush();
        MetadataResource normalized = normalize(resource);
        MetadataResourceEntity newEntity = toEntity(normalized);
        resourceRepo.save(newEntity);
        return normalized;
    }

    @Transactional
    public void deleteResource(String type, String fullName) {
        MetadataResourceEntity existing = resourceRepo.findByTypeAndFullName(type, fullName)
                .orElseThrow(() -> new NoSuchElementException("Unknown metadata resource: " + type + " / " + fullName));
        resourceRepo.delete(existing);
    }

    @Transactional
    public MetadataRetrieveJob retrieve(List<MetadataManifestParser.TypeRequest> typeRequests) {
        String id = "09S" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        List<MetadataResource> allResources = resourceRepo.findAll().stream()
                .map(this::toRecord)
                .toList();
        List<MetadataResource> matched = allResources.stream()
                .filter(resource -> typeRequests.stream().anyMatch(req ->
                        req.type().equals(resource.type()) &&
                        (req.members().contains("*") || req.members().contains(resource.fullName()))))
                .toList();
        String zipFileBase64 = zipService.buildZip(typeRequests, allResources);
        MetadataRetrieveJob job = new MetadataRetrieveJob(id, true, true, "Succeeded", zipFileBase64, matched.size(), matched);

        MetadataRetrieveJobEntity entity = new MetadataRetrieveJobEntity();
        entity.setId(id);
        entity.setDone(true);
        entity.setSuccess(true);
        entity.setStatus("Succeeded");
        entity.setZipFileBase64(zipFileBase64);
        entity.setNumberComponentsTotal(matched.size());
        entity.setCreatedDate(Instant.now());
        entity.setCompletedDate(Instant.now());
        retrieveJobRepo.save(entity);

        return job;
    }

    public MetadataRetrieveJob checkRetrieveStatus(String id) {
        MetadataRetrieveJobEntity entity = retrieveJobRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Unknown retrieve id: " + id));
        return new MetadataRetrieveJob(
                entity.getId(),
                entity.isDone(),
                entity.isSuccess(),
                entity.getStatus(),
                entity.getZipFileBase64(),
                entity.getNumberComponentsTotal(),
                List.of()
        );
    }

    @Transactional
    public MetadataDeployJob deploy(String zipFile) {
        String id = "0Af" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        MetadataDeployJobEntity entity = new MetadataDeployJobEntity();
        entity.setId(id);
        entity.setDone(true);
        entity.setSuccess(true);
        entity.setStatus("Succeeded");
        entity.setNumberComponentsTotal(3);
        entity.setNumberComponentsDeployed(3);
        entity.setNumberComponentErrors(0);
        entity.setCreatedDate(Instant.now());
        entity.setCompletedDate(Instant.now());
        deployJobRepo.save(entity);

        return new MetadataDeployJob(id, true, true, "Succeeded", 3, 3, 0);
    }

    public MetadataDeployJob checkDeployStatus(String id) {
        MetadataDeployJobEntity entity = deployJobRepo.findById(id).orElse(null);
        if (entity != null) {
            return toDeployRecord(entity);
        }
        // Original behavior: computeIfAbsent with a default job for unknown IDs
        MetadataDeployJobEntity fallback = new MetadataDeployJobEntity();
        fallback.setId(id);
        fallback.setDone(true);
        fallback.setSuccess(true);
        fallback.setStatus("Succeeded");
        fallback.setNumberComponentsTotal(0);
        fallback.setNumberComponentsDeployed(0);
        fallback.setNumberComponentErrors(0);
        fallback.setCreatedDate(Instant.now());
        deployJobRepo.save(fallback);
        return toDeployRecord(fallback);
    }

    @Transactional
    public MetadataDeployJob cancelDeploy(String id) {
        MetadataDeployJobEntity existing = deployJobRepo.findById(id).orElse(null);
        int total = existing != null ? existing.getNumberComponentsTotal() : 0;
        int deployed = existing != null ? existing.getNumberComponentsDeployed() : 0;
        int errors = existing != null ? existing.getNumberComponentErrors() : 0;

        MetadataDeployJobEntity canceled = existing != null ? existing : new MetadataDeployJobEntity();
        canceled.setId(id);
        canceled.setDone(false);
        canceled.setSuccess(false);
        canceled.setStatus("Canceling");
        canceled.setNumberComponentsTotal(total);
        canceled.setNumberComponentsDeployed(deployed);
        canceled.setNumberComponentErrors(errors);
        if (canceled.getCreatedDate() == null) {
            canceled.setCreatedDate(Instant.now());
        }
        canceled.setCompletedDate(null);
        deployJobRepo.save(canceled);

        return new MetadataDeployJob(id, false, false, "Canceling", total, deployed, errors);
    }

    @Transactional
    public void reset() {
        deployJobRepo.deleteAll();
        retrieveJobRepo.deleteAll();
        resourceRepo.deleteAll();
        defaultResources().forEach(resource -> resourceRepo.save(toEntity(resource)));
    }

    public record ReadMetadataRecord(String xmlType, String fullName, String label, Map<String, Object> attributes) {
    }

    // --- Conversion methods ---

    private MetadataResource toRecord(MetadataResourceEntity entity) {
        Map<String, Object> attributes = deserializeAttributes(entity.getAttributesJson());
        return new MetadataResource(
                entity.getType(),
                entity.getFullName(),
                entity.getFileName(),
                entity.getDirectoryName(),
                entity.isInFolder(),
                entity.isMetaFile(),
                entity.getLastModifiedDate(),
                entity.getLabel(),
                attributes,
                entity.getSuffix()
        );
    }

    private MetadataResourceEntity toEntity(MetadataResource record) {
        MetadataResourceEntity entity = new MetadataResourceEntity();
        entity.setType(record.type());
        entity.setFullName(record.fullName());
        entity.setFileName(record.fileName());
        entity.setDirectoryName(record.directoryName());
        entity.setInFolder(record.inFolder());
        entity.setMetaFile(record.metaFile());
        entity.setLastModifiedDate(record.lastModifiedDate());
        entity.setLabel(record.label());
        entity.setSuffix(record.suffix());
        entity.setAttributesJson(serializeAttributes(record.attributes()));
        return entity;
    }

    private MetadataDeployJob toDeployRecord(MetadataDeployJobEntity entity) {
        return new MetadataDeployJob(
                entity.getId(),
                entity.isDone(),
                entity.isSuccess(),
                entity.getStatus(),
                entity.getNumberComponentsTotal(),
                entity.getNumberComponentsDeployed(),
                entity.getNumberComponentErrors()
        );
    }

    private String serializeAttributes(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize attributes", e);
        }
    }

    private Map<String, Object> deserializeAttributes(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize attributes", e);
        }
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

    private void initDefaults() {
        if (resourceRepo.count() == 0) {
            defaultResources().forEach(resource -> resourceRepo.save(toEntity(resource)));
        }
    }

    private List<MetadataResource> defaultResources() {
        return List.of();
    }
}
