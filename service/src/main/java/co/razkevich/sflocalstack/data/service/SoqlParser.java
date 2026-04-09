package co.razkevich.sflocalstack.data.service;

import java.util.ArrayList;
import java.util.List;

public class SoqlParser {

    private final List<SoqlToken> tokens;
    private int pos;

    public SoqlParser(List<SoqlToken> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    public SoqlAst.SelectStatement parse() {
        expect(SoqlToken.Type.SELECT);

        // Check for COUNT()
        boolean isCount = false;
        List<SoqlAst.FieldRef> fields;
        if (check(SoqlToken.Type.COUNT)) {
            int savedPos = pos;
            advance(); // consume COUNT
            if (check(SoqlToken.Type.LPAREN)) {
                advance(); // consume (
                expect(SoqlToken.Type.RPAREN); // consume )
                isCount = true;
                fields = List.of();
            } else {
                // Not COUNT(), backtrack and parse as fields
                pos = savedPos;
                fields = parseFieldList();
            }
        } else {
            fields = parseFieldList();
        }

        expect(SoqlToken.Type.FROM);
        String fromObject = parseObjectName();

        SoqlAst.Expression where = null;
        if (check(SoqlToken.Type.WHERE)) {
            advance();
            where = parseOrExpr();
        }

        List<SoqlAst.OrderByField> orderBy = List.of();
        if (check(SoqlToken.Type.ORDER)) {
            advance();
            expect(SoqlToken.Type.BY);
            orderBy = parseOrderByList();
        }

        Integer limit = null;
        if (check(SoqlToken.Type.LIMIT)) {
            advance();
            limit = parseInteger();
        }

        Integer offset = null;
        if (check(SoqlToken.Type.OFFSET)) {
            advance();
            offset = parseInteger();
        }

        if (!check(SoqlToken.Type.EOF)) {
            throw error("Unexpected token '" + current().value() + "'");
        }

        return new SoqlAst.SelectStatement(fields, fromObject, where, orderBy, limit, offset, isCount);
    }

    // --- Field list parsing ---

    private List<SoqlAst.FieldRef> parseFieldList() {
        List<SoqlAst.FieldRef> fields = new ArrayList<>();
        fields.add(parseFieldRef());
        while (check(SoqlToken.Type.COMMA)) {
            advance();
            fields.add(parseFieldRef());
        }
        return fields;
    }

    private SoqlAst.FieldRef parseFieldRef() {
        StringBuilder path = new StringBuilder();
        path.append(expectIdentifier());
        while (check(SoqlToken.Type.DOT)) {
            advance();
            path.append('.').append(expectIdentifier());
        }
        return new SoqlAst.FieldRef(path.toString());
    }

    private String parseObjectName() {
        return expectIdentifier();
    }

    // --- WHERE expression parsing (recursive descent with proper precedence) ---

    private SoqlAst.Expression parseOrExpr() {
        SoqlAst.Expression left = parseAndExpr();
        while (check(SoqlToken.Type.OR)) {
            advance();
            SoqlAst.Expression right = parseAndExpr();
            left = new SoqlAst.LogicalExpr(left, "OR", right);
        }
        return left;
    }

    private SoqlAst.Expression parseAndExpr() {
        SoqlAst.Expression left = parsePrimaryExpr();
        while (check(SoqlToken.Type.AND)) {
            advance();
            SoqlAst.Expression right = parsePrimaryExpr();
            left = new SoqlAst.LogicalExpr(left, "AND", right);
        }
        return left;
    }

    private SoqlAst.Expression parsePrimaryExpr() {
        if (check(SoqlToken.Type.LPAREN)) {
            advance();
            SoqlAst.Expression expr = parseOrExpr();
            expect(SoqlToken.Type.RPAREN);
            return expr;
        }
        return parseComparison();
    }

    private SoqlAst.Expression parseComparison() {
        SoqlAst.FieldRef field = parseFieldRef();

        // field IS [NOT] NULL
        if (check(SoqlToken.Type.IS)) {
            advance();
            if (check(SoqlToken.Type.NOT)) {
                advance();
                expect(SoqlToken.Type.NULL);
                return new SoqlAst.NullCheckExpr(field, true);
            }
            expect(SoqlToken.Type.NULL);
            return new SoqlAst.NullCheckExpr(field, false);
        }

        // field NOT IN (...)
        if (check(SoqlToken.Type.NOT)) {
            advance();
            expect(SoqlToken.Type.IN);
            expect(SoqlToken.Type.LPAREN);
            List<Object> values = parseValueList();
            expect(SoqlToken.Type.RPAREN);
            return new SoqlAst.InExpr(field, values, true);
        }

        // field IN (...)
        if (check(SoqlToken.Type.IN)) {
            advance();
            expect(SoqlToken.Type.LPAREN);
            List<Object> values = parseValueList();
            expect(SoqlToken.Type.RPAREN);
            return new SoqlAst.InExpr(field, values, false);
        }

        // field LIKE 'pattern'
        if (check(SoqlToken.Type.LIKE)) {
            advance();
            String pattern = expectStringLiteral();
            return new SoqlAst.LikeExpr(field, pattern);
        }

        // field op value
        String operator = parseOperator();
        Object value = parseLiteralValue();
        return new SoqlAst.ComparisonExpr(field, operator, value);
    }

    private List<Object> parseValueList() {
        List<Object> values = new ArrayList<>();
        values.add(parseLiteralValue());
        while (check(SoqlToken.Type.COMMA)) {
            advance();
            values.add(parseLiteralValue());
        }
        return values;
    }

    private Object parseLiteralValue() {
        SoqlToken token = current();
        return switch (token.type()) {
            case STRING_LITERAL -> {
                advance();
                yield token.value();
            }
            case NUMBER_LITERAL -> {
                advance();
                String num = token.value();
                if (num.contains(".")) {
                    yield Double.parseDouble(num);
                }
                yield Long.parseLong(num);
            }
            case TRUE -> {
                advance();
                yield Boolean.TRUE;
            }
            case FALSE -> {
                advance();
                yield Boolean.FALSE;
            }
            case NULL -> {
                advance();
                yield null;
            }
            default -> throw error("Expected literal value, got '" + token.value() + "'");
        };
    }

    private String parseOperator() {
        SoqlToken token = current();
        return switch (token.type()) {
            case EQ -> { advance(); yield "="; }
            case NEQ -> { advance(); yield "!="; }
            case LT -> { advance(); yield "<"; }
            case GT -> { advance(); yield ">"; }
            case LTE -> { advance(); yield "<="; }
            case GTE -> { advance(); yield ">="; }
            default -> throw error("Expected comparison operator, got '" + token.value() + "'");
        };
    }

    // --- ORDER BY parsing ---

    private List<SoqlAst.OrderByField> parseOrderByList() {
        List<SoqlAst.OrderByField> list = new ArrayList<>();
        list.add(parseOrderByField());
        while (check(SoqlToken.Type.COMMA)) {
            advance();
            list.add(parseOrderByField());
        }
        return list;
    }

    private SoqlAst.OrderByField parseOrderByField() {
        SoqlAst.FieldRef field = parseFieldRef();
        boolean ascending = true;
        if (check(SoqlToken.Type.ASC)) {
            advance();
        } else if (check(SoqlToken.Type.DESC)) {
            advance();
            ascending = false;
        }
        Boolean nullsFirst = null;
        if (check(SoqlToken.Type.NULLS)) {
            advance();
            if (check(SoqlToken.Type.FIRST)) {
                advance();
                nullsFirst = Boolean.TRUE;
            } else if (check(SoqlToken.Type.LAST)) {
                advance();
                nullsFirst = Boolean.FALSE;
            } else {
                throw error("Expected FIRST or LAST after NULLS");
            }
        }
        return new SoqlAst.OrderByField(field, ascending, nullsFirst);
    }

    // --- Utility ---

    private int parseInteger() {
        SoqlToken token = current();
        if (token.type() != SoqlToken.Type.NUMBER_LITERAL) {
            throw error("Expected integer, got '" + token.value() + "'");
        }
        advance();
        return Integer.parseInt(token.value());
    }

    private String expectIdentifier() {
        SoqlToken token = current();
        // Allow keywords to be used as identifiers (field/object names)
        if (token.type() == SoqlToken.Type.IDENTIFIER || isKeywordUsableAsIdentifier(token.type())) {
            advance();
            return token.value();
        }
        throw error("Expected identifier, got '" + token.value() + "' (" + token.type() + ")");
    }

    private boolean isKeywordUsableAsIdentifier(SoqlToken.Type type) {
        // In Salesforce SOQL, certain keywords can appear as field/object names
        // when context makes it unambiguous. We allow most keywords as identifiers.
        return switch (type) {
            case COUNT, FIRST, LAST, ASC, DESC, NULLS, ORDER, BY,
                 LIMIT, OFFSET, IN, LIKE, IS, NOT, NULL, TRUE, FALSE,
                 AND, OR, FROM, WHERE, SELECT -> true;
            default -> false;
        };
    }

    private String expectStringLiteral() {
        SoqlToken token = current();
        if (token.type() != SoqlToken.Type.STRING_LITERAL) {
            throw error("Expected string literal, got '" + token.value() + "'");
        }
        advance();
        return token.value();
    }

    private void expect(SoqlToken.Type type) {
        SoqlToken token = current();
        if (token.type() != type) {
            throw error("Expected " + type + ", got '" + token.value() + "' (" + token.type() + ")");
        }
        advance();
    }

    private boolean check(SoqlToken.Type type) {
        return current().type() == type;
    }

    private SoqlToken current() {
        return tokens.get(pos);
    }

    private void advance() {
        if (pos < tokens.size() - 1) {
            pos++;
        }
    }

    private IllegalArgumentException error(String message) {
        SoqlToken token = current();
        return new IllegalArgumentException(
                "SOQL parse error at position " + token.position() + ": " + message);
    }
}
