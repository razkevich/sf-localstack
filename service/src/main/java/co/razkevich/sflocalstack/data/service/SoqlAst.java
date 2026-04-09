package co.razkevich.sflocalstack.data.service;

import java.util.List;

public final class SoqlAst {

    private SoqlAst() {
    }

    public record SelectStatement(
            List<FieldRef> fields,
            String fromObject,
            Expression where,
            List<OrderByField> orderBy,
            Integer limit,
            Integer offset,
            boolean isCount
    ) {
    }

    public record FieldRef(String path) {
    }

    public sealed interface Expression permits ComparisonExpr, LogicalExpr, InExpr, LikeExpr, NullCheckExpr {
    }

    public record ComparisonExpr(FieldRef field, String operator, Object value) implements Expression {
    }

    public record LogicalExpr(Expression left, String operator, Expression right) implements Expression {
    }

    public record InExpr(FieldRef field, List<Object> values, boolean negated) implements Expression {
    }

    public record LikeExpr(FieldRef field, String pattern) implements Expression {
    }

    public record NullCheckExpr(FieldRef field, boolean isNotNull) implements Expression {
    }

    public record OrderByField(FieldRef field, boolean ascending, Boolean nullsFirst) {
    }
}
