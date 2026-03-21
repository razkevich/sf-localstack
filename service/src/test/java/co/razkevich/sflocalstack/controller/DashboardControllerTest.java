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
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void reset() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());
    }

    @Test
    void overviewReturnsSeededObjectCounts() throws Exception {
        mockMvc.perform(get("/api/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("sf-localstack"))
                .andExpect(jsonPath("$.apiVersion").value("v60.0"))

                .andExpect(jsonPath("$.totalRecords").value(16))

                .andExpect(jsonPath("$.objectCounts[0].objectType").value("Account"))
                .andExpect(jsonPath("$.objectCounts[0].count").value(2));
    }

    @Test
    void requestsHonorsLimit() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/query").param("q", "SELECT Id, Name FROM Account"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard/requests").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].path").exists());
    }

    @Test
    void dashboardRequestsDoNotIncreaseCapturedRequestCount() throws Exception {
        mockMvc.perform(get("/api/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentRequestCount").value(0));

        mockMvc.perform(get("/api/dashboard/requests"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentRequestCount").value(0));
    }
}
