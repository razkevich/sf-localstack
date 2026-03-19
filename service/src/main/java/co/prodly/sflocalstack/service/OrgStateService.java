package co.prodly.sflocalstack.service;

import co.prodly.sflocalstack.model.SObjectRecord;
import co.prodly.sflocalstack.repository.SObjectRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
    public Optional<SObjectRecord> findByTypeAndId(String objectType, String id) {
        return repository.findById(id)
                .filter(record -> objectType.equalsIgnoreCase(record.getObjectType()));
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

    @Transactional(readOnly = true)
    public Object resolveFieldValue(String objectType, Map<String, Object> fields, String fieldPath) {
        if (fields.containsKey(fieldPath)) {
            return fields.get(fieldPath);
        }

        if (!fieldPath.contains(".")) {
            return fields.get(fieldPath);
        }

        String[] parts = fieldPath.split("\\.", 2);
        String relationshipName = parts[0];
        String relatedField = parts[1];

        Object literalValue = fields.get(fieldPath);
        if (literalValue != null) {
            return literalValue;
        }

        Object relationshipId = fields.get(relationshipName + "Id");
        if (!(relationshipId instanceof String relationshipRecordId)) {
            return null;
        }

        String relatedObjectType = inferObjectTypeFromRelationship(relationshipName);
        return findByTypeAndId(relatedObjectType, relationshipRecordId)
                .map(SObjectRecord::getFieldsJson)
                .map(this::fromJson)
                .map(relatedFields -> resolveFieldValue(relatedObjectType, relatedFields, relatedField))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> toSalesforceRecord(String apiVersion, String objectType, Map<String, Object> fields) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attributes", Map.of(
                "type", objectType,
                "url", "/services/data/" + apiVersion + "/sobjects/" + objectType + "/" + fields.get("Id")
        ));

        fields.forEach((key, value) -> {
            if ("attributes".equals(key)) {
                return;
            }

            if (key.contains(".")) {
                addNestedValue(result, key, value);
            } else {
                result.put(key, value);
            }
        });

        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> describeFields(String objectType) {
        return findByType(objectType).stream()
                .map(SObjectRecord::getFieldsJson)
                .map(this::fromJson)
                .flatMap(fields -> fields.entrySet().stream())
                .filter(entry -> !"attributes".equals(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> guessFieldType(entry.getValue()),
                        (left, right) -> left,
                        LinkedHashMap::new
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> {
                    Map<String, Object> field = new LinkedHashMap<>();
                    field.put("name", entry.getKey());
                    field.put("label", entry.getKey());
                    field.put("type", entry.getValue());
                    field.put("custom", false);
                    field.put("createable", true);
                    field.put("updateable", true);
                    field.put("deprecatedAndHidden", false);
                    field.put("filterable", true);
                    field.put("sortable", true);
                    field.put("soapType", soapTypeFor(entry.getValue()));
                    field.put("nillable", true);
                    return field;
                })
                .toList();
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
    public synchronized UpsertResult upsert(
            String objectType,
            String externalIdField,
            String externalIdValue,
            Map<String, Object> fields) {
        Optional<SObjectRecord> existing = findByType(objectType).stream()
                .filter(record -> externalIdValue.equals(String.valueOf(fromJson(record.getFieldsJson()).get(externalIdField))))
                .findFirst();

        if (existing.isPresent()) {
            SObjectRecord updated = update(existing.get().getId(), mergeExternalId(fields, externalIdField, externalIdValue))
                    .orElseThrow();
            return new UpsertResult(updated, false);
        }

        SObjectRecord created = create(objectType, mergeExternalId(fields, externalIdField, externalIdValue));
        return new UpsertResult(created, true);
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

    @SuppressWarnings("unchecked")
    private void addNestedValue(Map<String, Object> target, String fieldPath, Object value) {
        String[] parts = fieldPath.split("\\.");
        Map<String, Object> current = target;
        for (int index = 0; index < parts.length - 1; index++) {
            Object existing = current.get(parts[index]);
            if (!(existing instanceof Map<?, ?> existingMap)) {
                Map<String, Object> nested = new LinkedHashMap<>();
                current.put(parts[index], nested);
                current = nested;
            } else {
                current = (Map<String, Object>) existingMap;
            }
        }
        current.put(parts[parts.length - 1], value);
    }

    private String guessFieldType(Object value) {
        if (value == null) {
            return "string";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (value instanceof Integer || value instanceof Long) {
            return "int";
        }
        if (value instanceof Float || value instanceof Double) {
            return "double";
        }
        if (value instanceof String stringValue && stringValue.matches("\\d{4}-\\d{2}-\\d{2}T.*Z")) {
            return "datetime";
        }
        return "string";
    }

    private String inferObjectTypeFromRelationship(String relationshipName) {
        if (relationshipName.endsWith("__r")) {
            return relationshipName.substring(0, relationshipName.length() - 3) + "__c";
        }
        return relationshipName;
    }

    private String soapTypeFor(String fieldType) {
        return switch (fieldType) {
            case "boolean" -> "xsd:boolean";
            case "int" -> "xsd:int";
            case "double" -> "xsd:double";
            case "datetime" -> "xsd:dateTime";
            default -> "xsd:string";
        };
    }

    private Map<String, Object> mergeExternalId(Map<String, Object> fields, String externalIdField, String externalIdValue) {
        Map<String, Object> merged = new LinkedHashMap<>(fields);
        merged.put(externalIdField, externalIdValue);
        return merged;
    }
}
