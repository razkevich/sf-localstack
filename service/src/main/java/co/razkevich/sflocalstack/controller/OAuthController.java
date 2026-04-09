package co.razkevich.sflocalstack.controller;

import co.razkevich.sflocalstack.auth.model.User;
import co.razkevich.sflocalstack.auth.service.JwtService;
import co.razkevich.sflocalstack.auth.store.UserStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class OAuthController {

    private final UserStore userStore;
    private final JwtService jwtService;

    public OAuthController(UserStore userStore, JwtService jwtService) {
        this.userStore = userStore;
        this.jwtService = jwtService;
    }

    @PostMapping("/services/oauth2/token")
    public ResponseEntity<Map<String, Object>> token(
            @RequestParam(value = "grant_type", required = false) String grantType,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "password", required = false) String password) {

        if ("password".equals(grantType) && username != null && password != null && userStore.hasUsers()) {
            if (!userStore.validateCredentials(username, password)) {
                return ResponseEntity.status(400).body(Map.of(
                        "error", "invalid_grant",
                        "error_description", "authentication failure"
                ));
            }

            User user = userStore.findByUsername(username).orElseThrow();
            user.setLastLoginAt(Instant.now());
            String accessToken = jwtService.generateAccessToken(user);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("access_token", accessToken);
            response.put("instance_url", "http://localhost:8080");
            response.put("id", "https://login.salesforce.com/id/00D000000000001AAA/" + user.getId());
            response.put("token_type", "Bearer");
            response.put("issued_at", String.valueOf(System.currentTimeMillis()));
            response.put("signature", "jwt");
            return ResponseEntity.ok(response);
        }

        // Fallback: return a fake token (for backwards compatibility when no users are registered)
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", "00D000000000001!FAKE_ACCESS_TOKEN");
        response.put("instance_url", "http://localhost:8080");
        response.put("id", "https://login.salesforce.com/id/00D000000000001AAA/005000000000001AAA");
        response.put("token_type", "Bearer");
        response.put("issued_at", String.valueOf(System.currentTimeMillis()));
        response.put("signature", "fake");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/services/oauth2/userinfo")
    public ResponseEntity<Map<String, Object>> userinfo(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader != null && authHeader.startsWith("Bearer ") && userStore.hasUsers()) {
            String token = authHeader.substring(7);
            try {
                String userId = jwtService.extractUserId(token);
                String username = jwtService.extractUsername(token);
                Optional<User> userOpt = userStore.findById(userId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("user_id", user.getId());
                    info.put("organization_id", "00D000000000001AAA");
                    info.put("username", user.getUsername());
                    info.put("display_name", user.getUsername());
                    info.put("email", user.getEmail());
                    return ResponseEntity.ok(info);
                }
            } catch (Exception ignored) {
                // Fall through to default response
            }
        }

        // Fallback: return fake user info
        return ResponseEntity.ok(Map.of(
                "user_id", "005000000000001AAA",
                "organization_id", "00D000000000001AAA",
                "username", "admin@sf-localstack.dev",
                "display_name", "SF LocalStack Admin"
        ));
    }
}
