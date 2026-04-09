package co.razkevich.sflocalstack.bulk.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bulk_row_results")
public class BulkRowResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String jobId;

    private String resultType;

    private String sfId;

    private Boolean sfCreated;

    private String sfError;

    @Column(columnDefinition = "TEXT")
    private String originalRow;

    public BulkRowResultEntity() {}

    public BulkRowResultEntity(String jobId, String resultType, String sfId, Boolean sfCreated, String sfError, String originalRow) {
        this.jobId = jobId;
        this.resultType = resultType;
        this.sfId = sfId;
        this.sfCreated = sfCreated;
        this.sfError = sfError;
        this.originalRow = originalRow;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getResultType() { return resultType; }
    public void setResultType(String resultType) { this.resultType = resultType; }

    public String getSfId() { return sfId; }
    public void setSfId(String sfId) { this.sfId = sfId; }

    public Boolean getSfCreated() { return sfCreated; }
    public void setSfCreated(Boolean sfCreated) { this.sfCreated = sfCreated; }

    public String getSfError() { return sfError; }
    public void setSfError(String sfError) { this.sfError = sfError; }

    public String getOriginalRow() { return originalRow; }
    public void setOriginalRow(String originalRow) { this.originalRow = originalRow; }
}
