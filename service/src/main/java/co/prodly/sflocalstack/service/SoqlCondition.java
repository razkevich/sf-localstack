package co.prodly.sflocalstack.service;

public record SoqlCondition(
        String field,
        Operator operator,
        String rawValue,
        boolean nullLiteral
) {
    public enum Operator {
        EQUALS,
        NOT_EQUALS,
        LIKE,
        IS_NULL,
        IS_NOT_NULL
    }
}
