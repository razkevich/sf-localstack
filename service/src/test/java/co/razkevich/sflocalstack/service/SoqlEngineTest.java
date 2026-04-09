package co.razkevich.sflocalstack.service;

import co.razkevich.sflocalstack.data.service.OrgStateService;
import co.razkevich.sflocalstack.data.service.SoqlEngine;
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

    @BeforeEach
    void reset() {
        orgStateService.reset();
        var acme = orgStateService.create("Account", new java.util.HashMap<>(Map.of("Name", "Acme Corp", "Industry", "Technology")));
        orgStateService.create("Account", new java.util.HashMap<>(Map.of("Name", "Globex Corp", "Industry", "Manufacturing")));
        orgStateService.create("Account", new java.util.HashMap<>(Map.of("Name", "Initech", "Industry", "Technology")));
        orgStateService.create("Contact", new java.util.HashMap<>(Map.of(
                "FirstName", "John", "LastName", "Doe", "Email", "john.doe@acme.com", "AccountId", acme.getId())));
    }

    // --- Existing tests (backward compatibility) ---

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

        assertThat(results).containsExactly(Map.of("expr0", 3));
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

    // --- New tests for IN clause ---

    @Test
    void whereWithInClauseMatchesMultipleValues() {
        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, Name FROM Account WHERE Industry IN ('Technology', 'Manufacturing')");

        assertThat(results).hasSize(3);
    }

    @Test
    void whereWithInClauseFiltersSingleValue() {
        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, Name FROM Account WHERE Industry IN ('Technology')");

        assertThat(results).hasSize(2);
    }

    @Test
    void whereWithNotInClause() {
        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, Name FROM Account WHERE Industry NOT IN ('Manufacturing')");

        assertThat(results).hasSize(2);
    }

    // --- New tests for LIKE ---

    @Test
    void whereWithLikePrefix() {
        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, Name FROM Account WHERE Name LIKE 'Acme%'");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst()).containsEntry("Name", "Acme Corp");
    }

    @Test
    void whereWithLikeSuffix() {
        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, Name FROM Account WHERE Name LIKE '%Corp'");

        assertThat(results).hasSize(2);
    }

    // --- New tests for IS NULL / IS NOT NULL ---

    @Test
    void whereWithIsNull() {
        // Initech has no explicit Industry set as null — it IS set though, so this tests a field not present
        orgStateService.create("Account", new java.util.HashMap<>(Map.of("Name", "NoIndustry")));

        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, Name FROM Account WHERE Industry IS NULL");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst()).containsEntry("Name", "NoIndustry");
    }

    @Test
    void whereWithIsNotNull() {
        orgStateService.create("Account", new java.util.HashMap<>(Map.of("Name", "NoIndustry")));

        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, Name FROM Account WHERE Industry IS NOT NULL");

        assertThat(results).hasSize(3);
    }

    // --- New tests for AND/OR ---

    @Test
    void whereWithAndCondition() {
        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, Name FROM Account WHERE Name = 'Acme Corp' AND Industry = 'Technology'");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst()).containsEntry("Name", "Acme Corp");
    }

    @Test
    void whereWithOrCondition() {
        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, Name FROM Account WHERE Name = 'Acme Corp' OR Name = 'Globex Corp'");

        assertThat(results).hasSize(2);
    }

    @Test
    void whereWithAndOrMixed() {
        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, Name FROM Account WHERE Industry = 'Technology' AND (Name = 'Acme Corp' OR Name = 'Initech')");

        assertThat(results).hasSize(2);
    }

    // --- New tests for parentheses ---

    @Test
    void whereWithParenthesesOverridesPrecedence() {
        // Without parens: Name = 'Acme Corp' OR (Name = 'Globex Corp' AND Industry = 'Technology')
        // → Acme Corp matches first OR; Globex doesn't match AND
        // With parens: (Name = 'Acme Corp' OR Name = 'Globex Corp') AND Industry = 'Technology'
        // → Both match OR, but only Acme matches AND with Technology
        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, Name FROM Account WHERE (Name = 'Acme Corp' OR Name = 'Globex Corp') AND Industry = 'Technology'");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst()).containsEntry("Name", "Acme Corp");
    }

    // --- New tests for ORDER BY ---

    @Test
    void orderByAscending() {
        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, Name FROM Account ORDER BY Name ASC");

        assertThat(results).hasSize(3);
        assertThat(results.get(0)).containsEntry("Name", "Acme Corp");
        assertThat(results.get(1)).containsEntry("Name", "Globex Corp");
        assertThat(results.get(2)).containsEntry("Name", "Initech");
    }

    @Test
    void orderByDescending() {
        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, Name FROM Account ORDER BY Name DESC");

        assertThat(results).hasSize(3);
        assertThat(results.get(0)).containsEntry("Name", "Initech");
        assertThat(results.get(1)).containsEntry("Name", "Globex Corp");
        assertThat(results.get(2)).containsEntry("Name", "Acme Corp");
    }

    @Test
    void orderByDefaultAscending() {
        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, Name FROM Account ORDER BY Name");

        assertThat(results).hasSize(3);
        assertThat(results.get(0)).containsEntry("Name", "Acme Corp");
        assertThat(results.get(2)).containsEntry("Name", "Initech");
    }

    // --- New tests for OFFSET ---

    @Test
    void offsetSkipsRows() {
        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, Name FROM Account ORDER BY Name ASC LIMIT 1 OFFSET 1");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst()).containsEntry("Name", "Globex Corp");
    }

    @Test
    void offsetWithoutLimit() {
        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, Name FROM Account ORDER BY Name ASC OFFSET 2");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst()).containsEntry("Name", "Initech");
    }

    // --- Error handling ---

    @Test
    void parseErrorContainsPositionInfo() {
        assertThatThrownBy(() -> soqlEngine.execute("SELECT FROM Account"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("position");
    }

    @Test
    void unterminatedStringThrows() {
        assertThatThrownBy(() -> soqlEngine.execute("SELECT Id FROM Account WHERE Name = 'unterminated"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Comparison operators ---

    @Test
    void notEqualsOperatorWorks() {
        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, Name FROM Account WHERE Name != 'Acme Corp'");

        assertThat(results).hasSize(2);
    }

    @Test
    void nullComparisonWithEquals() {
        orgStateService.create("Account", new java.util.HashMap<>(Map.of("Name", "NoIndustry")));

        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, Name FROM Account WHERE Industry = null");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst()).containsEntry("Name", "NoIndustry");
    }

    @Test
    void nullComparisonWithNotEquals() {
        orgStateService.create("Account", new java.util.HashMap<>(Map.of("Name", "NoIndustry")));

        List<Map<String, Object>> results = soqlEngine.execute(
                "SELECT Id, Name FROM Account WHERE Industry != null");

        assertThat(results).hasSize(3);
    }
}
