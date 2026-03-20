package co.prodly.sflocalstack.integration;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cross-surface integration test: verifies REST, Bulk, and Metadata surfaces
 * coexist and reset returns the app to a deterministic baseline.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CrossSurfaceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void restBulkAndMetadataAllWorkThenResetRestoresBaseline() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());

        // --- REST: create a record ---
        mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"CrossSurface REST\",\"Industry\":\"Testing\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Id, Name FROM Account WHERE Name = 'CrossSurface REST'"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(1));

        // --- Bulk: ingest a record ---
        String jobResponse = mockMvc.perform(post("/services/data/v60.0/jobs/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"object\":\"Account\",\"operation\":\"insert\",\"contentType\":\"CSV\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String jobId = jobResponse.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/services/data/v60.0/jobs/ingest/{jobId}/batches", jobId)
                        .contentType("text/csv")
                        .content("Name,Industry\nCrossSurface Bulk,Technology\n"))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/services/data/v60.0/jobs/ingest/{jobId}", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"UploadComplete\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/services/data/v60.0/jobs/ingest/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("JobComplete"));

        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Id, Name FROM Account WHERE Name = 'CrossSurface Bulk'"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(1));

        // --- Metadata SOAP: describeMetadata ---
        mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content("""
                                <?xml version="1.0" encoding="UTF-8"?>
                                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                                  xmlns:met="http://soap.sforce.com/2006/04/metadata">
                                  <soapenv:Body>
                                    <met:describeMetadata/>
                                  </soapenv:Body>
                                </soapenv:Envelope>
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CustomField")));

        // --- Dashboard overview reflects cross-surface activity ---
        mockMvc.perform(get("/api/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.totalRecords").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.recentRequestCount").value(org.hamcrest.Matchers.greaterThan(0)));

        // --- Reset restores seeded baseline across all surfaces ---
        mockMvc.perform(post("/reset")).andExpect(status().isOk());

        // Verify request log was cleared immediately after reset (before any new calls are logged)
        mockMvc.perform(get("/api/dashboard/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Id, Name FROM Account WHERE Name = 'CrossSurface REST'"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(0));

        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Id, Name FROM Account WHERE Name = 'CrossSurface Bulk'"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(0));
    }
}
