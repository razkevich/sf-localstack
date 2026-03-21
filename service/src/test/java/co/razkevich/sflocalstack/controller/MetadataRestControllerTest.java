package co.razkevich.sflocalstack.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MetadataRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void resetAndSeed() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());
        for (String body : List.of(
                "{\"type\":\"CustomApplication\",\"fullName\":\"SalesConsole\",\"fileName\":\"applications/SalesConsole.app\",\"directoryName\":\"applications\",\"inFolder\":false,\"metaFile\":true,\"label\":\"Sales Console\",\"attributes\":{}}",
                "{\"type\":\"FlowDefinition\",\"fullName\":\"LoginFlow\",\"fileName\":\"flowDefinitions/LoginFlow.flowDefinition\",\"directoryName\":\"flowDefinitions\",\"inFolder\":false,\"metaFile\":true,\"label\":\"Login Flow\",\"attributes\":{}}"
        )) {
            mockMvc.perform(post("/api/admin/metadata/resources")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void toolingQueryReturnsTabsApplicationsCustomSettingsAndLoginFlows() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/tooling/query")
                        .param("q", "SELECT Name FROM TabDefinition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityTypeName").value("TabDefinition"))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.records[0].Name").exists());

        mockMvc.perform(get("/services/data/v60.0/tooling/query")
                        .param("q", "SELECT DeveloperName, NamespacePrefix FROM CustomApplication"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].DeveloperName").exists());

        mockMvc.perform(get("/services/data/v60.0/tooling/query")
                        .param("q", "SELECT QualifiedApiName FROM EntityDefinition WHERE IsCustomSetting = true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].QualifiedApiName").exists());

        mockMvc.perform(get("/services/data/v60.0/tooling/query")
                        .param("q", "SELECT DeveloperName, NamespacePrefix FROM FlowDefinition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].DeveloperName").exists());

        mockMvc.perform(get("/services/data/v60.0/tooling/query")
                        .param("q", "SELECT VersionNumber FROM Flow WHERE DefinitionId = 'FlowDefinition/LoginFlow'"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(2))
                .andExpect(jsonPath("$.records[0].VersionNumber").exists());

        mockMvc.perform(get("/services/data/v60.0/tooling/query")
                        .param("q", "SELECT EntityDefinition.QualifiedApiName, QualifiedApiName FROM FieldDefinition WHERE IsHistoryTracked = false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityTypeName").value("FieldDefinition"))
                .andExpect(jsonPath("$.records[0].QualifiedApiName").exists());
    }
}
