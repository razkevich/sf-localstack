package co.razkevich.sflocalstack.data.service;

public record SoqlToken(Type type, String value, int position) {

    public enum Type {
        // Keywords
        SELECT, FROM, WHERE, AND, OR, NOT, IN, LIKE, IS, NULL,
        ORDER, BY, ASC, DESC, NULLS, FIRST, LAST,
        LIMIT, OFFSET, COUNT,
        TRUE, FALSE,

        // Literals and identifiers
        IDENTIFIER, STRING_LITERAL, NUMBER_LITERAL,

        // Symbols
        COMMA, DOT, LPAREN, RPAREN,

        // Operators
        EQ, NEQ, LT, GT, LTE, GTE,

        // End
        EOF
    }
}
