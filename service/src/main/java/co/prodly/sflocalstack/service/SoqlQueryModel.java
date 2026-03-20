package co.prodly.sflocalstack.service;

import java.util.List;

public record SoqlQueryModel(
        List<String> selectedFields,
        String objectType,
        List<SoqlCondition> conditions,
        Integer limit,
        boolean countQuery
) {
}
