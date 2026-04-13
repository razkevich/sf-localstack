package co.razkevich.sflocalstack.auth;

import co.razkevich.sflocalstack.auth.store.UserStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserStore userStore;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearUsers() {
        // Clear all users from InMemoryUserStore before each test
        userStore.listUsers().forEach(u -> userStore.deleteUser(u.getId()));
    }

    private String registerAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"email\":\"admin@test.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Map<String, Object> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return (String) body.get("accessToken");
    }

    @Test
    void registerFirstUserCreatesAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"email\":\"admin@test.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.username").value("admin"));
    }

    @Test
    void loginReturnsTokens() throws Exception {
        registerAdmin();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.username").value("admin"));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        registerAdmin();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void refreshReturnsNewAccessToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"email\":\"admin@test.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Map<String, Object> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        String refreshToken = (String) body.get("refreshToken");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void meReturnsUserInfo() throws Exception {
        String token = registerAdmin();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void protectedEndpointWithoutTokenReturns401WhenUsersExist() throws Exception {
        registerAdmin();

        mockMvc.perform(get("/services/data/v60.0/sobjects/Account/describe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$[0].errorCode").value("INVALID_SESSION_ID"));
    }

    @Test
    void protectedEndpointWithValidTokenReturns200() throws Exception {
        String token = registerAdmin();

        mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"Test\"}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/services/data/v60.0/sobjects/Account/describe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanListUsers() throws Exception {
        String token = registerAdmin();

        mockMvc.perform(get("/api/auth/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"));
    }

    @Test
    void adminCanDeleteOtherUser() throws Exception {
        String adminToken = registerAdmin();

        // Register a second user
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user1\",\"email\":\"user1@test.com\",\"password\":\"pass123\"}")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn();
        Map<String, Object> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> user = (Map<String, Object>) body.get("user");
        String userId = (String) user.get("id");

        mockMvc.perform(delete("/api/auth/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCannotDeleteSelf() throws Exception {
        String token = registerAdmin();

        // Get admin's user ID
        MvcResult meResult = mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> me = objectMapper.readValue(meResult.getResponse().getContentAsString(), Map.class);
        String adminId = (String) me.get("id");

        mockMvc.perform(delete("/api/auth/users/" + adminId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot delete yourself"));
    }

    @Test
    void userRoleCannotRegisterNewUsers() throws Exception {
        String adminToken = registerAdmin();

        // Register a regular user
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user1\",\"email\":\"user1@test.com\",\"password\":\"pass123\"}")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn();
        Map<String, Object> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        String userToken = (String) body.get("accessToken");

        // Open registration: any user can register (role is USER, not ADMIN)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user2\",\"email\":\"user2@test.com\",\"password\":\"pass456\"}")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated());
    }

    @Test
    void oauthTokenWithPasswordGrantReturnsRealJwt() throws Exception {
        registerAdmin();

        mockMvc.perform(post("/services/oauth2/token")
                        .param("grant_type", "password")
                        .param("username", "admin")
                        .param("password", "secret123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.instance_url").value("http://localhost"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.signature").value("jwt"));
    }

    @Test
    void noUsersRegisteredAllowsAllEndpoints() throws Exception {
        // No users registered - auth should be bypassed
        mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"No Auth Needed\"}"))
                .andExpect(status().isCreated());
    }
}
