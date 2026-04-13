package co.razkevich.sflocalstack.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Simulates the complete SF CLI developer workflow via MockMvc.
 * Every test a developer would run with `sf` CLI is covered here.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SfCliWorkflowIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private String fullToken; // orgId!jwt format
    private String userId;

    @BeforeEach
    void setUp() throws Exception {
        String username = "cli_" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult reg = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"email\":\"" + username + "@test.dev\",\"password\":\"pass123\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        userId = JsonPath.read(reg.getResponse().getContentAsString(), "$.user.id");

        MvcResult oauth = mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "password")
                        .param("username", username)
                        .param("password", "pass123"))
                .andExpect(status().isOk())
                .andReturn();
        fullToken = JsonPath.read(oauth.getResponse().getContentAsString(), "$.access_token");
    }

    // === Version Discovery ===

    @Test
    void versionDiscovery_returnsApiVersions() throws Exception {
        mockMvc.perform(get("/services/data/")
                        .header("Authorization", "Bearer " + fullToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[-1].version").value("60.0"));
    }

    // === sObject CRUD ===

    @Test
    void createAndQueryAccount() throws Exception {
        mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .header("Authorization", "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"Test Corp\",\"Industry\":\"Technology\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.id").isNotEmpty());

        mockMvc.perform(get("/services/data/v60.0/query")
                        .header("Authorization", "Bearer " + fullToken)
                        .param("q", "SELECT Id, Name, Industry FROM Account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(1))
                .andExpect(jsonPath("$.done").value(true))
                .andExpect(jsonPath("$.records[0].Name").value("Test Corp"))
                .andExpect(jsonPath("$.records[0].Industry").value("Technology"));
    }

    @Test
    void createAndQueryContact() throws Exception {
        mockMvc.perform(post("/services/data/v60.0/sobjects/Contact")
                        .header("Authorization", "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"FirstName\":\"Jane\",\"LastName\":\"Doe\",\"Email\":\"jane@test.com\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/services/data/v60.0/query")
                        .header("Authorization", "Bearer " + fullToken)
                        .param("q", "SELECT Id, FirstName, LastName, Email FROM Contact"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(1))
                .andExpect(jsonPath("$.records[0].FirstName").value("Jane"))
                .andExpect(jsonPath("$.records[0].LastName").value("Doe"))
                .andExpect(jsonPath("$.records[0].Email").value("jane@test.com"));
    }

    @Test
    void updateRecord() throws Exception {
        MvcResult created = mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .header("Authorization", "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"Update Me\",\"Industry\":\"Old\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(patch("/services/data/v60.0/sobjects/Account/" + id)
                        .header("Authorization", "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Industry\":\"Updated\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/services/data/v60.0/query")
                        .header("Authorization", "Bearer " + fullToken)
                        .param("q", "SELECT Id, Industry FROM Account WHERE Name='Update Me'"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].Industry").value("Updated"));
    }

    @Test
    void deleteRecord() throws Exception {
        MvcResult created = mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .header("Authorization", "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"Delete Me\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(delete("/services/data/v60.0/sobjects/Account/" + id)
                        .header("Authorization", "Bearer " + fullToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/services/data/v60.0/query")
                        .header("Authorization", "Bearer " + fullToken)
                        .param("q", "SELECT Id FROM Account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(0));
    }

    @Test
    void getRecordById() throws Exception {
        MvcResult created = mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .header("Authorization", "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"By ID Corp\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/services/data/v60.0/sobjects/Account/" + id)
                        .header("Authorization", "Bearer " + fullToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attributes.type").value("Account"))
                .andExpect(jsonPath("$.Name").value("By ID Corp"));
    }

    // === sObject Describe ===

    @Test
    void describeSObject() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/sobjects/Account/describe")
                        .header("Authorization", "Bearer " + fullToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Account"))
                .andExpect(jsonPath("$.queryable").value(true))
                .andExpect(jsonPath("$.createable").value(true))
                .andExpect(jsonPath("$.fields").isArray())
                .andExpect(jsonPath("$.fields", hasSize(greaterThanOrEqualTo(10))));
    }

    // === SOQL Features ===

    @Test
    void soqlWhereClause() throws Exception {
        mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .header("Authorization", "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"Tech Corp\",\"Industry\":\"Technology\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .header("Authorization", "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"Finance Corp\",\"Industry\":\"Finance\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/services/data/v60.0/query")
                        .header("Authorization", "Bearer " + fullToken)
                        .param("q", "SELECT Id, Name FROM Account WHERE Industry='Technology'"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(1))
                .andExpect(jsonPath("$.records[0].Name").value("Tech Corp"));
    }

    @Test
    void soqlOrderByAndLimit() throws Exception {
        for (String name : new String[]{"Charlie", "Alpha", "Bravo"}) {
            mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                            .header("Authorization", "Bearer " + fullToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"Name\":\"" + name + "\"}"))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/services/data/v60.0/query")
                        .header("Authorization", "Bearer " + fullToken)
                        .param("q", "SELECT Id, Name FROM Account ORDER BY Name LIMIT 2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(2))
                .andExpect(jsonPath("$.records[0].Name").value("Alpha"))
                .andExpect(jsonPath("$.records[1].Name").value("Bravo"));
    }

    // === User sObject Lookup (SF CLI needs this) ===

    @Test
    void userSObjectLookup_fallsBackToUserStore() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/sobjects/User/" + userId)
                        .header("Authorization", "Bearer " + fullToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attributes.type").value("User"))
                .andExpect(jsonPath("$.Username").isNotEmpty())
                .andExpect(jsonPath("$.IsActive").value(true));
    }

    // === Unauthenticated access rejected ===

    @Test
    void unauthenticatedRequestRejected() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/sobjects/Account/describe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$[0].errorCode").value("INVALID_SESSION_ID"));
    }

    // === Create Lead and Opportunity ===

    @Test
    void createLeadAndOpportunity() throws Exception {
        mockMvc.perform(post("/services/data/v60.0/sobjects/Lead")
                        .header("Authorization", "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"FirstName\":\"Sara\",\"LastName\":\"Connor\",\"Company\":\"Cyberdyne\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/services/data/v60.0/sobjects/Opportunity")
                        .header("Authorization", "Bearer " + fullToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"Big Deal\",\"StageName\":\"Prospecting\",\"CloseDate\":\"2026-12-31\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/services/data/v60.0/query")
                        .header("Authorization", "Bearer " + fullToken)
                        .param("q", "SELECT Id, FirstName, LastName, Company FROM Lead"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").value(1))
                .andExpect(jsonPath("$.records[0].Company").value("Cyberdyne"));
    }
}
