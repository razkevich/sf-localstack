package co.razkevich.sflocalstack.bulk.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "bulk_batches")
public class BulkBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String jobId;

    @Lob
    private String csvData;

    private int sequenceNumber;

    public BulkBatchEntity() {}

    public BulkBatchEntity(String jobId, String csvData, int sequenceNumber) {
        this.jobId = jobId;
        this.csvData = csvData;
        this.sequenceNumber = sequenceNumber;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getCsvData() { return csvData; }
    public void setCsvData(String csvData) { this.csvData = csvData; }

    public int getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }
}
