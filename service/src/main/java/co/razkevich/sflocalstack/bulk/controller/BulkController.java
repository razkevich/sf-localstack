package co.razkevich.sflocalstack.bulk.controller;

import co.razkevich.sflocalstack.bulk.model.BulkIngestJob;
import co.razkevich.sflocalstack.bulk.service.BulkJobService;
import co.razkevich.sflocalstack.model.SalesforceError;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/services/data/{apiVersion}/jobs/ingest")
public class BulkController {

    private final BulkJobService bulkJobService;

    public BulkController(BulkJobService bulkJobService) {
        this.bulkJobService = bulkJobService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        BulkIngestJob job = bulkJobService.createJob(
                String.valueOf(body.get("operation")),
                String.valueOf(body.get("object")),
                body.get("externalIdFieldName") == null ? null : String.valueOf(body.get("externalIdFieldName"))
        );
        return ResponseEntity.ok(toResponse(job, ResponseShape.CREATE));
    }

    @PutMapping(value = "/{jobId}/batches", consumes = "text/csv")
    public ResponseEntity<Void> upload(@PathVariable String jobId, @RequestBody String csv) {
        bulkJobService.upload(jobId, csv);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{jobId}")
    public ResponseEntity<Map<String, Object>> close(@PathVariable String jobId, @RequestBody Map<String, Object> body) {
        if (!"UploadComplete".equals(body.get("state"))) {
            return ResponseEntity.badRequest().build();
        }
        BulkIngestJob uploadCompleteView = bulkJobService.close(jobId);
        return ResponseEntity.ok(toResponse(uploadCompleteView, ResponseShape.CLOSE));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String jobId) {
        return ResponseEntity.ok(toResponse(bulkJobService.getJob(jobId), ResponseShape.GET));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> delete(@PathVariable String jobId) {
        return bulkJobService.delete(jobId) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/{jobId}/successfulResults", produces = "text/csv")
    public ResponseEntity<String> successfulResults(@PathVariable String jobId) {
        return csv(bulkJobService.successfulResults(jobId));
    }

    @GetMapping(value = "/{jobId}/failedResults", produces = "text/csv")
    public ResponseEntity<String> failedResults(@PathVariable String jobId) {
        return csv(bulkJobService.failedResults(jobId));
    }

    @GetMapping(value = "/{jobId}/unprocessedrecords", produces = "text/csv")
    public ResponseEntity<String> unprocessed(@PathVariable String jobId) {
        return csv(bulkJobService.unprocessedResults(jobId));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<List<SalesforceError>> notFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(List.of(new SalesforceError(ex.getMessage(), "NOT_FOUND")));
    }

    private ResponseEntity<String> csv(String content) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv")
                .body(content);
    }

    private Map<String, Object> toResponse(BulkIngestJob job, ResponseShape shape) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", job.id());
        response.put("operation", job.operation());
        response.put("object", job.object());
        response.put("state", shape == ResponseShape.CLOSE ? "UploadComplete" : job.state());
        if (job.externalIdFieldName() != null) {
            response.put("externalIdFieldName", job.externalIdFieldName());
        }
        response.put("contentType", "CSV");
        response.put("concurrencyMode", "Parallel");
        response.put("createdById", "005000000000001");
        response.put("createdDate", job.createdDate().toString());
        response.put("systemModstamp", job.systemModstamp().toString());
        if (shape == ResponseShape.CREATE || shape == ResponseShape.GET) {
            response.put("columnDelimiter", "COMMA");
        }
        if (shape == ResponseShape.CREATE) {
            response.put("contentUrl", "/services/data/v60.0/jobs/ingest/" + job.id() + "/batches");
            response.put("lineEnding", "LF");
        }
        if (shape == ResponseShape.GET) {
            response.put("lineEnding", "LF");
            response.put("jobType", "V2Ingest");
            response.put("retries", 0);
            response.put("totalProcessingTime", 0);
            response.put("apiActiveProcessingTime", 0);
            response.put("apexProcessingTime", 0);
            response.put("isPkChunkingSupported", false);
        }
        if (shape == ResponseShape.GET && "JobComplete".equals(job.state())) {
            response.put("numberRecordsProcessed", job.numberRecordsProcessed());
            response.put("numberRecordsFailed", job.numberRecordsFailed());
        }
        response.put("apiVersion", 1);
        return response;
    }

    private enum ResponseShape {
        CREATE,
        CLOSE,
        GET
    }
}
