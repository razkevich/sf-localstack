package co.razkevich.sflocalstack.model;

public record MetadataDeployJob(
        String id,
        boolean done,
        boolean success,
        String status,
        int numberComponentsTotal,
        int numberComponentsDeployed,
        int numberComponentErrors
) {
}
