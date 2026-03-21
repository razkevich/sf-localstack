package co.razkevich.sflocalstack.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    @BeforeEach
    void resetAndSeed() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());
        for (String body : List.of(
                "{\"type\":\"FlowDefinition\",\"fullName\":\"LoginFlow\",\"fileName\":\"flowDefinitions/LoginFlow.flowDefinition\",\"directoryName\":\"flowDefinitions\",\"inFolder\":false,\"metaFile\":true,\"label\":\"Login Flow\",\"attributes\":{}}",
                "{\"type\":\"CustomField\",\"fullName\":\"Account.Type\",\"fileName\":\"objects/Account.object\",\"directoryName\":\"objects\",\"inFolder\":false,\"metaFile\":true,\"label\":\"Type\",\"attributes\":{\"fieldType\":\"Text\"}}",
                "{\"type\":\"GlobalValueSet\",\"fullName\":\"CustomerPriority\",\"fileName\":\"globalValueSets/CustomerPriority.globalValueSet\",\"directoryName\":\"globalValueSets\",\"inFolder\":false,\"metaFile\":true,\"label\":\"Customer Priority\",\"attributes\":{\"values\":[\"High\",\"Medium\",\"Low\"]}}",
                "{\"type\":\"StandardValueSet\",\"fullName\":\"AccountType\",\"fileName\":\"standardValueSets/AccountType.standardValueSet\",\"directoryName\":\"standardValueSets\",\"inFolder\":false,\"metaFile\":true,\"label\":\"Account Type\",\"attributes\":{}}"
        )) {
            mockMvc.perform(post("/api/admin/metadata/resources")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void metadataFlowsWorkAfterReset() throws Exception {

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

    @Test
    void multiTypeRetrieveProducesZipWithComponentsFromAllTypes() throws Exception {
        String retrieveEnvelope = """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:met="http://soap.sforce.com/2006/04/metadata">
                  <soapenv:Body>
                    <met:retrieve>
                      <met:retrieveRequest>
                        <met:apiVersion>60.0</met:apiVersion>
                        <met:unpackaged>
                          <met:types>
                            <met:members>*</met:members>
                            <met:name>CustomField</met:name>
                          </met:types>
                          <met:types>
                            <met:members>*</met:members>
                            <met:name>GlobalValueSet</met:name>
                          </met:types>
                        </met:unpackaged>
                      </met:retrieveRequest>
                    </met:retrieve>
                  </soapenv:Body>
                </soapenv:Envelope>
                """;

        String retrieveResponse = mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(retrieveEnvelope))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String retrieveId = retrieveResponse.replaceAll("(?s).*<id>([^<]+)</id>.*", "$1");
        assertThat(retrieveId).startsWith("09S");

        String statusResponse = mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content("""
                                <?xml version="1.0" encoding="UTF-8"?>
                                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                                  xmlns:met="http://soap.sforce.com/2006/04/metadata">
                                  <soapenv:Body>
                                    <met:checkRetrieveStatus>
                                      <met:asyncProcessId>%s</met:asyncProcessId>
                                      <met:includeZip>true</met:includeZip>
                                    </met:checkRetrieveStatus>
                                  </soapenv:Body>
                                </soapenv:Envelope>
                                """.formatted(retrieveId)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<done>true</done>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<success>true</success>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<zipFile>")))
                .andReturn().getResponse().getContentAsString();

        String zipBase64 = statusResponse.replaceAll("(?s).*<zipFile>([^<]+)</zipFile>.*", "$1");
        assertThat(zipBase64).isNotBlank();

        byte[] zipBytes = java.util.Base64.getDecoder().decode(zipBase64);
        java.util.List<String> entryNames = new java.util.ArrayList<>();
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNames.add(entry.getName());
            }
        }
        assertThat(entryNames).anyMatch(n -> n.contains("Account.Type") || n.contains("objects"));
        assertThat(entryNames).anyMatch(n -> n.contains("CustomerPriority") || n.contains("globalValueSets"));
    }

    @Test
    void retrieveZipContainsSalesforceCompatibleXmlFormat() throws Exception {
        String retrieveResponse = mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content("""
                                <?xml version="1.0" encoding="UTF-8"?>
                                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                                  xmlns:met="http://soap.sforce.com/2006/04/metadata">
                                  <soapenv:Body>
                                    <met:retrieve>
                                      <met:retrieveRequest>
                                        <met:apiVersion>60.0</met:apiVersion>
                                        <met:unpackaged>
                                          <met:types>
                                            <met:members>*</met:members>
                                            <met:name>CustomField</met:name>
                                          </met:types>
                                          <met:types>
                                            <met:members>*</met:members>
                                            <met:name>GlobalValueSet</met:name>
                                          </met:types>
                                        </met:unpackaged>
                                      </met:retrieveRequest>
                                    </met:retrieve>
                                  </soapenv:Body>
                                </soapenv:Envelope>
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String retrieveId = retrieveResponse.replaceAll("(?s).*<id>([^<]+)</id>.*", "$1");

        String statusResponse = mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content("""
                                <?xml version="1.0" encoding="UTF-8"?>
                                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                                  xmlns:met="http://soap.sforce.com/2006/04/metadata">
                                  <soapenv:Body>
                                    <met:checkRetrieveStatus>
                                      <met:asyncProcessId>%s</met:asyncProcessId>
                                      <met:includeZip>true</met:includeZip>
                                    </met:checkRetrieveStatus>
                                  </soapenv:Body>
                                </soapenv:Envelope>
                                """.formatted(retrieveId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String zipBase64 = statusResponse.replaceAll("(?s).*<zipFile>([^<]+)</zipFile>.*", "$1");
        byte[] zipBytes = java.util.Base64.getDecoder().decode(zipBase64);

        java.util.Map<String, String> entryContents = new java.util.HashMap<>();
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryContents.put(entry.getName(), new String(zis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
        }

        // CustomField must produce <CustomObject> root with <fields> children
        String customObjectXml = entryContents.get("unpackaged/objects/Account.object");
        assertThat(customObjectXml).contains("<CustomObject");
        assertThat(customObjectXml).contains("<fields>");
        assertThat(customObjectXml).contains("<fullName>Type</fullName>");
        assertThat(customObjectXml).doesNotContain("<CustomField");

        // GlobalValueSet must produce <GlobalValueSet> root with <customValue> children
        String gvsXml = entryContents.get("unpackaged/globalValueSets/CustomerPriority.globalValueSet");
        assertThat(gvsXml).contains("<GlobalValueSet");
        assertThat(gvsXml).contains("<customValue>");
        assertThat(gvsXml).contains("<fullName>High</fullName>");
    }

    @Test
    void retrieveFlowProducesZipAndCheckRetrieveStatusReturnsContent() throws Exception {
        String retrieveEnvelope = """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:met="http://soap.sforce.com/2006/04/metadata">
                  <soapenv:Body>
                    <met:retrieve>
                      <met:retrieveRequest>
                        <met:apiVersion>60.0</met:apiVersion>
                        <met:unpackaged>
                          <met:types>
                            <met:members>*</met:members>
                            <met:name>CustomField</met:name>
                          </met:types>
                          <met:types>
                            <met:members>*</met:members>
                            <met:name>StandardValueSet</met:name>
                          </met:types>
                        </met:unpackaged>
                      </met:retrieveRequest>
                    </met:retrieve>
                  </soapenv:Body>
                </soapenv:Envelope>
                """;

        String retrieveResponse = mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(retrieveEnvelope))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<done>false</done>")))
                .andReturn().getResponse().getContentAsString();

        String retrieveId = retrieveResponse.replaceAll("(?s).*<id>([^<]+)</id>.*", "$1");
        assertThat(retrieveId).startsWith("09S");

        String statusResponse = mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content("""
                                <?xml version="1.0" encoding="UTF-8"?>
                                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                                  xmlns:met="http://soap.sforce.com/2006/04/metadata">
                                  <soapenv:Body>
                                    <met:checkRetrieveStatus>
                                      <met:asyncProcessId>%s</met:asyncProcessId>
                                      <met:includeZip>true</met:includeZip>
                                    </met:checkRetrieveStatus>
                                  </soapenv:Body>
                                </soapenv:Envelope>
                                """.formatted(retrieveId)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<done>true</done>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<success>true</success>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<zipFile>")))
                .andReturn().getResponse().getContentAsString();

        String zipBase64 = statusResponse.replaceAll("(?s).*<zipFile>([^<]+)</zipFile>.*", "$1");
        assertThat(zipBase64).isNotBlank();

        byte[] zipBytes = java.util.Base64.getDecoder().decode(zipBase64);
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                new java.io.ByteArrayInputStream(zipBytes))) {
            java.util.zip.ZipEntry entry = zis.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).startsWith("unpackaged/");
        }
    }
}
