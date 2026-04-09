package co.razkevich.sflocalstack.service;

import co.razkevich.sflocalstack.data.model.SObjectRecord;
import co.razkevich.sflocalstack.data.service.OrgStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrgStateServiceTest {

    @Autowired
    private OrgStateService orgStateService;

    @BeforeEach
    void reset() {
        orgStateService.reset();
    }

    @Test
    void findByIdReturnsCreatedRecord() {
        SObjectRecord record = orgStateService.create("Account", new HashMap<>(Map.of("Name", "FindMe")));
        Optional<SObjectRecord> found = orgStateService.findById(record.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getObjectType()).isEqualTo("Account");
    }

    @Test
    void findByIdReturnsEmptyForMissingId() {
        Optional<SObjectRecord> found = orgStateService.findById("nonexistent");
        assertThat(found).isEmpty();
    }

    @Test
    void findByTypeAndIdReturnsMatchingRecord() {
        SObjectRecord record = orgStateService.create("Account", new HashMap<>(Map.of("Name", "TypeAndId")));
        Optional<SObjectRecord> found = orgStateService.findByTypeAndId("Account", record.getId());
        assertThat(found).isPresent();
    }

    @Test
    void findByTypeAndIdReturnsEmptyForWrongType() {
        SObjectRecord record = orgStateService.create("Account", new HashMap<>(Map.of("Name", "WrongType")));
        Optional<SObjectRecord> found = orgStateService.findByTypeAndId("Contact", record.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void findAllReturnsAllRecords() {
        orgStateService.create("Account", new HashMap<>(Map.of("Name", "A1")));
        orgStateService.create("Account", new HashMap<>(Map.of("Name", "A2")));
        orgStateService.create("Contact", new HashMap<>(Map.of("LastName", "C1")));
        assertThat(orgStateService.findAll()).hasSize(3);
    }

    @Test
    void deleteRemovesRecord() {
        SObjectRecord record = orgStateService.create("Account", new HashMap<>(Map.of("Name", "DeleteMe")));
        boolean deleted = orgStateService.delete(record.getId());
        assertThat(deleted).isTrue();
        assertThat(orgStateService.findById(record.getId())).isEmpty();
    }

    @Test
    void deleteReturnsFalseForMissingId() {
        boolean deleted = orgStateService.delete("nonexistent");
        assertThat(deleted).isFalse();
    }

    @Test
    void resetClearsAllRecords() {
        orgStateService.create("Account", new HashMap<>(Map.of("Name", "R1")));
        orgStateService.create("Account", new HashMap<>(Map.of("Name", "R2")));
        orgStateService.reset();
        assertThat(orgStateService.findAll()).isEmpty();
    }

    @Test
    void fromJsonParsesValidJson() {
        Map<String, Object> result = orgStateService.fromJson("{\"Name\":\"Test\",\"Industry\":\"Tech\"}");
        assertThat(result).containsEntry("Name", "Test");
        assertThat(result).containsEntry("Industry", "Tech");
    }
}
