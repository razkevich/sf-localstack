package co.prodly.sflocalstack.integration;

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
class RestQueryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createThenQueryThenResetRestoresSeededBaseline() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());

        mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"Feature One Query Test\",\"Industry\":\"Testing\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Id, Name FROM Account WHERE Name = 'Feature One Query Test'"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(1));

        mockMvc.perform(post("/reset")).andExpect(status().isOk());

        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Id, Name FROM Account WHERE Name = 'Feature One Query Test'"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(0));
    }
}
