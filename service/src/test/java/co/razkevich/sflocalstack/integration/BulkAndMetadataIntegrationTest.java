package co.razkevich.sflocalstack.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests Bulk API, Metadata SOAP, Tooling API, and their tenant isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BulkAndMetadataIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private String fullToken;

    @BeforeEach
    void setUp() throws Exception {
        String username = "bulk_" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"email\":\"" + username + "@test.dev\",\"password\":\"pass\"}"))
                .andExpect(status().isCreated());

        MvcResult oauth = mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "password")
                        .param("username", username)
                        .param("password", "pass"))
                .andReturn();
        fullToken = JsonPath.read(oauth.getResponse().getContentAsString(), "$.access_token");
    }

    // === Bulk API ===

    @Test
    void createBulkIngestJob() throws Exception {
        mockMvc.perform(post("/services/data/v60.0/jobs/ingest")
                        .header("Authorization", "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"object\":\"Account\",\"operation\":\"insert\",\"contentType\":\"CSV\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.state").value("Open"))
                .andExpect(jsonPath("$.object").value("Account"))
                .andExpect(jsonPath("$.operation").value("insert"));
    }

    @Test
    void closeBulkJob() throws Exception {
        MvcResult created = mockMvc.perform(post("/services/data/v60.0/jobs/ingest")
                        .header("Authorization", "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"object\":\"Account\",\"operation\":\"insert\",\"contentType\":\"CSV\"}"))
                .andReturn();
        String jobId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(patch("/services/data/v60.0/jobs/ingest/" + jobId)
                        .header("Authorization", "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"UploadComplete\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("UploadComplete"));
    }

    @Test
    void getBulkJobById() throws Exception {
        MvcResult created = mockMvc.perform(post("/services/data/v60.0/jobs/ingest")
                        .header("Authorization", "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"object\":\"Contact\",\"operation\":\"insert\",\"contentType\":\"CSV\"}"))
                .andReturn();
        String jobId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/services/data/v60.0/jobs/ingest/" + jobId)
                        .header("Authorization", "Bearer " + fullToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId))
                .andExpect(jsonPath("$.object").value("Contact"));
    }

    // === Metadata SOAP ===

    @Test
    void metadataDescribe() throws Exception {
        String soapBody = """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:met="http://soap.sforce.com/2006/04/metadata">
                  <soapenv:Body><met:describeMetadata/></soapenv:Body>
                </soapenv:Envelope>""";

        mockMvc.perform(post("/services/Soap/m/60.0")
                        .header("Authorization", "Bearer " + fullToken)
                        .contentType(MediaType.TEXT_XML)
                        .content(soapBody))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("describeMetadataResponse")))
                .andExpect(content().string(containsString("describeMetadataResponse")));
    }

    @Test
    void metadataListCustomObjects() throws Exception {
        String soapBody = """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:met="http://soap.sforce.com/2006/04/metadata">
                  <soapenv:Body>
                    <met:listMetadata>
                      <met:queries><met:type>CustomObject</met:type></met:queries>
                    </met:listMetadata>
                  </soapenv:Body>
                </soapenv:Envelope>""";

        mockMvc.perform(post("/services/Soap/m/60.0")
                        .header("Authorization", "Bearer " + fullToken)
                        .contentType(MediaType.TEXT_XML)
                        .content(soapBody))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("listMetadataResponse")));
    }

    // === Tooling API ===

    @Test
    void toolingQuery_tabDefinition() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/tooling/query")
                        .header("Authorization", "Bearer " + fullToken)
                        .param("q", "SELECT Name FROM TabDefinition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true))
                .andExpect(jsonPath("$.records").isArray());
    }

    // === Tenant Isolation for Bulk and Metadata ===

    @Test
    void bulkJobIsolation_acrossOrgs() throws Exception {
        // Register second user
        String user2 = "bulk2_" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user2 + "\",\"email\":\"" + user2 + "@test.dev\",\"password\":\"pass\"}"))
                .andExpect(status().isCreated());
        MvcResult oauth2 = mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "password")
                        .param("username", user2)
                        .param("password", "pass"))
                .andReturn();
        String token2 = JsonPath.read(oauth2.getResponse().getContentAsString(), "$.access_token");

        // User 1 creates a bulk job
        mockMvc.perform(post("/services/data/v60.0/jobs/ingest")
                        .header("Authorization", "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"object\":\"Account\",\"operation\":\"insert\",\"contentType\":\"CSV\"}"))
                .andExpect(status().isOk());

        // User 2 lists bulk jobs — should not see User 1's job
        mockMvc.perform(get("/services/data/v60.0/jobs/ingest")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(0)));
    }

    @Test
    void sObjectIsolation_crossOrgRecordNotFound() throws Exception {
        // Create record as user 1
        MvcResult created = mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .header("Authorization", "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"Secret Corp\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String recordId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        // Register second user
        String user2 = "iso_" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user2 + "\",\"email\":\"" + user2 + "@test.dev\",\"password\":\"pass\"}"))
                .andExpect(status().isCreated());
        MvcResult oauth2 = mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "password")
                        .param("username", user2)
                        .param("password", "pass"))
                .andReturn();
        String token2 = JsonPath.read(oauth2.getResponse().getContentAsString(), "$.access_token");

        // User 2 tries to GET the record by ID — should get 404
        mockMvc.perform(get("/services/data/v60.0/sobjects/Account/" + recordId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isNotFound());

        // User 2 queries — should find 0 records
        mockMvc.perform(get("/services/data/v60.0/query")
                        .header("Authorization", "Bearer " + token2)
                        .param("q", "SELECT Id FROM Account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(0));
    }
}
