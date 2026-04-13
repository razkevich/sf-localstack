package co.razkevich.sflocalstack.controller;

import co.razkevich.sflocalstack.auth.store.UserStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OAuthAuthorizeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserStore userStore;

    @BeforeEach
    void setUp() {
        userStore.createUser("oauthuser", "oauth@test.dev", "secret123", co.razkevich.sflocalstack.auth.model.Role.USER);
    }

    // === Feature 0: Authorization Endpoint ===

    @Test
    void getAuthorize_withValidParams_returnsHtmlLoginForm() throws Exception {
        mockMvc.perform(get("/services/oauth2/authorize")
                        .param("response_type", "code")
                        .param("client_id", "PlatformCLI")
                        .param("redirect_uri", "http://localhost:1717/OauthRedirect")
                        .param("state", "abc123"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(containsString("value=\"abc123\"")))
                .andExpect(content().string(containsString("value=\"http://localhost:1717/OauthRedirect\"")));
    }

    @Test
    void getAuthorize_withoutResponseTypeCode_returns400() throws Exception {
        mockMvc.perform(get("/services/oauth2/authorize")
                        .param("client_id", "PlatformCLI")
                        .param("redirect_uri", "http://localhost:1717/OauthRedirect"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("unsupported_response_type"));
    }

    @Test
    void postAuthorize_withValidCredentials_redirectsWithCode() throws Exception {
        mockMvc.perform(post("/services/oauth2/authorize")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "oauthuser")
                        .param("password", "secret123")
                        .param("response_type", "code")
                        .param("client_id", "PlatformCLI")
                        .param("redirect_uri", "http://localhost:1717/OauthRedirect")
                        .param("state", "abc123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("http://localhost:1717/OauthRedirect?code=")))
                .andExpect(header().string("Location", containsString("state=abc123")));
    }

    @Test
    void postAuthorize_withInvalidCredentials_returnsHtmlWithError() throws Exception {
        mockMvc.perform(post("/services/oauth2/authorize")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "oauthuser")
                        .param("password", "wrongpassword")
                        .param("response_type", "code")
                        .param("client_id", "PlatformCLI")
                        .param("redirect_uri", "http://localhost:1717/OauthRedirect")
                        .param("state", "abc123"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Invalid username or password")));
    }

    // === Feature 1: Authorization Code Token Exchange ===

    @Test
    void tokenExchange_withValidCode_returnsJwt() throws Exception {
        // Generate code via authorize flow
        MvcResult authorizeResult = mockMvc.perform(post("/services/oauth2/authorize")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "oauthuser")
                        .param("password", "secret123")
                        .param("response_type", "code")
                        .param("client_id", "PlatformCLI")
                        .param("redirect_uri", "http://localhost:1717/OauthRedirect")
                        .param("state", "test"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = authorizeResult.getResponse().getHeader("Location");
        String code = extractParam(location, "code");

        // Exchange code for token
        mockMvc.perform(post("/services/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", "http://localhost:1717/OauthRedirect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.access_token", startsWith("00D")))
                .andExpect(jsonPath("$.instance_url").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"));
    }

    @Test
    void tokenExchange_withReplayedCode_returns400() throws Exception {
        MvcResult authorizeResult = mockMvc.perform(post("/services/oauth2/authorize")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "oauthuser")
                        .param("password", "secret123")
                        .param("response_type", "code")
                        .param("client_id", "PlatformCLI")
                        .param("redirect_uri", "http://localhost:1717/OauthRedirect")
                        .param("state", "test"))
                .andReturn();

        String code = extractParam(authorizeResult.getResponse().getHeader("Location"), "code");

        // First exchange succeeds
        mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", "http://localhost:1717/OauthRedirect"))
                .andExpect(status().isOk());

        // Second exchange fails (single-use)
        mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", "http://localhost:1717/OauthRedirect"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    @Test
    void tokenExchange_withMismatchedRedirectUri_returns400() throws Exception {
        MvcResult authorizeResult = mockMvc.perform(post("/services/oauth2/authorize")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "oauthuser")
                        .param("password", "secret123")
                        .param("response_type", "code")
                        .param("client_id", "PlatformCLI")
                        .param("redirect_uri", "http://localhost:1717/OauthRedirect")
                        .param("state", "test"))
                .andReturn();

        String code = extractParam(authorizeResult.getResponse().getHeader("Location"), "code");

        mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", "http://WRONG:9999/callback"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    @Test
    void tokenExchange_withMissingCode_returns400() throws Exception {
        mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "authorization_code")
                        .param("redirect_uri", "http://localhost:1717/OauthRedirect"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
    }

    @Test
    void fullFlow_authorizeToToken_producesWorkingJwt() throws Exception {
        // Step 1: Authorize
        MvcResult authorizeResult = mockMvc.perform(post("/services/oauth2/authorize")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "oauthuser")
                        .param("password", "secret123")
                        .param("response_type", "code")
                        .param("client_id", "PlatformCLI")
                        .param("redirect_uri", "http://localhost:1717/OauthRedirect")
                        .param("state", "fullflow"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String code = extractParam(authorizeResult.getResponse().getHeader("Location"), "code");

        // Step 2: Exchange code for token
        MvcResult tokenResult = mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", "http://localhost:1717/OauthRedirect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andReturn();

        String tokenJson = tokenResult.getResponse().getContentAsString();
        String accessToken = com.jayway.jsonpath.JsonPath.read(tokenJson, "$.access_token");

        // Step 3: Use token to access protected endpoint
        mockMvc.perform(get("/services/data/v60.0/sobjects/Account/describe")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Account"));
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
