package co.razkevich.sflocalstack.metadata.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "metadata_deploy_jobs")
public class MetadataDeployJobEntity {

    @Id
    private String id;

    private boolean done;

    private boolean success;

    private String status;

    private int numberComponentsTotal;

    private int numberComponentsDeployed;

    private int numberComponentErrors;

    private Instant createdDate;

    private Instant completedDate;

    public MetadataDeployJobEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getNumberComponentsTotal() { return numberComponentsTotal; }
    public void setNumberComponentsTotal(int numberComponentsTotal) { this.numberComponentsTotal = numberComponentsTotal; }

    public int getNumberComponentsDeployed() { return numberComponentsDeployed; }
    public void setNumberComponentsDeployed(int numberComponentsDeployed) { this.numberComponentsDeployed = numberComponentsDeployed; }

    public int getNumberComponentErrors() { return numberComponentErrors; }
    public void setNumberComponentErrors(int numberComponentErrors) { this.numberComponentErrors = numberComponentErrors; }

    public Instant getCreatedDate() { return createdDate; }
    public void setCreatedDate(Instant createdDate) { this.createdDate = createdDate; }

    public Instant getCompletedDate() { return completedDate; }
    public void setCompletedDate(Instant completedDate) { this.completedDate = completedDate; }
}
