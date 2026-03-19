package co.prodly.sflocalstack.service;

import co.prodly.sflocalstack.model.SObjectRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Simple SOQL engine that handles basic SELECT queries.
 * TODO: apex-parser -> H2 SQL transpilation for full SOQL support.
 */
@Service
public class SoqlEngine {

    private static final Logger log = LoggerFactory.getLogger(SoqlEngine.class);

    // Matches: SELECT <fields> FROM <object> [WHERE ...] [LIMIT n]
    private static final Pattern SELECT_PATTERN = Pattern.compile(
            "(?i)^\\s*SELECT\\s+(.+?)\\s+FROM\\s+(\\w+)(?:\\s+WHERE\\s+.+?)?(?:\\s+LIMIT\\s+(\\d+))?\\s*$"
    );

    private final OrgStateService orgStateService;

    public SoqlEngine(OrgStateService orgStateService) {
        this.orgStateService = orgStateService;
    }

    /**
     * Parse and execute a SOQL query. Returns list of field maps.
     */
    public List<Map<String, Object>> execute(String soql) {
        log.debug("Executing SOQL: {}", soql);

        Matcher matcher = SELECT_PATTERN.matcher(soql.trim());
        if (!matcher.matches()) {
            log.warn("Unsupported SOQL query: {}", soql);
            return List.of();
        }

        String fieldsStr = matcher.group(1).trim();
        String objectType = matcher.group(2).trim();
        String limitStr = matcher.group(3);

        List<String> requestedFields = parseFields(fieldsStr);
        boolean selectAll = requestedFields.size() == 1 && requestedFields.get(0).equals("*");

        List<SObjectRecord> records = orgStateService.findByType(objectType);

        List<Map<String, Object>> result = records.stream()
                .map(record -> {
                    Map<String, Object> fields = orgStateService.fromJson(record.getFieldsJson());
                    if (selectAll) {
                        return fields;
                    }
                    return requestedFields.stream()
                            .filter(fields::containsKey)
                            .collect(Collectors.toMap(f -> f, fields::get));
                })
                .collect(Collectors.toList());

        if (limitStr != null) {
            int limit = Integer.parseInt(limitStr);
            result = result.stream().limit(limit).collect(Collectors.toList());
        }

        return result;
    }

    private List<String> parseFields(String fieldsStr) {
        if (fieldsStr.equals("*")) {
            return List.of("*");
        }
        return List.of(fieldsStr.split("\\s*,\\s*"));
    }
}
