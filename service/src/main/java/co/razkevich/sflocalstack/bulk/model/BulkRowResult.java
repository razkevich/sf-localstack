package co.razkevich.sflocalstack.bulk.model;

public record BulkRowResult(
        String id,
        boolean created,
        String error,
        String originalRow
) {
}
