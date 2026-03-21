package co.prodly.sflocalstack.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ToolingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void reset() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());
    }

    @Test
    void smallStaticResourceQueryReturnsBodyLength1() throws Exception {
        mockMvc.perform(get("/services/data/v66.0/tooling/query")
                        .param("q", "SELECT BodyLength FROM StaticResource WHERE Name IN ('SmallResource')"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(1))
                .andExpect(jsonPath("$.records[0].BodyLength").value(1));
    }

    @Test
    void bigStaticResourceQueryReturnsBodyLength200000000() throws Exception {
        mockMvc.perform(get("/services/data/v66.0/tooling/query")
                        .param("q", "SELECT BodyLength FROM StaticResource WHERE Name IN ('BigResource')"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(1))
                .andExpect(jsonPath("$.records[0].BodyLength").value(200000000));
    }

    @Test
    void unknownStaticResourceQueryReturnsEmpty() throws Exception {
        mockMvc.perform(get("/services/data/v66.0/tooling/query")
                        .param("q", "SELECT BodyLength FROM StaticResource WHERE Name IN ('NonExistentResource')"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(0));
    }

    @Test
    void toolingQueryEndpointAlsoReachableOnV60() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/tooling/query")
                        .param("q", "SELECT BodyLength FROM StaticResource WHERE Name IN ('SmallResource')"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].BodyLength").value(1));
    }
}
