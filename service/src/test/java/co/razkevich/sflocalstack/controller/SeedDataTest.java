package co.razkevich.sflocalstack.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SeedDataTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void reset() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());
    }

    @Test
    void pdriConnectionIsSeedied() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Id, PDRI__Instance_URL__c, PDRI__OrganizationId__c FROM PDRI__Connection__c"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(1))
                .andExpect(jsonPath("$.records[0].Id").value("a0D5e00000Q9h43EAB"))
                .andExpect(jsonPath("$.records[0].PDRI__OrganizationId__c").value("some-id"));
    }

    @Test
    void pdriComparisonViewIsSeedied() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Id, Name FROM PDRI__ComparisonView__c"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(1))
                .andExpect(jsonPath("$.records[0].Id").value("a1QDn000002BPrXMAW"));
    }

    @Test
    void pdriComparisonViewRulesAreSeedied() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Id, PDRI__FilterValue__c FROM PDRI__ComparisonViewRule__c"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(5));
    }

    @Test
    void staticResourcesAreSeedied() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Name, BodyLength FROM StaticResource"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(3));
    }

    @Test
    void organizationIsSandbox() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT IsSandbox, OrganizationType, TrialExpirationDate FROM Organization"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(1))
                .andExpect(jsonPath("$.records[0].IsSandbox").value(true))
                .andExpect(jsonPath("$.records[0].OrganizationType").value("abcd"))
                .andExpect(jsonPath("$.records[0].TrialExpirationDate").value("some-date"));
    }
}
