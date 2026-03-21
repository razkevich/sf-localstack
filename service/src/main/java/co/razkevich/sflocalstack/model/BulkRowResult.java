package co.razkevich.sflocalstack.model;

public record BulkRowResult(
        String id,
        boolean created,
        String error,
        String originalRow
) {
}
