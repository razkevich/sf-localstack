package co.prodly.sflocalstack.controller;

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
class OAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void tokenEndpointReturnsFakeJwt() throws Exception {
        mockMvc.perform(post("/services/oauth2/token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.instance_url").value("http://localhost:8080"))
                .andExpect(jsonPath("$.token_type").value("Bearer"));
    }

    @Test
    void userinfoEndpointReturnsOrgDetails() throws Exception {
        mockMvc.perform(get("/services/oauth2/userinfo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").exists())
                .andExpect(jsonPath("$.organization_id").exists())
                .andExpect(jsonPath("$.username").exists());
    }
}
