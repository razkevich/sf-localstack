package co.prodly.sflocalstack.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BulkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void reset() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());
    }

    @Test
    void createUploadCloseInspectAndDeleteJob() throws Exception {
        String jobResponse = mockMvc.perform(post("/services/data/v60.0/jobs/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"object\":\"Account\",\"operation\":\"insert\",\"contentType\":\"CSV\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("Open"))
                .andExpect(jsonPath("$.contentUrl").value(org.hamcrest.Matchers.containsString("/batches")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String jobId = jobResponse.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/services/data/v60.0/jobs/ingest/{jobId}/batches", jobId)
                        .contentType("text/csv")
                        .content("Name,Industry\nBulk Corp,Technology\n"))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        mockMvc.perform(patch("/services/data/v60.0/jobs/ingest/{jobId}", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"UploadComplete\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("UploadComplete"))
                .andExpect(jsonPath("$.numberRecordsProcessed").doesNotExist())
                .andExpect(jsonPath("$.numberRecordsFailed").doesNotExist());

        mockMvc.perform(get("/services/data/v60.0/jobs/ingest/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId))
                .andExpect(jsonPath("$.state").value("JobComplete"))
                .andExpect(jsonPath("$.numberRecordsProcessed").value(1))
                .andExpect(jsonPath("$.numberRecordsFailed").value(0));

        mockMvc.perform(get("/services/data/v60.0/jobs/ingest/{jobId}/successfulResults", jobId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("sf__Id,sf__Created")));

        mockMvc.perform(delete("/services/data/v60.0/jobs/ingest/{jobId}", jobId))
                .andExpect(status().isNoContent());
    }
}
