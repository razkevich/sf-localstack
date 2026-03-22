package co.razkevich.sflocalstack.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void reset() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());
        String acmeResponse = mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"Acme Corp\",\"Industry\":\"Technology\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String acmeId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(acmeResponse).get("id").asText();
        mockMvc.perform(post("/services/data/v60.0/sobjects/Contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"FirstName\":\"John\",\"LastName\":\"Doe\",\"AccountId\":\"" + acmeId + "\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"Globex Corp\",\"Industry\":\"Manufacturing\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void soqlSelectReturnsSeededAccounts() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Id, Name FROM Account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true))
                .andExpect(jsonPath("$.totalSize").isNumber())
                .andExpect(jsonPath("$.records").isArray());
    }

    @Test
    void soqlSelectWithLimitRespectsLimit() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Id FROM Account LIMIT 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(1));
    }

    @Test
    void soqlWhereAndRelationshipProjectionReturnSalesforceShape() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Id, FirstName, Account.Name FROM Contact WHERE LastName = 'Doe'"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(1))
                .andExpect(jsonPath("$.records[0].attributes.type").value("Contact"))
                .andExpect(jsonPath("$.records[0].FirstName").value("John"))
                .andExpect(jsonPath("$.records[0].Account.Name").value("Acme Corp"));
    }

    @Test
    void malformedSoqlReturnsSalesforceStyleError() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "NOT VALID SOQL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].errorCode").value("MALFORMED_QUERY"))
                .andExpect(jsonPath("$[0].message").exists());
    }
}
