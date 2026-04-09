package co.razkevich.sflocalstack.metadata.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "metadata_retrieve_jobs")
public class MetadataRetrieveJobEntity {

    @Id
    private String id;

    private boolean done;

    private boolean success;

    private String status;

    @Lob
    private String zipFileBase64;

    private int numberComponentsTotal;

    private Instant createdDate;

    private Instant completedDate;

    public MetadataRetrieveJobEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getZipFileBase64() { return zipFileBase64; }
    public void setZipFileBase64(String zipFileBase64) { this.zipFileBase64 = zipFileBase64; }

    public int getNumberComponentsTotal() { return numberComponentsTotal; }
    public void setNumberComponentsTotal(int numberComponentsTotal) { this.numberComponentsTotal = numberComponentsTotal; }

    public Instant getCreatedDate() { return createdDate; }
    public void setCreatedDate(Instant createdDate) { this.createdDate = createdDate; }

    public Instant getCompletedDate() { return completedDate; }
    public void setCompletedDate(Instant completedDate) { this.completedDate = completedDate; }
}
