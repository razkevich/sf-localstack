package co.razkevich.sflocalstack.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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
        for (String body : List.of(
                "{\"type\":\"CustomField\",\"fullName\":\"Account.Type\",\"fileName\":\"objects/Account.object\",\"directoryName\":\"objects\",\"inFolder\":false,\"metaFile\":true,\"label\":\"Type\",\"attributes\":{\"fieldType\":\"Text\"}}",
                "{\"type\":\"CustomField\",\"fullName\":\"Account.Status\",\"fileName\":\"objects/Account.object\",\"directoryName\":\"objects\",\"inFolder\":false,\"metaFile\":true,\"label\":\"Status\",\"attributes\":{\"fieldType\":\"Text\"}}",
                "{\"type\":\"StandardValueSet\",\"fullName\":\"AccountType\",\"fileName\":\"standardValueSets/AccountType.standardValueSet\",\"directoryName\":\"standardValueSets\",\"inFolder\":false,\"metaFile\":true,\"label\":\"Account Type\",\"attributes\":{}}",
                "{\"type\":\"StandardValueSet\",\"fullName\":\"IndustryType\",\"fileName\":\"standardValueSets/IndustryType.standardValueSet\",\"directoryName\":\"standardValueSets\",\"inFolder\":false,\"metaFile\":true,\"label\":\"Industry Type\",\"attributes\":{}}",
                "{\"type\":\"CustomTab\",\"fullName\":\"standard-Account\",\"fileName\":\"tabs/standard-Account.tab\",\"directoryName\":\"tabs\",\"inFolder\":false,\"metaFile\":true,\"label\":\"Account\",\"attributes\":{}}",
                "{\"type\":\"GlobalValueSet\",\"fullName\":\"CustomerPriority\",\"fileName\":\"globalValueSets/CustomerPriority.globalValueSet\",\"directoryName\":\"globalValueSets\",\"inFolder\":false,\"metaFile\":true,\"label\":\"Customer Priority\",\"attributes\":{\"values\":[\"High\",\"Medium\",\"Low\"]}}"
        )) {
            mockMvc.perform(post("/api/admin/metadata/resources")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isCreated());
        }
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

    @Test
    void listMetadataWithMultipleTypesReturnsAllMatchingEntries() throws Exception {
        mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(envelope("""
                                <met:listMetadata>
                                  <met:queries>
                                    <met:type>CustomField</met:type>
                                  </met:queries>
                                  <met:queries>
                                    <met:type>CustomTab</met:type>
                                  </met:queries>
                                  <met:asOfVersion>60.0</met:asOfVersion>
                                </met:listMetadata>
                                """)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<fullName>Account.Type</fullName>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<type>CustomField</type>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<fullName>standard-Account</fullName>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<type>CustomTab</type>")));
    }

    @Test
    void retrieveReturnsIdWithPendingStatus() throws Exception {
        mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(envelope("""
                                <met:retrieve>
                                  <met:retrieveRequest>
                                    <met:apiVersion>60.0</met:apiVersion>
                                    <met:unpackaged>
                                      <met:types>
                                        <met:members>*</met:members>
                                        <met:name>CustomField</met:name>
                                      </met:types>
                                    </met:unpackaged>
                                  </met:retrieveRequest>
                                </met:retrieve>
                                """)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<retrieveResponse>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<id>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<done>false</done>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<status>Pending</status>")));
    }

    @Test
    void checkRetrieveStatusReturnsZipWhenComplete() throws Exception {
        String retrieveResponse = mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(envelope("""
                                <met:retrieve>
                                  <met:retrieveRequest>
                                    <met:apiVersion>60.0</met:apiVersion>
                                    <met:unpackaged>
                                      <met:types>
                                        <met:members>*</met:members>
                                        <met:name>CustomField</met:name>
                                      </met:types>
                                    </met:unpackaged>
                                  </met:retrieveRequest>
                                </met:retrieve>
                                """)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String retrieveId = retrieveResponse.replaceAll("(?s).*<id>([^<]+)</id>.*", "$1");

        mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(envelope("""
                                <met:checkRetrieveStatus>
                                  <met:asyncProcessId>%s</met:asyncProcessId>
                                  <met:includeZip>true</met:includeZip>
                                </met:checkRetrieveStatus>
                                """.formatted(retrieveId))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<done>true</done>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<success>true</success>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<status>Succeeded</status>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<zipFile>")));
    }

    @Test
    void retrieveWithExplicitMemberReturnsOnlyMatchedComponent() throws Exception {
        String retrieveResponse = mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(envelope("""
                                <met:retrieve>
                                  <met:retrieveRequest>
                                    <met:apiVersion>60.0</met:apiVersion>
                                    <met:unpackaged>
                                      <met:types>
                                        <met:members>Account.Type</met:members>
                                        <met:name>CustomField</met:name>
                                      </met:types>
                                    </met:unpackaged>
                                  </met:retrieveRequest>
                                </met:retrieve>
                                """)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String retrieveId = retrieveResponse.replaceAll("(?s).*<id>([^<]+)</id>.*", "$1");

        mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(envelope("""
                                <met:checkRetrieveStatus>
                                  <met:asyncProcessId>%s</met:asyncProcessId>
                                  <met:includeZip>true</met:includeZip>
                                </met:checkRetrieveStatus>
                                """.formatted(retrieveId))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<done>true</done>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<success>true</success>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<numberComponentsTotal>1</numberComponentsTotal>")));
    }

    @Test
    void retrieveWithMultipleTypesReturnsComponentsFromAllTypes() throws Exception {
        String retrieveResponse = mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(envelope("""
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
                                """)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String retrieveId = retrieveResponse.replaceAll("(?s).*<id>([^<]+)</id>.*", "$1");

        mockMvc.perform(post("/services/Soap/m/60.0")
                        .contentType(MediaType.TEXT_XML)
                        .content(envelope("""
                                <met:checkRetrieveStatus>
                                  <met:asyncProcessId>%s</met:asyncProcessId>
                                  <met:includeZip>true</met:includeZip>
                                </met:checkRetrieveStatus>
                                """.formatted(retrieveId))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<done>true</done>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<numberComponentsTotal>4</numberComponentsTotal>")));
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
