package co.prodly.sflocalstack.model;

import java.time.Instant;
import java.util.Map;

public record MetadataResource(
        String type,
        String fullName,
        String fileName,
        String directoryName,
        boolean inFolder,
        boolean metaFile,
        Instant lastModifiedDate,
        String label,
        Map<String, Object> attributes
) {
    public MetadataCatalogEntry toCatalogEntry() {
        return new MetadataCatalogEntry(type, fullName, fileName, directoryName, inFolder, metaFile, lastModifiedDate);
    }
}
