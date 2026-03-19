package co.prodly.sflocalstack.service;

import org.springframework.stereotype.Service;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetadataToolingService {

    public List<Map<String, Object>> executeToolingQuery(String soql) {
        if (soql.equalsIgnoreCase("SELECT Name FROM TabDefinition")) {
            return List.of(record("TabDefinition", fields(
                    entry("Name", "standard-Account"),
                    entry("DurableId", "TabDefinition/standard-Account")
            )));
        }
        if (soql.equalsIgnoreCase("SELECT DeveloperName, NamespacePrefix FROM CustomApplication")) {
            return List.of(record("CustomApplication", fields(
                    entry("DeveloperName", "SalesConsole"),
                    entry("NamespacePrefix", null)
            )));
        }
        if (soql.equalsIgnoreCase("SELECT QualifiedApiName FROM EntityDefinition WHERE IsCustomSetting = true")) {
            return List.of(record("EntityDefinition", fields(entry("QualifiedApiName", "FeatureFlags__c"))));
        }
        if (soql.equalsIgnoreCase("SELECT DeveloperName, NamespacePrefix FROM FlowDefinition")) {
            return List.of(record("FlowDefinition", fields(
                    entry("DeveloperName", "LoginFlow"),
                    entry("NamespacePrefix", null)
            )));
        }
        if (soql.equalsIgnoreCase("SELECT VersionNumber FROM Flow WHERE DefinitionId = 'FlowDefinition/LoginFlow'")) {
            return List.of(
                    record("Flow", fields(entry("VersionNumber", 1))),
                    record("Flow", fields(entry("VersionNumber", 2)))
            );
        }
        if (soql.equalsIgnoreCase("SELECT DurableId FROM FlowDefinitionView WHERE ApiName = 'LoginFlow'")) {
            return List.of(record("FlowDefinitionView", fields(entry("DurableId", "FlowDefinition/LoginFlow"))));
        }
        throw new IllegalArgumentException("Unsupported tooling query: " + soql);
    }

    public List<Map<String, Object>> executeStandardMetadataQuery(String soql) {
        if (soql.equalsIgnoreCase("SELECT DurableId FROM FlowDefinitionView WHERE ApiName = 'LoginFlow'")) {
            return List.of(record("FlowDefinitionView", fields(entry("DurableId", "FlowDefinition/LoginFlow"))));
        }
        return List.of();
    }

    private Map<String, Object> record(String type, Map<String, Object> fields) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attributes", Map.of("type", type, "url", "/services/data/v60.0/tooling/sobjects/" + type));
        result.putAll(fields);
        return result;
    }

    @SafeVarargs
    private Map<String, Object> fields(Map.Entry<String, Object>... entries) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : entries) {
            values.put(entry.getKey(), entry.getValue());
        }
        return values;
    }

    private Map.Entry<String, Object> entry(String key, Object value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }
}
