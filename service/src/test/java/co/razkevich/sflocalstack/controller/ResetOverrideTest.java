package co.razkevich.sflocalstack.controller;

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
class ResetOverrideTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void resetWithSeedOverrideAppliesFieldValue() throws Exception {
        mockMvc.perform(post("/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "seedOverrides": {
                                    "PDRI__Connection__c": {
                                      "PDRI__Instance_URL__c": "http://override-host:9999"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Id, PDRI__Instance_URL__c FROM PDRI__Connection__c"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].PDRI__Instance_URL__c").value("http://override-host:9999"));
    }

    @Test
    void resetWithoutBodyRestoresDefaultSeedUrl() throws Exception {
        // First override the URL
        mockMvc.perform(post("/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seedOverrides":{"PDRI__Connection__c":{"PDRI__Instance_URL__c":"http://temp:1234"}}}
                                """))
                .andExpect(status().isOk());

        // Then reset without override - should restore default seed value
        mockMvc.perform(post("/reset")).andExpect(status().isOk());

        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT PDRI__Instance_URL__c FROM PDRI__Connection__c"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].PDRI__Instance_URL__c").value("http://localhost:8080"));
    }

    @Test
    void resetWithMalformedBodySucceeds() throws Exception {
        mockMvc.perform(post("/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
