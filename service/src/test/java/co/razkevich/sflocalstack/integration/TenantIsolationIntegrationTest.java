package co.razkevich.sflocalstack.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantIsolationIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void twoUsers_haveIsolatedData() throws Exception {
        // Register user A (first user = ADMIN with legacy org)
        MvcResult regA = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tenantA\",\"email\":\"a@test.dev\",\"password\":\"passA\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String tokenA = JsonPath.read(regA.getResponse().getContentAsString(), "$.accessToken");

        // Register user B (gets a new org)
        MvcResult regB = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tenantB\",\"email\":\"b@test.dev\",\"password\":\"passB\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String tokenB = JsonPath.read(regB.getResponse().getContentAsString(), "$.accessToken");

        // Verify different org IDs in OAuth responses
        MvcResult oauthA = mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "password")
                        .param("username", "tenantA")
                        .param("password", "passA"))
                .andExpect(status().isOk())
                .andReturn();
        String orgIdA = extractOrgIdFromToken(JsonPath.read(oauthA.getResponse().getContentAsString(), "$.access_token"));

        MvcResult oauthB = mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "password")
                        .param("username", "tenantB")
                        .param("password", "passB"))
                .andExpect(status().isOk())
                .andReturn();
        String orgIdB = extractOrgIdFromToken(JsonPath.read(oauthB.getResponse().getContentAsString(), "$.access_token"));

        // Org IDs must be different
        org.junit.jupiter.api.Assertions.assertNotEquals(orgIdA, orgIdB,
                "Users must have different org IDs");

        // User A creates an Account
        mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .header("Authorization", "Bearer " + orgIdA + "!" + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"Alpha Corp\"}"))
                .andExpect(status().isCreated());

        // User B creates an Account
        mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .header("Authorization", "Bearer " + orgIdB + "!" + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"Beta Corp\"}"))
                .andExpect(status().isCreated());

        // User A queries — should see only Alpha Corp
        mockMvc.perform(get("/services/data/v60.0/query")
                        .header("Authorization", "Bearer " + orgIdA + "!" + tokenA)
                        .param("q", "SELECT Id, Name FROM Account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(1))
                .andExpect(jsonPath("$.records[0].Name").value("Alpha Corp"));

        // User B queries — should see only Beta Corp
        mockMvc.perform(get("/services/data/v60.0/query")
                        .header("Authorization", "Bearer " + orgIdB + "!" + tokenB)
                        .param("q", "SELECT Id, Name FROM Account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(1))
                .andExpect(jsonPath("$.records[0].Name").value("Beta Corp"));
    }

    @Test
    void orgDisplay_showsDifferentOrgIds() throws Exception {
        // Register two users
        MvcResult regA = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dispA\",\"email\":\"da@test.dev\",\"password\":\"pass\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String tokenA = JsonPath.read(regA.getResponse().getContentAsString(), "$.accessToken");

        MvcResult regB = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dispB\",\"email\":\"db@test.dev\",\"password\":\"pass\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String tokenB = JsonPath.read(regB.getResponse().getContentAsString(), "$.accessToken");

        // Get OAuth tokens with org IDs
        MvcResult oA = mockMvc.perform(post("/services/oauth2/token")
                .param("grant_type", "password").param("username", "dispA").param("password", "pass"))
                .andReturn();
        MvcResult oB = mockMvc.perform(post("/services/oauth2/token")
                .param("grant_type", "password").param("username", "dispB").param("password", "pass"))
                .andReturn();

        String fullTokenA = JsonPath.read(oA.getResponse().getContentAsString(), "$.access_token");
        String fullTokenB = JsonPath.read(oB.getResponse().getContentAsString(), "$.access_token");

        // Userinfo should show different org IDs
        mockMvc.perform(get("/services/oauth2/userinfo")
                        .header("Authorization", "Bearer " + fullTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization_id").exists());

        mockMvc.perform(get("/services/oauth2/userinfo")
                        .header("Authorization", "Bearer " + fullTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization_id").exists());

        // Verify org IDs are different
        String orgA = JsonPath.read(
                mockMvc.perform(get("/services/oauth2/userinfo").header("Authorization", "Bearer " + fullTokenA))
                        .andReturn().getResponse().getContentAsString(), "$.organization_id");
        String orgB = JsonPath.read(
                mockMvc.perform(get("/services/oauth2/userinfo").header("Authorization", "Bearer " + fullTokenB))
                        .andReturn().getResponse().getContentAsString(), "$.organization_id");

        org.junit.jupiter.api.Assertions.assertNotEquals(orgA, orgB);
    }

    private String extractOrgIdFromToken(String sfToken) {
        // Token format: <orgId>!<jwt>
        int bang = sfToken.indexOf('!');
        return bang > 0 ? sfToken.substring(0, bang) : "00D000000000001AAA";
    }
}
