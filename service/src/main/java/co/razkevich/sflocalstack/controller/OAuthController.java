package co.razkevich.sflocalstack.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class OAuthController {

    @PostMapping("/services/oauth2/token")
    public ResponseEntity<Map<String, Object>> token() {
        return ResponseEntity.ok(Map.of(
                "access_token", "00D000000000001!FAKE_ACCESS_TOKEN",
                "instance_url", "http://localhost:8080",
                "token_type", "Bearer",
                "issued_at", System.currentTimeMillis()
        ));
    }

    @GetMapping("/services/oauth2/userinfo")
    public ResponseEntity<Map<String, Object>> userinfo() {
        return ResponseEntity.ok(Map.of(
                "user_id", "005000000000001AAA",
                "organization_id", "00D000000000001AAA",
                "username", "admin@sf-localstack.dev",
                "display_name", "SF LocalStack Admin"
        ));
    }
}
