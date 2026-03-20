package co.prodly.sflocalstack.model;

public record MetadataRetrieveJob(
        String id,
        boolean done,
        boolean success,
        String status,
        String zipFileBase64,
        int numberComponentsTotal
) {}
