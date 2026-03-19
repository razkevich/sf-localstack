package co.prodly.sflocalstack.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MetadataIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void metadataFlowsWorkAfterReset() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());

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

        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT DurableId FROM FlowDefinitionView WHERE ApiName = 'LoginFlow'"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].DurableId").value("FlowDefinition/LoginFlow"));
    }
}
