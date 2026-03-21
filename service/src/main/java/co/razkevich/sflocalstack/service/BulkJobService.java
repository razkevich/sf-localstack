package co.razkevich.sflocalstack.service;

import co.razkevich.sflocalstack.model.BulkIngestJob;
import co.razkevich.sflocalstack.model.BulkRowResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BulkJobService {

    private final Map<String, BulkIngestJob> jobs = new ConcurrentHashMap<>();
    private final OrgStateService orgStateService;

    public BulkJobService(OrgStateService orgStateService) {
        this.orgStateService = orgStateService;
    }

    public BulkIngestJob createJob(String operation, String object, String externalIdFieldName) {
        String id = "750" + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
        BulkIngestJob job = new BulkIngestJob(id, operation, object, externalIdFieldName, Instant.now(), "Open");
        jobs.put(id, job);
        return job;
    }

    public BulkIngestJob getJob(String jobId) {
        BulkIngestJob job = jobs.get(jobId);
        if (job == null) {
            throw new NoSuchElementException("Bulk job not found: " + jobId);
        }
        return job;
    }

    public void upload(String jobId, String csv) {
        getJob(jobId).uploadedCsvBatches().add(csv);
    }

    public BulkIngestJob close(String jobId) {
        BulkIngestJob job = getJob(jobId);
        job.successfulResults().clear();
        job.failedResults().clear();
        job.unprocessedResults().clear();

        for (String csv : job.uploadedCsvBatches()) {
            processCsv(job, csv);
        }

        job.setState("JobComplete");
        return job;
    }

    public boolean delete(String jobId) {
        return jobs.remove(jobId) != null;
    }

    public void reset() {
        jobs.clear();
    }

    public String successfulResults(String jobId) {
        StringBuilder csv = new StringBuilder("sf__Id,sf__Created\n");
        for (BulkRowResult row : getJob(jobId).successfulResults()) {
            csv.append(row.id()).append(',').append(row.created()).append('\n');
        }
        return csv.toString();
    }

    public String failedResults(String jobId) {
        StringBuilder csv = new StringBuilder("sf__Id,sf__Error\n");
        for (BulkRowResult row : getJob(jobId).failedResults()) {
            csv.append(row.id() == null ? "" : row.id()).append(',').append(row.error()).append('\n');
        }
        return csv.toString();
    }

    public String unprocessedResults(String jobId) {
        StringBuilder csv = new StringBuilder("sf__Id,sf__Error\n");
        for (BulkRowResult row : getJob(jobId).unprocessedResults()) {
            csv.append(row.id() == null ? "" : row.id()).append(',').append(row.error()).append('\n');
        }
        return csv.toString();
    }

    private void processCsv(BulkIngestJob job, String csv) {
        String[] lines = csv.strip().split("\\r?\\n");
        if (lines.length < 2) {
            return;
        }

        String[] headers = lines[0].split(",");
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            Map<String, Object> row = parseRow(headers, lines[i]);
            handleRow(job, row, lines[i]);
        }
    }

    private Map<String, Object> parseRow(String[] headers, String line) {
        String[] values = line.split(",", -1);
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            row.put(headers[i], i < values.length ? values[i] : "");
        }
        return row;
    }

    private void handleRow(BulkIngestJob job, Map<String, Object> row, String originalRow) {
        try {
            switch (job.operation()) {
                case "insert" -> {
                    var record = orgStateService.create(job.object(), row);
                    job.successfulResults().add(new BulkRowResult(record.getId(), true, null, originalRow));
                }
                case "update" -> {
                    Object id = row.get("Id");
                    if (!(id instanceof String idValue) || idValue.isBlank()) {
                        throw new IllegalArgumentException("Missing required Id");
                    }
                    orgStateService.update(idValue, row).orElseThrow(() -> new IllegalArgumentException("Record not found"));
                    job.successfulResults().add(new BulkRowResult(idValue, false, null, originalRow));
                }
                case "delete" -> {
                    Object id = row.get("Id");
                    if (!(id instanceof String idValue) || idValue.isBlank()) {
                        throw new IllegalArgumentException("Missing required Id");
                    }
                    if (!orgStateService.delete(idValue)) {
                        throw new IllegalArgumentException("Record not found");
                    }
                    job.successfulResults().add(new BulkRowResult(idValue, false, null, originalRow));
                }
                case "upsert" -> {
                    if (job.externalIdFieldName() == null || job.externalIdFieldName().isBlank()) {
                        throw new IllegalArgumentException("Missing externalIdFieldName");
                    }
                    Object ext = row.get(job.externalIdFieldName());
                    if (!(ext instanceof String extValue) || extValue.isBlank()) {
                        throw new IllegalArgumentException("Missing required external id value");
                    }
                    var result = orgStateService.upsert(job.object(), job.externalIdFieldName(), extValue, row);
                    job.successfulResults().add(new BulkRowResult(result.record().getId(), result.created(), null, originalRow));
                }
                default -> throw new IllegalArgumentException("Unsupported bulk operation");
            }
        } catch (IllegalArgumentException ex) {
            job.failedResults().add(new BulkRowResult(null, false, ex.getMessage(), originalRow));
        }
    }
}
