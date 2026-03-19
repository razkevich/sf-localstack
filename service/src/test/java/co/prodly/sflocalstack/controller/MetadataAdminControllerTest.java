package co.prodly.sflocalstack.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MetadataAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void metadataResourcesCanBeCreatedUpdatedAndDeleted() throws Exception {
        mockMvc.perform(post("/api/admin/metadata/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"CustomApplication",
                                  "fullName":"OpsConsole",
                                  "fileName":"applications/OpsConsole.app",
                                  "directoryName":"applications",
                                  "inFolder":false,
                                  "metaFile":true,
                                  "lastModifiedDate":"2026-03-19T20:00:00Z",
                                  "label":"Ops Console",
                                  "attributes":{}
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("OpsConsole"));

        mockMvc.perform(put("/api/admin/metadata/resources/CustomApplication/OpsConsole")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"CustomApplication",
                                  "fullName":"OpsConsole",
                                  "fileName":"applications/OpsConsole.app",
                                  "directoryName":"applications",
                                  "inFolder":false,
                                  "metaFile":true,
                                  "lastModifiedDate":"2026-03-19T20:00:00Z",
                                  "label":"Ops Console Updated",
                                  "attributes":{"color":"blue"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Ops Console Updated"));

        mockMvc.perform(get("/api/admin/metadata/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.fullName == 'OpsConsole')].label").value("Ops Console Updated"));

        mockMvc.perform(delete("/api/admin/metadata/resources/CustomApplication/OpsConsole"))
                .andExpect(status().isNoContent());
    }
}
