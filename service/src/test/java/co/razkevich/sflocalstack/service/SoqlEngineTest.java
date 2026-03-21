package co.razkevich.sflocalstack.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class SoqlEngineTest {

    @Autowired
    private SoqlEngine soqlEngine;

    @Autowired
    private OrgStateService orgStateService;

    @Autowired
    private SeedDataLoader seedDataLoader;

    @BeforeEach
    void reset() {
        orgStateService.reset();
        seedDataLoader.load();
    }

    @Test
    void equalityFilterMatchesSeededAccount() {
        List<Map<String, Object>> results = soqlEngine.execute("SELECT Id, Name FROM Account WHERE Name = 'Acme Corp'");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst()).containsEntry("Name", "Acme Corp");
    }

    @Test
    void likeFilterMatchesBothSeededAccounts() {
        List<Map<String, Object>> results = soqlEngine.execute("SELECT Id, Name FROM Account WHERE Name LIKE '%Corp%'");

        assertThat(results).hasSize(2);
    }

    @Test
    void relationshipProjectionUsesNestedObjectShape() {
        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, FirstName, Account.Name FROM Contact WHERE LastName = 'Doe'");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst()).containsEntry("FirstName", "John");
        assertThat(results.getFirst()).containsKey("Account");
        assertThat(results.getFirst().get("Account")).isEqualTo(Map.of("Name", "Acme Corp"));
    }

    @Test
    void countAggregateReturnsExpr0() {
        List<Map<String, Object>> results = soqlEngine.execute("SELECT COUNT() FROM Account");

        assertThat(results).containsExactly(Map.of("expr0", 2));
    }

    @Test
    void limitIsRespectedAfterFiltering() {
        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id FROM Account WHERE Name LIKE '%Corp%' LIMIT 1");

        assertThat(results).hasSize(1);
    }

    @Test
    void unsupportedQueryThrows() {
        assertThatThrownBy(() -> soqlEngine.execute("NOT VALID SOQL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported SOQL");
    }
}
