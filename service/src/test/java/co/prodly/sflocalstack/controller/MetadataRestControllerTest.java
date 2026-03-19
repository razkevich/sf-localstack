package co.prodly.sflocalstack.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MetadataRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void toolingQueryReturnsTabsApplicationsCustomSettingsAndLoginFlows() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/tooling/query")
                        .param("q", "SELECT Name FROM TabDefinition"))
                .andExpect(status().isOk())
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
    }
}
