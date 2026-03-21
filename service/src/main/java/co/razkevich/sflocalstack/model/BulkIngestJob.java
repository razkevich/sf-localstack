package co.razkevich.sflocalstack.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class BulkIngestJob {
    private final String id;
    private final String operation;
    private final String object;
    private final String externalIdFieldName;
    private final Instant createdDate;
    private final Instant systemModstamp;
    private String state;
    private final List<String> uploadedCsvBatches = new ArrayList<>();
    private final List<BulkRowResult> successfulResults = new ArrayList<>();
    private final List<BulkRowResult> failedResults = new ArrayList<>();
    private final List<BulkRowResult> unprocessedResults = new ArrayList<>();

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
    public List<String> uploadedCsvBatches() { return uploadedCsvBatches; }
    public List<BulkRowResult> successfulResults() { return successfulResults; }
    public List<BulkRowResult> failedResults() { return failedResults; }
    public List<BulkRowResult> unprocessedResults() { return unprocessedResults; }
    public int numberRecordsProcessed() { return successfulResults.size(); }
    public int numberRecordsFailed() { return failedResults.size(); }
}
