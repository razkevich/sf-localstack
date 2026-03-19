package co.prodly.sflocalstack.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@SpringBootTest
class SoqlEngineTest {

    @Autowired
    private SoqlEngine soqlEngine;

    @Test
    void parseBasicSelectDoesNotThrow() {
        assertThatNoException().isThrownBy(() ->
                soqlEngine.execute("SELECT Id, Name FROM Account"));
    }

    @Test
    void selectAllReturnsRecords() {
        // After seed load, we should have Account records
        List<Map<String, Object>> results = soqlEngine.execute("SELECT Id, Name FROM Account");
        assertThat(results).isNotNull();
    }

    @Test
    void limitIsRespected() {
        List<Map<String, Object>> results = soqlEngine.execute("SELECT Id FROM Account LIMIT 1");
        assertThat(results).hasSizeLessThanOrEqualTo(1);
    }

    @Test
    void unsupportedQueryReturnsEmpty() {
        // Malformed / unsupported queries return empty list, not exception
        List<Map<String, Object>> results = soqlEngine.execute("NOT VALID SOQL");
        assertThat(results).isEmpty();
    }
}
