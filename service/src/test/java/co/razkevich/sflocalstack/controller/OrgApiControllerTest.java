package co.razkevich.sflocalstack.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrgApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void reset() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());
    }

    @Test
    void describeGlobalListsBaselineObjectsWithUrls() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/sobjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encoding").value("UTF-8"))
                .andExpect(jsonPath("$.maxBatchSize").value(200))
                .andExpect(jsonPath("$.sobjects[?(@.name == 'Account')].keyPrefix").value("001"))
                .andExpect(jsonPath("$.sobjects[?(@.name == 'Account')].queryable").value(true))
                .andExpect(jsonPath("$.sobjects[?(@.name == 'Account')].urls.describe")
                        .value("/services/data/v60.0/sobjects/Account/describe"))
                .andExpect(jsonPath("$.sobjects[?(@.name == 'Contact')].keyPrefix").value("003"));
    }

    @Test
    void describeGlobalIncludesCustomObjectsPresentInTheOrg() throws Exception {
        mockMvc.perform(post("/services/data/v60.0/sobjects/Invoice__c")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"INV-001\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/services/data/v60.0/sobjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sobjects[?(@.name == 'Invoice__c')].custom").value(true));
    }

    @Test
    void describeGlobalHonorsRequestedApiVersionInUrls() throws Exception {
        mockMvc.perform(get("/services/data/v58.0/sobjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sobjects[?(@.name == 'Account')].urls.sobject")
                        .value("/services/data/v58.0/sobjects/Account"));
    }

    @Test
    void limitsReportsMaxAndRemaining() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.DailyApiRequests.Max").value(100000))
                .andExpect(jsonPath("$.DailyApiRequests.Remaining").exists())
                .andExpect(jsonPath("$.DataStorageMB.Max").value(1024))
                .andExpect(jsonPath("$.FileStorageMB.Remaining").value(1024));
    }

    @Test
    void limitsRemainingNeverGoesNegative() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"Name\":\"Acme " + i + "\"}"))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(get("/services/data/v60.0/limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.DataStorageMB.Remaining").value(1023));
    }
}
