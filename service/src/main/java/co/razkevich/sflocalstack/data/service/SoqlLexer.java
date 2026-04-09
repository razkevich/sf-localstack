package co.razkevich.sflocalstack.data.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SoqlLexer {

    private static final Map<String, SoqlToken.Type> KEYWORDS = Map.ofEntries(
            Map.entry("SELECT", SoqlToken.Type.SELECT),
            Map.entry("FROM", SoqlToken.Type.FROM),
            Map.entry("WHERE", SoqlToken.Type.WHERE),
            Map.entry("AND", SoqlToken.Type.AND),
            Map.entry("OR", SoqlToken.Type.OR),
            Map.entry("NOT", SoqlToken.Type.NOT),
            Map.entry("IN", SoqlToken.Type.IN),
            Map.entry("LIKE", SoqlToken.Type.LIKE),
            Map.entry("IS", SoqlToken.Type.IS),
            Map.entry("NULL", SoqlToken.Type.NULL),
            Map.entry("ORDER", SoqlToken.Type.ORDER),
            Map.entry("BY", SoqlToken.Type.BY),
            Map.entry("ASC", SoqlToken.Type.ASC),
            Map.entry("DESC", SoqlToken.Type.DESC),
            Map.entry("NULLS", SoqlToken.Type.NULLS),
            Map.entry("FIRST", SoqlToken.Type.FIRST),
            Map.entry("LAST", SoqlToken.Type.LAST),
            Map.entry("LIMIT", SoqlToken.Type.LIMIT),
            Map.entry("OFFSET", SoqlToken.Type.OFFSET),
            Map.entry("COUNT", SoqlToken.Type.COUNT),
            Map.entry("TRUE", SoqlToken.Type.TRUE),
            Map.entry("FALSE", SoqlToken.Type.FALSE)
    );

    private final String input;
    private int pos;

    public SoqlLexer(String input) {
        this.input = input;
        this.pos = 0;
    }

    public List<SoqlToken> tokenize() {
        List<SoqlToken> tokens = new ArrayList<>();
        while (pos < input.length()) {
            skipWhitespace();
            if (pos >= input.length()) {
                break;
            }
            char ch = input.charAt(pos);

            if (ch == '\'') {
                tokens.add(readStringLiteral());
            } else if (Character.isDigit(ch) || (ch == '-' && pos + 1 < input.length() && Character.isDigit(input.charAt(pos + 1)))) {
                tokens.add(readNumberLiteral());
            } else if (Character.isLetter(ch) || ch == '_') {
                tokens.add(readIdentifierOrKeyword());
            } else if (ch == '(') {
                tokens.add(new SoqlToken(SoqlToken.Type.LPAREN, "(", pos++));
            } else if (ch == ')') {
                tokens.add(new SoqlToken(SoqlToken.Type.RPAREN, ")", pos++));
            } else if (ch == ',') {
                tokens.add(new SoqlToken(SoqlToken.Type.COMMA, ",", pos++));
            } else if (ch == '.') {
                tokens.add(new SoqlToken(SoqlToken.Type.DOT, ".", pos++));
            } else if (ch == '=') {
                tokens.add(new SoqlToken(SoqlToken.Type.EQ, "=", pos++));
            } else if (ch == '!' && pos + 1 < input.length() && input.charAt(pos + 1) == '=') {
                tokens.add(new SoqlToken(SoqlToken.Type.NEQ, "!=", pos));
                pos += 2;
            } else if (ch == '<' && pos + 1 < input.length() && input.charAt(pos + 1) == '>') {
                tokens.add(new SoqlToken(SoqlToken.Type.NEQ, "<>", pos));
                pos += 2;
            } else if (ch == '<' && pos + 1 < input.length() && input.charAt(pos + 1) == '=') {
                tokens.add(new SoqlToken(SoqlToken.Type.LTE, "<=", pos));
                pos += 2;
            } else if (ch == '>' && pos + 1 < input.length() && input.charAt(pos + 1) == '=') {
                tokens.add(new SoqlToken(SoqlToken.Type.GTE, ">=", pos));
                pos += 2;
            } else if (ch == '<') {
                tokens.add(new SoqlToken(SoqlToken.Type.LT, "<", pos++));
            } else if (ch == '>') {
                tokens.add(new SoqlToken(SoqlToken.Type.GT, ">", pos++));
            } else {
                throw new IllegalArgumentException(
                        "Unexpected character '" + ch + "' at position " + pos);
            }
        }
        tokens.add(new SoqlToken(SoqlToken.Type.EOF, "", pos));
        return tokens;
    }

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }

    private SoqlToken readStringLiteral() {
        int start = pos;
        pos++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (pos < input.length()) {
            char ch = input.charAt(pos);
            if (ch == '\\' && pos + 1 < input.length() && input.charAt(pos + 1) == '\'') {
                sb.append('\'');
                pos += 2;
            } else if (ch == '\'') {
                pos++; // skip closing quote
                return new SoqlToken(SoqlToken.Type.STRING_LITERAL, sb.toString(), start);
            } else {
                sb.append(ch);
                pos++;
            }
        }
        throw new IllegalArgumentException("Unterminated string literal starting at position " + start);
    }

    private SoqlToken readNumberLiteral() {
        int start = pos;
        if (input.charAt(pos) == '-') {
            pos++;
        }
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
            pos++;
        }
        if (pos < input.length() && input.charAt(pos) == '.') {
            pos++;
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                pos++;
            }
        }
        return new SoqlToken(SoqlToken.Type.NUMBER_LITERAL, input.substring(start, pos), start);
    }

    private SoqlToken readIdentifierOrKeyword() {
        int start = pos;
        while (pos < input.length() && isIdentifierChar(input.charAt(pos))) {
            pos++;
        }
        String word = input.substring(start, pos);
        String upper = word.toUpperCase(Locale.ROOT);
        SoqlToken.Type keywordType = KEYWORDS.get(upper);
        if (keywordType != null) {
            return new SoqlToken(keywordType, word, start);
        }
        return new SoqlToken(SoqlToken.Type.IDENTIFIER, word, start);
    }

    private boolean isIdentifierChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_';
    }
}
