package co.razkevich.sflocalstack.model;

import java.util.List;

public record MetadataRetrieveJob(
        String id,
        boolean done,
        boolean success,
        String status,
        String zipFileBase64,
        int numberComponentsTotal,
        List<MetadataResource> matchedResources
) {}
