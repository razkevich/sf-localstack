package co.razkevich.sflocalstack.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MetadataServiceTest {

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private MetadataManifestParser manifestParser;

    @Test
    void readMetadataSupportsCustomFieldAndValueSets() {
        var customField = metadataService.readMetadata("CustomField", java.util.List.of("Account.Type"));
        var standardValueSet = metadataService.readMetadata("StandardValueSet", java.util.List.of("AccountType"));
        var globalValueSet = metadataService.readMetadata("GlobalValueSet", java.util.List.of("CustomerPriority"));

        assertThat(customField).hasSize(1);
        assertThat(customField.getFirst().xmlType()).isEqualTo("CustomField");
        assertThat(standardValueSet.getFirst().xmlType()).isEqualTo("StandardValueSet");
        assertThat(globalValueSet.getFirst().xmlType()).isEqualTo("GlobalValueSet");
    }

    @Test
    void retrieveWithWildcardReturnsZipAndMatchedCount() {
        var typeRequests = List.of(new MetadataManifestParser.TypeRequest("CustomField", List.of("*")));
        var job = metadataService.retrieve(typeRequests);

        assertThat(job.id()).startsWith("09S");
        assertThat(job.done()).isTrue();
        assertThat(job.success()).isTrue();
        assertThat(job.status()).isEqualTo("Succeeded");
        assertThat(job.zipFileBase64()).isNotBlank();
        assertThat(job.numberComponentsTotal()).isGreaterThan(0);
    }

    @Test
    void checkRetrieveStatusReturnsSameJobForKnownId() {
        var typeRequests = List.of(new MetadataManifestParser.TypeRequest("CustomField", List.of("*")));
        var job = metadataService.retrieve(typeRequests);
        var status = metadataService.checkRetrieveStatus(job.id());

        assertThat(status.id()).isEqualTo(job.id());
        assertThat(status.done()).isTrue();
        assertThat(status.zipFileBase64()).isEqualTo(job.zipFileBase64());
    }

    @Test
    void deployStatusIsDeterministic() {
        var deploy = metadataService.deploy("dGVzdA==");
        var status = metadataService.checkDeployStatus(deploy.id());

        assertThat(status.id()).isEqualTo(deploy.id());
        assertThat(status.done()).isTrue();
        assertThat(status.success()).isTrue();
    }
}
