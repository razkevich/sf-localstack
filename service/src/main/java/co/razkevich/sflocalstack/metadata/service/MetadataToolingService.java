package co.razkevich.sflocalstack.metadata.service;

import co.razkevich.sflocalstack.data.service.OrgStateService;
import org.springframework.stereotype.Service;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MetadataToolingService {

    private static final Pattern QUERY_PATTERN = Pattern.compile(
            "(?is)^\\s*SELECT\\s+(.+?)\\s+FROM\\s+(\\w+)\\s*(?:WHERE\\s+(.+))?$"
    );
    private static final Pattern EQUALITY_PATTERN = Pattern.compile("(?i)^(\\w+)\\s*=\\s*(.+)$");
    private static final Pattern IN_PATTERN = Pattern.compile("(?i)^(\\w+)\\s+IN\\s*\\((.+)\\)$");

    private final MetadataService metadataService;
    private final OrgStateService orgStateService;

    public MetadataToolingService(MetadataService metadataService, OrgStateService orgStateService) {
        this.metadataService = metadataService;
        this.orgStateService = orgStateService;
    }

    public List<Map<String, Object>> executeToolingQuery(String soql) {
        ParsedQuery query = parse(soql);
        if ("FlowDefinitionView".equals(query.objectName())) {
            throw new IllegalArgumentException("Unsupported tooling query: " + soql);
        }
        // SourceMember always returns empty — skip WHERE parsing for unsupported operators
        if ("SourceMember".equals(query.objectName())) {
            return List.of();
        }
        return project(query, datasetFor(query.objectName()), true);
    }

    public String inferEntityTypeName(String soql) {
        return parse(soql).objectName();
    }

    public List<Map<String, Object>> executeStandardMetadataQuery(String soql) {
        ParsedQuery query;
        try {
            query = parse(soql);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
        if (!"FlowDefinitionView".equals(query.objectName())) {
            return List.of();
        }
        return project(query, datasetFor(query.objectName()), false);
    }

    private ParsedQuery parse(String soql) {
        Matcher matcher = QUERY_PATTERN.matcher(soql.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported tooling query: " + soql);
        }

        List<String> fields = List.of(matcher.group(1).split("\\s*,\\s*"));
        String objectName = matcher.group(2).trim();
        Condition condition = parseCondition(matcher.group(3));
        return new ParsedQuery(fields, objectName, condition);
    }

    private Condition parseCondition(String rawWhereClause) {
        if (rawWhereClause == null || rawWhereClause.isBlank()) {
            return null;
        }

        String trimmed = rawWhereClause.trim();

        Matcher inMatcher = IN_PATTERN.matcher(trimmed);
        if (inMatcher.matches()) {
            String field = inMatcher.group(1).trim();
            Set<String> values = Arrays.stream(inMatcher.group(2).split(","))
                    .map(String::trim)
                    .map(v -> v.startsWith("'") && v.endsWith("'") ? v.substring(1, v.length() - 1) : v)
                    .collect(Collectors.toSet());
            return Condition.in(field, values);
        }

        Matcher matcher = EQUALITY_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            // Unsupported operator — return null so caller gets all records
            return null;
        }

        String field = matcher.group(1).trim();
        String rawValue = matcher.group(2).trim();
        Object value;
        if (rawValue.startsWith("'") && rawValue.endsWith("'")) {
            value = rawValue.substring(1, rawValue.length() - 1);
        } else if ("true".equalsIgnoreCase(rawValue) || "false".equalsIgnoreCase(rawValue)) {
            value = Boolean.parseBoolean(rawValue);
        } else {
            value = rawValue;
        }
        return Condition.eq(field, value);
    }

    private List<Map<String, Object>> project(ParsedQuery query, List<Map<String, Object>> dataset, boolean toolingUrl) {
        return dataset.stream()
                .filter(record -> matches(record, query.condition()))
                .map(record -> projectRecord(query.objectName(), query.fields(), record, toolingUrl))
                .toList();
    }

    private boolean matches(Map<String, Object> record, Condition condition) {
        if (condition == null) {
            return true;
        }
        if (condition.isIn()) {
            Object current = record.get(condition.field());
            if (current == null) return false;
            return condition.inValues().stream()
                    .anyMatch(v -> String.valueOf(current).equalsIgnoreCase(v));
        }
        Object current = record.get(condition.field());
        if (current == null) {
            return false;
        }
        if (current instanceof Boolean bool && condition.value() instanceof Boolean expectedBool) {
            return bool.equals(expectedBool);
        }
        return String.valueOf(current).equalsIgnoreCase(String.valueOf(condition.value()));
    }

    private Map<String, Object> projectRecord(String objectName, List<String> fields, Map<String, Object> record, boolean toolingUrl) {
        Map<String, Object> result = new LinkedHashMap<>();
        String prefix = toolingUrl ? "/services/data/v60.0/tooling/sobjects/" : "/services/data/v60.0/sobjects/";
        result.put("attributes", Map.of("type", objectName, "url", prefix + objectName));
        for (String field : fields) {
            result.put(field, record.get(field));
        }
        return result;
    }

    private List<Map<String, Object>> datasetFor(String objectName) {
        return switch (objectName) {
            case "TabDefinition" -> List.of(row(
                    entry("Name", "standard-Account"),
                    entry("DurableId", "TabDefinition/standard-Account")
            ));
            case "CustomApplication" -> metadataService.listResources().stream()
                    .filter(resource -> "CustomApplication".equals(resource.type()))
                    .map(resource -> row(
                            entry("DeveloperName", resource.fullName()),
                            entry("NamespacePrefix", null)
                    ))
                    .toList();
            case "EntityDefinition" -> List.of(row(
                    entry("QualifiedApiName", "FeatureFlags__c"),
                    entry("IsCustomSetting", true)
            ));
            case "FieldDefinition" -> List.of(
                    row(entry("EntityDefinition.QualifiedApiName", "Account"), entry("QualifiedApiName", "Account.IsHistoryTracked__c"), entry("IsHistoryTracked", false)),
                    row(entry("EntityDefinition.QualifiedApiName", "Contact"), entry("QualifiedApiName", "Contact.IsHistoryTracked__c"), entry("IsHistoryTracked", false))
            );
            case "SourceMember" -> List.of();
            case "FlowDefinition" -> metadataService.listResources().stream()
                    .filter(resource -> "FlowDefinition".equals(resource.type()))
                    .map(resource -> row(
                            entry("DeveloperName", resource.fullName()),
                            entry("NamespacePrefix", null)
                    ))
                    .toList();
            case "Flow" -> List.of(
                    row(entry("DefinitionId", "FlowDefinition/LoginFlow"), entry("VersionNumber", 1)),
                    row(entry("DefinitionId", "FlowDefinition/LoginFlow"), entry("VersionNumber", 2))
            );
            case "FlowDefinitionView" -> metadataService.listResources().stream()
                    .filter(resource -> "FlowDefinition".equals(resource.type()))
                    .map(resource -> row(
                            entry("ApiName", resource.fullName()),
                            entry("DurableId", "FlowDefinition/" + resource.fullName())
                    ))
                    .toList();
            case "StaticResource" -> orgStateService.findByType("StaticResource").stream()
                    .map(record -> orgStateService.fromJson(record.getFieldsJson()))
                    .toList();
            default -> throw new IllegalArgumentException("Unsupported tooling query object: " + objectName);
        };
    }

    @SafeVarargs
    private Map<String, Object> row(Map.Entry<String, Object>... entries) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : entries) {
            values.put(entry.getKey(), entry.getValue());
        }
        return values;
    }

    private Map.Entry<String, Object> entry(String key, Object value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    private record ParsedQuery(List<String> fields, String objectName, Condition condition) {
    }

    private record Condition(String field, Object value, Set<String> inValues) {
        static Condition eq(String field, Object value) {
            return new Condition(field, value, null);
        }
        static Condition in(String field, Set<String> values) {
            return new Condition(field, null, values);
        }
        boolean isIn() {
            return inValues != null;
        }
    }
}
