package co.prodly.sflocalstack.model;

import java.time.Instant;

public record MetadataCatalogEntry(
        String type,
        String fullName,
        String fileName,
        String directoryName,
        boolean inFolder,
        boolean metaFile,
        Instant lastModifiedDate,
        String suffix
) {
}
