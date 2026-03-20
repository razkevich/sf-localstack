package co.prodly.sflocalstack.integration;

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
class ApplicationShellIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void overviewReturnsExpectedShapeAfterReset() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("sf-localstack"))
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.apiVersion").value("v60.0"))
                .andExpect(jsonPath("$.totalRecords").isNumber())
                .andExpect(jsonPath("$.objectCounts").isArray());
    }

    @Test
    void requestLogGrowsAfterApiCallsThenClearsOnReset() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());

        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Id FROM Account LIMIT 1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));

        mockMvc.perform(post("/reset")).andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void resetResponseIndicatesSuccess() throws Exception {
        mockMvc.perform(post("/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void versionDiscoveryEndpointResponds() throws Exception {
        mockMvc.perform(get("/services/data"))
                .andExpect(status().isOk());
    }
}
