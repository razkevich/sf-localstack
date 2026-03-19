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
class SObjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void reset() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());
    }

    @Test
    void describeReturnsInferredFieldMetadata() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/sobjects/Account/describe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Account"))
                .andExpect(jsonPath("$.fields[?(@.name == 'Name')].type").value("string"))
                .andExpect(jsonPath("$.fields[?(@.name == 'CreatedDate')].type").value("datetime"));
    }

    @Test
    void listReturnsAttributesForRecords() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/sobjects/Account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].attributes.type").value("Account"))
                .andExpect(jsonPath("$.records[0].attributes.url").exists());
    }
}
