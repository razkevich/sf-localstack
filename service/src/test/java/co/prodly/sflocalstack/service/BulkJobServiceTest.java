package co.prodly.sflocalstack.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BulkJobServiceTest {

    @Autowired
    private BulkJobService bulkJobService;

    @Autowired
    private OrgStateService orgStateService;

    @Autowired
    private SeedDataLoader seedDataLoader;

    @BeforeEach
    void reset() {
        orgStateService.reset();
        seedDataLoader.load();
        bulkJobService.reset();
    }

    @Test
    void insertCsvProducesSuccessfulRowResult() {
        var job = bulkJobService.createJob("insert", "Account", null);
        bulkJobService.upload(job.id(), "Name,Industry\nBulk Test,Technology\n");
        var completed = bulkJobService.close(job.id());

        assertThat(completed.numberRecordsProcessed()).isEqualTo(1);
        assertThat(completed.numberRecordsFailed()).isEqualTo(0);
        assertThat(bulkJobService.successfulResults(job.id())).contains("sf__Id,sf__Created");
    }

    @Test
    void invalidUpdateRowProducesFailureResult() {
        var job = bulkJobService.createJob("update", "Account", null);
        bulkJobService.upload(job.id(), "Name\nMissing Id\n");
        var completed = bulkJobService.close(job.id());

        assertThat(completed.numberRecordsProcessed()).isZero();
        assertThat(completed.numberRecordsFailed()).isEqualTo(1);
        assertThat(bulkJobService.failedResults(job.id())).contains("Missing required Id");
    }
}
