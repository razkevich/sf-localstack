package co.prodly.sflocalstack.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MetadataServiceTest {

    @Autowired
    private MetadataService metadataService;

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
    void deployStatusIsDeterministic() {
        var deploy = metadataService.deploy("dGVzdA==");
        var status = metadataService.checkDeployStatus(deploy.id());

        assertThat(status.id()).isEqualTo(deploy.id());
        assertThat(status.done()).isTrue();
        assertThat(status.success()).isTrue();
    }
}
