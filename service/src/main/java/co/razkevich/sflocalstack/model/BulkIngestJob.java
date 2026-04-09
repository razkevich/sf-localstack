package co.razkevich.sflocalstack.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "bulk_ingest_jobs")
public class BulkIngestJob {

    @Id
    private String id;

    private String operation;
    private String object;
    private String externalIdFieldName;
    private Instant createdDate;
    private Instant systemModstamp;
    private String state;
    private int numberRecordsProcessed;
    private int numberRecordsFailed;

    public BulkIngestJob() {}

    public BulkIngestJob(String id, String operation, String object, String externalIdFieldName, Instant createdDate, String state) {
        this.id = id;
        this.operation = operation;
        this.object = object;
        this.externalIdFieldName = externalIdFieldName;
        this.createdDate = createdDate;
        this.systemModstamp = createdDate;
        this.state = state;
    }

    public String id() { return id; }
    public String operation() { return operation; }
    public String object() { return object; }
    public String externalIdFieldName() { return externalIdFieldName; }
    public Instant createdDate() { return createdDate; }
    public Instant systemModstamp() { return systemModstamp; }
    public String state() { return state; }
    public void setState(String state) { this.state = state; }
    public int numberRecordsProcessed() { return numberRecordsProcessed; }
    public void setNumberRecordsProcessed(int numberRecordsProcessed) { this.numberRecordsProcessed = numberRecordsProcessed; }
    public int numberRecordsFailed() { return numberRecordsFailed; }
    public void setNumberRecordsFailed(int numberRecordsFailed) { this.numberRecordsFailed = numberRecordsFailed; }
}
