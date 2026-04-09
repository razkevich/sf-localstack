package co.razkevich.sflocalstack.bulk.service;

import co.razkevich.sflocalstack.bulk.model.BulkBatchEntity;
import co.razkevich.sflocalstack.bulk.model.BulkIngestJob;
import co.razkevich.sflocalstack.bulk.model.BulkRowResult;
import co.razkevich.sflocalstack.bulk.model.BulkRowResultEntity;
import co.razkevich.sflocalstack.bulk.repository.BulkBatchRepository;
import co.razkevich.sflocalstack.bulk.repository.BulkIngestJobRepository;
import co.razkevich.sflocalstack.bulk.repository.BulkRowResultRepository;
import co.razkevich.sflocalstack.data.service.OrgStateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class BulkJobService {

    private final BulkIngestJobRepository jobRepository;
    private final BulkBatchRepository batchRepository;
    private final BulkRowResultRepository rowResultRepository;
    private final OrgStateService orgStateService;

    public BulkJobService(BulkIngestJobRepository jobRepository,
                          BulkBatchRepository batchRepository,
                          BulkRowResultRepository rowResultRepository,
                          OrgStateService orgStateService) {
        this.jobRepository = jobRepository;
        this.batchRepository = batchRepository;
        this.rowResultRepository = rowResultRepository;
        this.orgStateService = orgStateService;
    }

    @Transactional
    public BulkIngestJob createJob(String operation, String object, String externalIdFieldName) {
        String id = "750" + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
        BulkIngestJob job = new BulkIngestJob(id, operation, object, externalIdFieldName, Instant.now(), "Open");
        return jobRepository.save(job);
    }

    public BulkIngestJob getJob(String jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new NoSuchElementException("Bulk job not found: " + jobId));
    }

    @Transactional
    public void upload(String jobId, String csv) {
        getJob(jobId); // verify job exists
        int nextSeq = batchRepository.findByJobIdOrderBySequenceNumber(jobId).size();
        batchRepository.save(new BulkBatchEntity(jobId, csv, nextSeq));
    }

    @Transactional
    public BulkIngestJob close(String jobId) {
        BulkIngestJob job = getJob(jobId);

        // Clear any previous results for this job
        rowResultRepository.deleteByJobId(jobId);

        List<BulkBatchEntity> batches = batchRepository.findByJobIdOrderBySequenceNumber(jobId);
        int successCount = 0;
        int failCount = 0;

        for (BulkBatchEntity batch : batches) {
            int[] counts = processCsv(job, batch.getCsvData());
            successCount += counts[0];
            failCount += counts[1];
        }

        job.setState("JobComplete");
        job.setNumberRecordsProcessed(successCount);
        job.setNumberRecordsFailed(failCount);
        return jobRepository.save(job);
    }

    @Transactional
    public boolean delete(String jobId) {
        if (!jobRepository.existsById(jobId)) {
            return false;
        }
        rowResultRepository.deleteByJobId(jobId);
        batchRepository.deleteByJobId(jobId);
        jobRepository.deleteById(jobId);
        return true;
    }

    @Transactional
    public void reset() {
        rowResultRepository.deleteAll();
        batchRepository.deleteAll();
        jobRepository.deleteAll();
    }

    public String successfulResults(String jobId) {
        getJob(jobId); // verify job exists
        List<BulkRowResultEntity> rows = rowResultRepository.findByJobIdAndResultType(jobId, "successfulResults");
        StringBuilder csv = new StringBuilder("sf__Id,sf__Created\n");
        for (BulkRowResultEntity row : rows) {
            csv.append(row.getSfId()).append(',').append(row.getSfCreated()).append('\n');
        }
        return csv.toString();
    }

    public String failedResults(String jobId) {
        getJob(jobId); // verify job exists
        List<BulkRowResultEntity> rows = rowResultRepository.findByJobIdAndResultType(jobId, "failedResults");
        StringBuilder csv = new StringBuilder("sf__Id,sf__Error\n");
        for (BulkRowResultEntity row : rows) {
            csv.append(row.getSfId() == null ? "" : row.getSfId()).append(',').append(row.getSfError()).append('\n');
        }
        return csv.toString();
    }

    public String unprocessedResults(String jobId) {
        getJob(jobId); // verify job exists
        List<BulkRowResultEntity> rows = rowResultRepository.findByJobIdAndResultType(jobId, "unprocessedRecords");
        StringBuilder csv = new StringBuilder("sf__Id,sf__Error\n");
        for (BulkRowResultEntity row : rows) {
            csv.append(row.getSfId() == null ? "" : row.getSfId()).append(',').append(row.getSfError()).append('\n');
        }
        return csv.toString();
    }

    private int[] processCsv(BulkIngestJob job, String csv) {
        String[] lines = csv.strip().split("\\r?\\n");
        int successCount = 0;
        int failCount = 0;
        if (lines.length < 2) {
            return new int[]{0, 0};
        }

        String[] headers = lines[0].split(",");
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            Map<String, Object> row = parseRow(headers, lines[i]);
            boolean success = handleRow(job, row, lines[i]);
            if (success) {
                successCount++;
            } else {
                failCount++;
            }
        }
        return new int[]{successCount, failCount};
    }

    private Map<String, Object> parseRow(String[] headers, String line) {
        String[] values = line.split(",", -1);
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            row.put(headers[i], i < values.length ? values[i] : "");
        }
        return row;
    }

    private boolean handleRow(BulkIngestJob job, Map<String, Object> row, String originalRow) {
        try {
            switch (job.operation()) {
                case "insert" -> {
                    var record = orgStateService.create(job.object(), row);
                    rowResultRepository.save(new BulkRowResultEntity(
                            job.id(), "successfulResults", record.getId(), true, null, originalRow));
                }
                case "update" -> {
                    Object id = row.get("Id");
                    if (!(id instanceof String idValue) || idValue.isBlank()) {
                        throw new IllegalArgumentException("Missing required Id");
                    }
                    orgStateService.update(idValue, row).orElseThrow(() -> new IllegalArgumentException("Record not found"));
                    rowResultRepository.save(new BulkRowResultEntity(
                            job.id(), "successfulResults", idValue, false, null, originalRow));
                }
                case "delete" -> {
                    Object id = row.get("Id");
                    if (!(id instanceof String idValue) || idValue.isBlank()) {
                        throw new IllegalArgumentException("Missing required Id");
                    }
                    if (!orgStateService.delete(idValue)) {
                        throw new IllegalArgumentException("Record not found");
                    }
                    rowResultRepository.save(new BulkRowResultEntity(
                            job.id(), "successfulResults", idValue, false, null, originalRow));
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
                    rowResultRepository.save(new BulkRowResultEntity(
                            job.id(), "successfulResults", result.record().getId(), result.created(), null, originalRow));
                }
                default -> throw new IllegalArgumentException("Unsupported bulk operation");
            }
            return true;
        } catch (IllegalArgumentException ex) {
            rowResultRepository.save(new BulkRowResultEntity(
                    job.id(), "failedResults", null, false, ex.getMessage(), originalRow));
            return false;
        }
    }
}
