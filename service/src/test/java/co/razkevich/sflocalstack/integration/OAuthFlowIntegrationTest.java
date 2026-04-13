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

import java.util.Base64;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests both OAuth2 flows (password grant + authorization code) end-to-end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OAuthFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private String username;

    @BeforeEach
    void setUp() throws Exception {
        username = "oauth_" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"email\":\"" + username + "@test.dev\",\"password\":\"secret\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void passwordGrant_returnsSalesforceCompatibleResponse() throws Exception {
        MvcResult result = mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "password")
                        .param("username", username)
                        .param("password", "secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.access_token", containsString("!")))
                .andExpect(jsonPath("$.instance_url").isNotEmpty())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.issued_at").isNotEmpty())
                .andExpect(jsonPath("$.signature").value("jwt"))
                .andReturn();

        // Token should work for protected endpoint
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.access_token");
        mockMvc.perform(get("/services/data/v60.0/sobjects/Account/describe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void authorizationCodeFlow_fullEndToEnd() throws Exception {
        // Step 1: GET authorize — returns HTML login page
        mockMvc.perform(get("/services/oauth2/authorize")
                        .param("response_type", "code")
                        .param("client_id", "TestClient")
                        .param("redirect_uri", "http://localhost:1717/OauthRedirect")
                        .param("state", "teststate"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("name=\"username\"")));

        // Step 2: POST authorize — submit credentials, get redirect with code
        MvcResult authResult = mockMvc.perform(post("/services/oauth2/authorize")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", username)
                        .param("password", "secret")
                        .param("response_type", "code")
                        .param("client_id", "TestClient")
                        .param("redirect_uri", "http://localhost:1717/OauthRedirect")
                        .param("state", "teststate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("code=")))
                .andExpect(header().string("Location", containsString("state=teststate")))
                .andReturn();

        String location = authResult.getResponse().getHeader("Location");
        String code = extractParam(location, "code");

        // Step 3: Exchange code for token
        MvcResult tokenResult = mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", "http://localhost:1717/OauthRedirect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andReturn();

        // Step 4: Use the token
        String token = JsonPath.read(tokenResult.getResponse().getContentAsString(), "$.access_token");
        mockMvc.perform(get("/services/data/v60.0/sobjects/Account/describe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Account"));
    }

    @Test
    void userInfoEndpoint_returnsUserDetails() throws Exception {
        MvcResult oauth = mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "password")
                        .param("username", username)
                        .param("password", "secret"))
                .andReturn();
        String token = JsonPath.read(oauth.getResponse().getContentAsString(), "$.access_token");

        mockMvc.perform(get("/services/oauth2/userinfo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").isNotEmpty())
                .andExpect(jsonPath("$.organization_id").isNotEmpty())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.display_name").value(username));
    }

    @Test
    void identityEndpoint_returnsUserInfo() throws Exception {
        MvcResult oauth = mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "password")
                        .param("username", username)
                        .param("password", "secret"))
                .andReturn();
        String token = JsonPath.read(oauth.getResponse().getContentAsString(), "$.access_token");
        String idUrl = JsonPath.read(oauth.getResponse().getContentAsString(), "$.id");
        // Extract path from id URL (e.g., "http://localhost/id/00D.../userId")
        String idPath = idUrl.replaceFirst("https?://[^/]+", "");

        mockMvc.perform(get(idPath)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.organization_id").isNotEmpty());
    }

    @Test
    void tokenContainsOrgIdClaim() throws Exception {
        MvcResult oauth = mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "password")
                        .param("username", username)
                        .param("password", "secret"))
                .andReturn();
        String fullToken = JsonPath.read(oauth.getResponse().getContentAsString(), "$.access_token");

        // Extract JWT part (after the !)
        String jwt = fullToken.substring(fullToken.indexOf('!') + 1);
        String payload = jwt.split("\\.")[1];
        // Pad base64
        while (payload.length() % 4 != 0) payload += "=";
        String decoded = new String(Base64.getUrlDecoder().decode(payload));

        assertTrue(decoded.contains("\"orgId\""), "JWT should contain orgId claim");
        assertTrue(decoded.contains("\"userId\""), "JWT should contain userId claim");
        assertTrue(decoded.contains("\"username\""), "JWT should contain username claim");
    }

    @Test
    void invalidCredentials_rejected() throws Exception {
        mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "password")
                        .param("username", username)
                        .param("password", "WRONG"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    @Test
    void authCodeReplay_rejected() throws Exception {
        MvcResult authResult = mockMvc.perform(post("/services/oauth2/authorize")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", username).param("password", "secret")
                        .param("response_type", "code").param("client_id", "X")
                        .param("redirect_uri", "http://localhost:1717/OauthRedirect")
                        .param("state", "s"))
                .andReturn();
        String code = extractParam(authResult.getResponse().getHeader("Location"), "code");

        // First exchange succeeds
        mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", "http://localhost:1717/OauthRedirect"))
                .andExpect(status().isOk());

        // Replay rejected
        mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", "http://localhost:1717/OauthRedirect"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    @Test
    void openRegistration_anyoneCanRegister() throws Exception {
        String newUser = "open_" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + newUser + "\",\"email\":\"" + newUser + "@t.dev\",\"password\":\"pass\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value(newUser));
    }

    private String extractParam(String url, String paramName) {
        String query = url.substring(url.indexOf('?') + 1);
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv[0].equals(paramName)) {
                return java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        throw new IllegalArgumentException("Param " + paramName + " not found in " + url);
    }
}
