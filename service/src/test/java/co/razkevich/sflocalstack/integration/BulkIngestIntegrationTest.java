package co.razkevich.sflocalstack.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BulkIngestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void bulkInsertMakesRecordsQueryableAfterClose() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());

        String jobResponse = mockMvc.perform(post("/services/data/v60.0/jobs/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"object\":\"Account\",\"operation\":\"insert\",\"contentType\":\"CSV\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String jobId = jobResponse.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/services/data/v60.0/jobs/ingest/{jobId}/batches", jobId)
                        .contentType("text/csv")
                        .content("Name,Industry\nBulk Integration,Technology\n"))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/services/data/v60.0/jobs/ingest/{jobId}", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"UploadComplete\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("UploadComplete"));

        mockMvc.perform(get("/services/data/v60.0/jobs/ingest/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("JobComplete"))
                .andExpect(jsonPath("$.numberRecordsProcessed").value(1));

        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Id, Name FROM Account WHERE Name = 'Bulk Integration'"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(1));
    }
}
