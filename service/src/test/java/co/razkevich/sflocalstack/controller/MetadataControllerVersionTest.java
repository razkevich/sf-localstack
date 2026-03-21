package co.razkevich.sflocalstack.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MetadataControllerVersionTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void reset() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());
    }

    @Test
    void describeMetadataRoutesOnV66() throws Exception {
        mockMvc.perform(post("/services/Soap/m/66.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(envelope("<met:describeMetadata/>")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<describeMetadataResponse>")))
                .andExpect(content().string(containsString("<metadataObjects>")));
    }

    @Test
    void listMetadataRoutesOnV66() throws Exception {
        mockMvc.perform(post("/services/Soap/m/66.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(envelope("""
                                <met:listMetadata>
                                  <met:queries>
                                    <met:type>ApexClass</met:type>
                                  </met:queries>
                                  <met:asOfVersion>66.0</met:asOfVersion>
                                </met:listMetadata>
                                """)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<listMetadataResponse>")))
                .andExpect(content().string(containsString("<type>ApexClass</type>")));
    }

    @Test
    void readMetadataRoutesOnV66() throws Exception {
        mockMvc.perform(post("/services/Soap/m/66.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(envelope("""
                                <met:readMetadata>
                                  <met:type>StandardValueSet</met:type>
                                  <met:fullNames>valueSet</met:fullNames>
                                </met:readMetadata>
                                """)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<readMetadataResponse>")))
                .andExpect(content().string(containsString("xsi:type=\"StandardValueSet\"")));
    }

    @Test
    void cancelDeployRoutesOnV66() throws Exception {
        String deployResponse = mockMvc.perform(post("/services/Soap/m/66.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(envelope("<met:deploy><met:ZipFile>dGVzdA==</met:ZipFile></met:deploy>")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String deployId = deployResponse.replaceAll("(?s).*<id>([^<]+)</id>.*", "$1");

        mockMvc.perform(post("/services/Soap/m/66.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(envelope("""
                                <met:cancelDeploy>
                                  <met:asyncProcessId>%s</met:asyncProcessId>
                                </met:cancelDeploy>
                                """.formatted(deployId))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<cancelDeployResponse>")))
                .andExpect(content().string(containsString("<id>")));
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
