package co.prodly.sflocalstack.service;

import co.prodly.sflocalstack.model.SObjectRecord;
import co.prodly.sflocalstack.repository.SObjectRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrgStateService {

    private static final Logger log = LoggerFactory.getLogger(OrgStateService.class);

    private final SObjectRecordRepository repository;
    private final ObjectMapper objectMapper;

    public OrgStateService(SObjectRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SObjectRecord create(String objectType, Map<String, Object> fields) {
        String id = generateId(objectType);
        Instant now = Instant.now();
        fields.put("Id", id);
        fields.put("CreatedDate", now.toString());
        fields.put("LastModifiedDate", now.toString());
        String json = toJson(fields);
        SObjectRecord record = new SObjectRecord(id, objectType, json, now, now);
        return repository.save(record);
    }

    @Transactional(readOnly = true)
    public List<SObjectRecord> findByType(String objectType) {
        return repository.findByObjectType(objectType);
    }

    @Transactional(readOnly = true)
    public Optional<SObjectRecord> findById(String id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<SObjectRecord> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> countByObjectType() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(
                        SObjectRecord::getObjectType,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
    }

    @Transactional
    public Optional<SObjectRecord> update(String id, Map<String, Object> fields) {
        return repository.findById(id).map(record -> {
            Map<String, Object> existing = fromJson(record.getFieldsJson());
            existing.putAll(fields);
            Instant now = Instant.now();
            existing.put("LastModifiedDate", now.toString());
            record.setFieldsJson(toJson(existing));
            record.setLastModifiedDate(now);
            return repository.save(record);
        });
    }

    @Transactional
    public boolean delete(String id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    public void reset() {
        repository.deleteAll();
        log.info("Org state reset — all sObject records deleted");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse fields JSON", e);
            return Map.of();
        }
    }

    private String toJson(Map<String, Object> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (Exception e) {
            log.error("Failed to serialize fields", e);
            return "{}";
        }
    }

    private String generateId(String objectType) {
        String prefix = objectType.length() >= 3
                ? objectType.substring(0, 3).toUpperCase()
                : objectType.toUpperCase();
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 15);
        return prefix + uid;
    }
}
