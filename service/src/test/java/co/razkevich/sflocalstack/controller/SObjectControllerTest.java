package co.razkevich.sflocalstack.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SObjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void reset() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());
        mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .contentType(APPLICATION_JSON)
                        .content("{\"Name\":\"Acme Corp\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void describeReturnsInferredFieldMetadata() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/sobjects/Account/describe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Account"))
                .andExpect(jsonPath("$.labelPlural").value("Accounts"))
                .andExpect(jsonPath("$.urls.describe").value("/services/data/v60.0/sobjects/Account/describe"))
                .andExpect(jsonPath("$.fields[?(@.name == 'Name')].type").value("string"))
                .andExpect(jsonPath("$.fields[?(@.name == 'CreatedDate')].type").value("datetime"));
    }

    @Test
    void listReturnsObjectDescribeAndRecentItems() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/sobjects/Account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objectDescribe.name").value("Account"))
                .andExpect(jsonPath("$.objectDescribe.urls.sobject").value("/services/data/v60.0/sobjects/Account"))
                .andExpect(jsonPath("$.recentItems[0].attributes.type").value("Account"))
                .andExpect(jsonPath("$.recentItems[0].attributes.url").exists());
    }

    @Test
    void externalIdUpsertCreatesRecordWithLocationHeader() throws Exception {
        mockMvc.perform(patch("/services/data/v60.0/sobjects/Account/External_Id__c/EXT-001")
                        .contentType(APPLICATION_JSON)
                        .content("{\"Name\":\"Upsert Created\",\"Industry\":\"Testing\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/services/data/v60.0/sobjects/Account/")))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void externalIdUpsertUpdatesExistingRecordWithNoContent() throws Exception {
        mockMvc.perform(patch("/services/data/v60.0/sobjects/Account/External_Id__c/EXT-001")
                        .contentType(APPLICATION_JSON)
                        .content("{\"Name\":\"Upsert Created\",\"Industry\":\"Testing\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/services/data/v60.0/sobjects/Account/External_Id__c/EXT-001")
                        .contentType(APPLICATION_JSON)
                        .content("{\"Name\":\"Upsert Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.created").value(false));

        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Id, Name FROM Account WHERE External_Id__c = 'EXT-001'"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(1))
                .andExpect(jsonPath("$.records[0].Name").value("Upsert Updated"));
    }

    @Test
    void missingRecordUpdateReturnsSalesforceStyleNotFound() throws Exception {
        mockMvc.perform(patch("/services/data/v60.0/sobjects/Account/001MISSING")
                        .contentType(APPLICATION_JSON)
                        .content("{\"Name\":\"Nope\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$[0].errorCode").value("NOT_FOUND"));
    }

    @Test
    void replaceRecordCanRemoveFields() throws Exception {
        String createResponse = mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .contentType(APPLICATION_JSON)
                        .content("{\"Name\":\"Replace Test\",\"Industry\":\"Testing\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String recordId = createResponse.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/services/data/v60.0/sobjects/Account/{id}", recordId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"Name\":\"Replace Test Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(recordId));

        mockMvc.perform(get("/services/data/v60.0/sobjects/Account/{id}", recordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Name").value("Replace Test Updated"))
                .andExpect(jsonPath("$.Industry").doesNotExist());
    }
}
