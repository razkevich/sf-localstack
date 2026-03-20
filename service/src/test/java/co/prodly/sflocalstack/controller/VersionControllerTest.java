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
class VersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void versionsIncludesLegacyAndCurrentApiVersions() throws Exception {
        mockMvc.perform(get("/services/data/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value("50.0"))
                .andExpect(jsonPath("$[10].version").value("60.0"));
    }

    @Test
    void versionRootReturnsResourceMap() throws Exception {
        mockMvc.perform(get("/services/data/v50.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("/services/data/v60.0/query"));
    }
}
