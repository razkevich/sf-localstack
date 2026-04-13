package co.razkevich.sflocalstack.data.service;

import co.razkevich.sflocalstack.data.model.SObjectRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SoqlEngine {

    private static final Logger log = LoggerFactory.getLogger(SoqlEngine.class);

    private final OrgStateService orgStateService;

    public SoqlEngine(OrgStateService orgStateService) {
        this.orgStateService = orgStateService;
    }

    public List<Map<String, Object>> execute(String soql) {
        return execute(null, soql);
    }

    public List<Map<String, Object>> execute(String orgId, String soql) {
        log.debug("Executing SOQL: {}", soql);

        SoqlAst.SelectStatement query = parse(soql);
        String objectType = query.fromObject();
        List<SObjectRecord> records = orgStateService.findByType(orgId, objectType);

        List<Map<String, Object>> filtered = records.stream()
                .map(record -> orgStateService.fromJson(record.getFieldsJson()))
                .filter(fields -> matchesExpression(objectType, fields, query.where()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        if (query.isCount()) {
            return List.of(Map.of("expr0", filtered.size()));
        }

        // ORDER BY
        if (!query.orderBy().isEmpty()) {
            filtered.sort(buildComparator(objectType, query.orderBy()));
        }

        // OFFSET
        int startIndex = 0;
        if (query.offset() != null) {
            startIndex = Math.min(query.offset(), filtered.size());
        }

        // LIMIT
        int endIndex = filtered.size();
        if (query.limit() != null) {
            endIndex = Math.min(startIndex + query.limit(), filtered.size());
        }

        List<Map<String, Object>> paged = filtered.subList(startIndex, endIndex);

        return paged.stream()
                .map(fields -> projectFields(objectType, fields, query.fields()))
                .toList();
    }

    private SoqlAst.SelectStatement parse(String soql) {
        try {
            List<SoqlToken> tokens = new SoqlLexer(soql.trim()).tokenize();
            return new SoqlParser(tokens).parse();
        } catch (IllegalArgumentException e) {
            log.warn("Unsupported SOQL query: {}", soql);
            throw new IllegalArgumentException("Unsupported SOQL query: " + soql + " (" + e.getMessage() + ")", e);
        }
    }

    // --- Expression evaluation ---

    private boolean matchesExpression(String objectType, Map<String, Object> fields, SoqlAst.Expression expr) {
        if (expr == null) {
            return true;
        }
        return switch (expr) {
            case SoqlAst.ComparisonExpr comp -> evaluateComparison(objectType, fields, comp);
            case SoqlAst.LogicalExpr logical -> evaluateLogical(objectType, fields, logical);
            case SoqlAst.InExpr in -> evaluateIn(objectType, fields, in);
            case SoqlAst.LikeExpr like -> evaluateLike(objectType, fields, like);
            case SoqlAst.NullCheckExpr nullCheck -> evaluateNullCheck(objectType, fields, nullCheck);
        };
    }

    private boolean evaluateComparison(String objectType, Map<String, Object> fields, SoqlAst.ComparisonExpr comp) {
        Object fieldValue = orgStateService.resolveFieldValue(objectType, fields, comp.field().path());
        Object expected = comp.value();

        if (expected == null) {
            return switch (comp.operator()) {
                case "=" -> fieldValue == null;
                case "!=" -> fieldValue != null;
                default -> false;
            };
        }

        return switch (comp.operator()) {
            case "=" -> valuesEqual(fieldValue, expected);
            case "!=" -> !valuesEqual(fieldValue, expected);
            case "<" -> compareValues(fieldValue, expected) < 0;
            case ">" -> compareValues(fieldValue, expected) > 0;
            case "<=" -> compareValues(fieldValue, expected) <= 0;
            case ">=" -> compareValues(fieldValue, expected) >= 0;
            default -> false;
        };
    }

    private boolean evaluateLogical(String objectType, Map<String, Object> fields, SoqlAst.LogicalExpr logical) {
        return switch (logical.operator()) {
            case "AND" -> matchesExpression(objectType, fields, logical.left())
                    && matchesExpression(objectType, fields, logical.right());
            case "OR" -> matchesExpression(objectType, fields, logical.left())
                    || matchesExpression(objectType, fields, logical.right());
            default -> false;
        };
    }

    private boolean evaluateIn(String objectType, Map<String, Object> fields, SoqlAst.InExpr in) {
        Object fieldValue = orgStateService.resolveFieldValue(objectType, fields, in.field().path());
        boolean found = in.values().stream().anyMatch(v -> valuesEqual(fieldValue, v));
        return in.negated() ? !found : found;
    }

    private boolean evaluateLike(String objectType, Map<String, Object> fields, SoqlAst.LikeExpr like) {
        Object fieldValue = orgStateService.resolveFieldValue(objectType, fields, like.field().path());
        if (!(fieldValue instanceof String stringValue)) {
            return false;
        }
        return likeMatches(stringValue, like.pattern());
    }

    private boolean evaluateNullCheck(String objectType, Map<String, Object> fields, SoqlAst.NullCheckExpr nullCheck) {
        Object fieldValue = orgStateService.resolveFieldValue(objectType, fields, nullCheck.field().path());
        return nullCheck.isNotNull() ? fieldValue != null : fieldValue == null;
    }

    // --- ORDER BY ---

    private Comparator<Map<String, Object>> buildComparator(String objectType, List<SoqlAst.OrderByField> orderByFields) {
        Comparator<Map<String, Object>> comparator = null;
        for (SoqlAst.OrderByField orderByField : orderByFields) {
            Comparator<Map<String, Object>> fieldComparator = (a, b) -> {
                Object va = orgStateService.resolveFieldValue(objectType, a, orderByField.field().path());
                Object vb = orgStateService.resolveFieldValue(objectType, b, orderByField.field().path());
                return compareForSort(va, vb, orderByField.ascending(), orderByField.nullsFirst());
            };
            comparator = comparator == null ? fieldComparator : comparator.thenComparing(fieldComparator);
        }
        return comparator;
    }

    @SuppressWarnings("unchecked")
    private int compareForSort(Object a, Object b, boolean ascending, Boolean nullsFirst) {
        if (a == null && b == null) return 0;
        if (a == null) {
            boolean nullFirst = nullsFirst != null ? nullsFirst : ascending;
            return nullFirst ? -1 : 1;
        }
        if (b == null) {
            boolean nullFirst = nullsFirst != null ? nullsFirst : ascending;
            return nullFirst ? 1 : -1;
        }

        int result;
        if (a instanceof Number na && b instanceof Number nb) {
            result = Double.compare(na.doubleValue(), nb.doubleValue());
        } else if (a instanceof Comparable ca && b instanceof Comparable) {
            result = ca.compareTo(b);
        } else {
            result = String.valueOf(a).compareToIgnoreCase(String.valueOf(b));
        }

        return ascending ? result : -result;
    }

    // --- Projection ---

    private Map<String, Object> projectFields(String objectType, Map<String, Object> fields, List<SoqlAst.FieldRef> selectedFields) {
        Map<String, Object> projected = new LinkedHashMap<>();
        for (SoqlAst.FieldRef fieldRef : selectedFields) {
            String field = fieldRef.path();
            String canonicalField = orgStateService.canonicalFieldPath(fields, field);
            Object resolved = orgStateService.resolveFieldValue(objectType, fields, field);
            if (resolved != null || !field.contains(".")) {
                projected.put(canonicalField, resolved);
            }
        }
        return orgStateService.toSalesforceRecord("v60.0", objectType, projected);
    }

    // --- Value comparison utilities ---

    private boolean valuesEqual(Object fieldValue, Object expected) {
        if (fieldValue == null && expected == null) {
            return true;
        }
        if (fieldValue == null || expected == null) {
            return false;
        }
        if (fieldValue instanceof Number number && expected instanceof Number expNumber) {
            return Double.compare(number.doubleValue(), expNumber.doubleValue()) == 0;
        }
        if (expected instanceof Number expNumber) {
            if (fieldValue instanceof String) {
                try {
                    return Double.compare(Double.parseDouble((String) fieldValue), expNumber.doubleValue()) == 0;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        if (fieldValue instanceof Number number && expected instanceof String expStr) {
            try {
                return Double.compare(number.doubleValue(), Double.parseDouble(expStr)) == 0;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        if (expected instanceof Boolean expBool) {
            return expBool.toString().equalsIgnoreCase(String.valueOf(fieldValue));
        }
        return String.valueOf(fieldValue).equalsIgnoreCase(String.valueOf(expected));
    }

    private int compareValues(Object fieldValue, Object expected) {
        if (fieldValue == null) {
            return -1;
        }
        if (fieldValue instanceof Number number && expected instanceof Number expNumber) {
            return Double.compare(number.doubleValue(), expNumber.doubleValue());
        }
        return String.valueOf(fieldValue).compareToIgnoreCase(String.valueOf(expected));
    }

    private boolean likeMatches(String actual, String pattern) {
        String regex = pattern
                .replace(".", "\\.")
                .replace("%", ".*")
                .replace("_", ".");
        return actual.toLowerCase(Locale.ROOT).matches(regex.toLowerCase(Locale.ROOT));
    }
}
