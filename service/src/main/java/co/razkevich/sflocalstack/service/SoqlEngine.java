package co.razkevich.sflocalstack.service;

import co.razkevich.sflocalstack.model.SObjectRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SoqlEngine {

    private static final Logger log = LoggerFactory.getLogger(SoqlEngine.class);

    private static final Pattern SELECT_PATTERN = Pattern.compile(
            "(?is)^\\s*SELECT\\s+(.+?)\\s+FROM\\s+(\\w+)\\s*(?:WHERE\\s+(.+?))?\\s*(?:ORDER\\s+BY\\s+.+?)?\\s*(?:LIMIT\\s+(\\d+))?\\s*$"
    );
    private static final Pattern NULL_PATTERN = Pattern.compile("(?i)^(\\S+)\\s+IS\\s+(NOT\\s+)?NULL$");
    private static final Pattern LIKE_PATTERN = Pattern.compile("(?i)^(\\S+)\\s+LIKE\\s+'(.*)'$");
    private static final Pattern COMPARISON_PATTERN = Pattern.compile("(?i)^(\\S+)\\s*(=|!=|<>|<=|>=|<|>)\\s*(.+)$");

    private final OrgStateService orgStateService;

    public SoqlEngine(OrgStateService orgStateService) {
        this.orgStateService = orgStateService;
    }

    public List<Map<String, Object>> execute(String soql) {
        log.debug("Executing SOQL: {}", soql);

        SoqlQueryModel query = parse(soql);
        List<SObjectRecord> records = orgStateService.findByType(query.objectType());

        List<Map<String, Object>> filtered = records.stream()
                .map(record -> orgStateService.fromJson(record.getFieldsJson()))
                .filter(fields -> matchesAll(query.objectType(), fields, query.conditions()))
                .toList();

        if (query.countQuery()) {
            return List.of(Map.of("expr0", filtered.size()));
        }

        List<Map<String, Object>> projected = filtered.stream()
                .map(fields -> projectFields(query.objectType(), fields, query.selectedFields()))
                .toList();

        if (query.limit() != null && projected.size() > query.limit()) {
            return projected.subList(0, query.limit());
        }

        return projected;
    }

    private SoqlQueryModel parse(String soql) {
        Matcher matcher = SELECT_PATTERN.matcher(soql.trim());
        if (!matcher.matches()) {
            log.warn("Unsupported SOQL query: {}", soql);
            throw new IllegalArgumentException("Unsupported SOQL query: " + soql);
        }

        String fieldsStr = matcher.group(1).trim();
        String objectType = matcher.group(2).trim();
        String whereClause = matcher.group(3);
        String limitStr = matcher.group(4);

        boolean countQuery = "COUNT()".equalsIgnoreCase(fieldsStr);
        List<String> selectedFields = countQuery ? List.of() : parseFields(fieldsStr);
        List<SoqlCondition> conditions = whereClause == null ? List.of() : parseConditions(stripOuterParens(whereClause));
        Integer limit = limitStr == null ? null : Integer.parseInt(limitStr);

        return new SoqlQueryModel(selectedFields, objectType, conditions, limit, countQuery);
    }

    private List<String> parseFields(String fieldsStr) {
        List<String> fields = new ArrayList<>();
        for (String field : fieldsStr.split("\\s*,\\s*")) {
            String trimmed = field.trim();
            if (!trimmed.isEmpty()) {
                fields.add(trimmed);
            }
        }
        return fields;
    }

    private List<SoqlCondition> parseConditions(String whereClause) {
        List<SoqlCondition> conditions = new ArrayList<>();
        for (String rawCondition : whereClause.split("(?i)\\s+AND\\s+")) {
            String condition = rawCondition.trim();

            Matcher nullMatcher = NULL_PATTERN.matcher(condition);
            if (nullMatcher.matches()) {
                conditions.add(new SoqlCondition(
                        nullMatcher.group(1),
                        nullMatcher.group(2) == null ? SoqlCondition.Operator.IS_NULL : SoqlCondition.Operator.IS_NOT_NULL,
                        null,
                        true
                ));
                continue;
            }

            Matcher likeMatcher = LIKE_PATTERN.matcher(condition);
            if (likeMatcher.matches()) {
                conditions.add(new SoqlCondition(
                        likeMatcher.group(1),
                        SoqlCondition.Operator.LIKE,
                        likeMatcher.group(2),
                        false
                ));
                continue;
            }

            // jOOQ trueCondition() renders as (1 = 1) standalone — always true, skip
            if ("(1 = 1)".equals(condition)) {
                continue;
            }

            Matcher comparisonMatcher = COMPARISON_PATTERN.matcher(condition);
            if (comparisonMatcher.matches()) {
                String rawValue = comparisonMatcher.group(3).trim();
                boolean nullLiteral = "null".equalsIgnoreCase(rawValue);
                conditions.add(new SoqlCondition(
                        comparisonMatcher.group(1),
                        ("!=".equals(comparisonMatcher.group(2)) || "<>".equals(comparisonMatcher.group(2))) ? SoqlCondition.Operator.NOT_EQUALS : SoqlCondition.Operator.EQUALS,
                        stripQuotes(rawValue),
                        nullLiteral
                ));
                continue;
            }

            throw new IllegalArgumentException("Unsupported WHERE condition: " + condition);
        }

        return conditions;
    }

    private boolean matchesAll(String objectType, Map<String, Object> fields, List<SoqlCondition> conditions) {
        for (SoqlCondition condition : conditions) {
            Object fieldValue = orgStateService.resolveFieldValue(objectType, fields, condition.field());
            if (!matchesCondition(fieldValue, condition)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesCondition(Object fieldValue, SoqlCondition condition) {
        return switch (condition.operator()) {
            case IS_NULL -> fieldValue == null;
            case IS_NOT_NULL -> fieldValue != null;
            case LIKE -> fieldValue instanceof String stringValue && likeMatches(stringValue, condition.rawValue());
            case EQUALS -> condition.nullLiteral() ? fieldValue == null : valuesEqual(fieldValue, condition.rawValue());
            case NOT_EQUALS -> condition.nullLiteral() ? fieldValue != null : !valuesEqual(fieldValue, condition.rawValue());
        };
    }

    private Map<String, Object> projectFields(String objectType, Map<String, Object> fields, List<String> selectedFields) {
        Map<String, Object> projected = new LinkedHashMap<>();
        for (String field : selectedFields) {
            String canonicalField = orgStateService.canonicalFieldPath(fields, field);
            Object resolved = orgStateService.resolveFieldValue(objectType, fields, field);
            if (resolved != null || !field.contains(".")) {
                projected.put(canonicalField, resolved);
            }
        }
        return orgStateService.toSalesforceRecord("v60.0", objectType, projected);
    }

    private boolean likeMatches(String actual, String pattern) {
        String regex = pattern
                .replace(".", "\\.")
                .replace("%", ".*")
                .replace("_", ".");
        return actual.toLowerCase(Locale.ROOT).matches(regex.toLowerCase(Locale.ROOT));
    }

    private boolean valuesEqual(Object fieldValue, String expected) {
        if (fieldValue == null) {
            return false;
        }
        // jOOQ trueCondition() renders as (1 = 1) — treat as boolean true
        if ("(1 = 1)".equals(expected)) {
            expected = "true";
        }
        if (fieldValue instanceof Number number) {
            try {
                return Double.compare(number.doubleValue(), Double.parseDouble(expected)) == 0;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return String.valueOf(fieldValue).equalsIgnoreCase(expected);
    }

    private String stripQuotes(String rawValue) {
        if (rawValue.startsWith("'") && rawValue.endsWith("'") && rawValue.length() >= 2) {
            return rawValue.substring(1, rawValue.length() - 1);
        }
        return rawValue;
    }

    private String stripOuterParens(String clause) {
        String s = clause.trim();
        while (s.startsWith("(") && s.endsWith(")") && matchingParen(s, 0) == s.length() - 1) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    private int matchingParen(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            if (s.charAt(i) == '(') depth++;
            else if (s.charAt(i) == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }
}
