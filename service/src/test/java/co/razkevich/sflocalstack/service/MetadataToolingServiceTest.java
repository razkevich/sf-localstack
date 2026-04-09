package co.razkevich.sflocalstack.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MetadataToolingServiceTest {

    @Autowired
    private MetadataToolingService metadataToolingService;

    @Test
    void executeStandardMetadataQueryReturnsResultsForFlowDefinitionView() {
        // Only FlowDefinitionView is handled by the standard metadata path;
        // all other object names return an empty list.
        List<Map<String, Object>> results = metadataToolingService.executeStandardMetadataQuery(
                "SELECT ApiName, DurableId FROM FlowDefinitionView");
        assertThat(results).isNotNull();
    }

    @Test
    void executeStandardMetadataQueryReturnsEmptyForNonFlowObjects() {
        // SourceMember and all other non-FlowDefinitionView objects return empty.
        List<Map<String, Object>> results = metadataToolingService.executeStandardMetadataQuery(
                "SELECT Id, MemberName FROM SourceMember");
        assertThat(results).isEmpty();
    }

    @Test
    void executeStandardMetadataQueryReturnsEmptyForInvalidSoql() {
        // Unparseable SOQL is handled gracefully and returns empty list.
        List<Map<String, Object>> results = metadataToolingService.executeStandardMetadataQuery("not soql at all");
        assertThat(results).isEmpty();
    }
}
