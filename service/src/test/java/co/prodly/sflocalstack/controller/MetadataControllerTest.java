package co.prodly.sflocalstack.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MetadataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void reset() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());
    }

    @Test
    void describeMetadataReturnsSupportedTypes() throws Exception {
        mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(envelope("<met:describeMetadata/>")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<metadataObjects>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<xmlName>CustomField</xmlName>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<xmlName>GlobalValueSet</xmlName>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<partialSaveAllowed>false</partialSaveAllowed>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<testRequired>false</testRequired>")));
    }

    @Test
    void listMetadataReturnsFilePropertiesForRequestedType() throws Exception {
        mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(envelope("""
                                <met:listMetadata>
                                  <met:queries>
                                    <met:type>CustomField</met:type>
                                  </met:queries>
                                  <met:asOfVersion>60.0</met:asOfVersion>
                                </met:listMetadata>
                                """)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<fullName>Account.Type</fullName>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<type>CustomField</type>")));
    }

    @Test
    void readMetadataReturnsTypedRecords() throws Exception {
        mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(envelope("""
                                <met:readMetadata>
                                  <met:type>GlobalValueSet</met:type>
                                  <met:fullNames>CustomerPriority</met:fullNames>
                                </met:readMetadata>
                                """)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("xsi:type=\"GlobalValueSet\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<fullName>CustomerPriority</fullName>")));
    }

    @Test
    void deployStatusAndCancelFlowsReturnDeterministicResults() throws Exception {
        String deployResponse = mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(envelope("""
                                <met:deploy>
                                  <met:ZipFile>dGVzdA==</met:ZipFile>
                                </met:deploy>
                                """)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<id>")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String deployId = deployResponse.replaceAll("(?s).*<id>([^<]+)</id>.*", "$1");

        mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(envelope("""
                                <met:checkDeployStatus>
                                  <met:asyncProcessId>%s</met:asyncProcessId>
                                  <met:includeDetails>true</met:includeDetails>
                                </met:checkDeployStatus>
                                """.formatted(deployId))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<done>true</done>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<status>Succeeded</status>")));

        mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(envelope("""
                                <met:cancelDeploy>
                                  <met:asyncProcessId>%s</met:asyncProcessId>
                                </met:cancelDeploy>
                                """.formatted(deployId))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<done>true</done>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<id>%s</id>".formatted(deployId))));
    }

    private String envelope(String body) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:met="http://soap.sforce.com/2006/04/metadata">
                  <soapenv:Body>
                    %s
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(body);
    }
}
