package co.razkevich.sflocalstack.metadata.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Extracts typed member requests from the SOAP parsed values map produced by
 * {@link MetadataSoapParser} for a {@code retrieve} operation.
 */
@Service
public class MetadataManifestParser {

    public record TypeRequest(String type, List<String> members) {}

    /**
     * Extracts type requests from the parsed SOAP values of a {@code retrieve} envelope.
     * Handles both single and multi-member {@code <types>} entries and {@code *} wildcards.
     */
    public List<TypeRequest> extractTypeRequests(Map<String, Object> values) {
        List<TypeRequest> requests = new ArrayList<>();

        // SF CLI uses <request> while Metadata API tests use <retrieveRequest>
        Object rawRequest = values.get("retrieveRequest");
        if (rawRequest == null) {
            rawRequest = values.get("request");
        }
        List<Map<String, Object>> retrieveRequests = asList(rawRequest);
        if (retrieveRequests.isEmpty()) {
            return requests;
        }

        Map<String, Object> retrieveRequest = retrieveRequests.getFirst();
        List<Map<String, Object>> unpackaged = asList(retrieveRequest.get("unpackaged"));
        if (unpackaged.isEmpty()) {
            return requests;
        }

        Map<String, Object> manifest = unpackaged.getFirst();
        List<Map<String, Object>> types = asList(manifest.get("types"));

        for (Map<String, Object> typeEntry : types) {
            String typeName = String.valueOf(typeEntry.getOrDefault("name", ""));
            List<String> members = extractMembers(typeEntry.get("members"));
            if (!typeName.isBlank() && !members.isEmpty()) {
                requests.add(new TypeRequest(typeName, members));
            }
        }

        return requests;
    }

    private List<String> extractMembers(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        String value = String.valueOf(raw).trim();
        return value.isBlank() ? List.of() : List.of(value);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asList(Object raw) {
        if (raw instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }
}
