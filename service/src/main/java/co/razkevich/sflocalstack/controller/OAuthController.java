package co.razkevich.sflocalstack.controller;

import co.razkevich.sflocalstack.auth.model.User;
import co.razkevich.sflocalstack.auth.service.AuthCodeService;
import co.razkevich.sflocalstack.auth.service.JwtService;
import co.razkevich.sflocalstack.auth.store.UserStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class OAuthController {

    private final UserStore userStore;
    private final JwtService jwtService;
    private final AuthCodeService authCodeService;

    public OAuthController(UserStore userStore, JwtService jwtService, AuthCodeService authCodeService) {
        this.userStore = userStore;
        this.jwtService = jwtService;
        this.authCodeService = authCodeService;
    }

    // === OAuth2 Authorization Code Flow ===

    @GetMapping("/services/oauth2/authorize")
    public ResponseEntity<?> authorize(
            @RequestParam(value = "response_type", required = false) String responseType,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "state", required = false, defaultValue = "") String state) {

        if (!"code".equals(responseType)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "unsupported_response_type",
                    "error_description", "Only response_type=code is supported"));
        }

        String html = renderLoginPage(clientId, redirectUri, state, null);
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    @PostMapping("/services/oauth2/authorize")
    public ResponseEntity<?> authorizeSubmit(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "response_type", required = false) String responseType,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "state", required = false, defaultValue = "") String state) {

        if (username == null || password == null || !userStore.validateCredentials(username, password)) {
            String html = renderLoginPage(clientId, redirectUri, state, "Invalid username or password");
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        }

        User user = userStore.findByUsername(username).orElseThrow();
        String code = authCodeService.generateCode(user.getId(), redirectUri);

        String location = redirectUri + "?code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);
        if (state != null && !state.isEmpty()) {
            location += "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location)
                .build();
    }

    // === Token Endpoint ===

    @PostMapping("/services/oauth2/token")
    public ResponseEntity<Map<String, Object>> token(
            @RequestParam(value = "grant_type", required = false) String grantType,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            HttpServletRequest request) {

        String instanceUrl = resolveInstanceUrl(request);

        // Authorization code grant
        if ("authorization_code".equals(grantType)) {
            if (code == null || code.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "invalid_request",
                        "error_description", "code is required"));
            }
            Optional<String> userId = authCodeService.exchangeCode(code, redirectUri != null ? redirectUri : "");
            if (userId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "invalid_grant",
                        "error_description", "authorization code is invalid, expired, or already used"));
            }
            User user = userStore.findById(userId.get()).orElseThrow();
            return ResponseEntity.ok(buildTokenResponse(user, instanceUrl));
        }

        // Password grant
        if ("password".equals(grantType) && username != null && password != null && userStore.hasUsers()) {
            if (!userStore.validateCredentials(username, password)) {
                return ResponseEntity.status(400).body(Map.of(
                        "error", "invalid_grant",
                        "error_description", "authentication failure"));
            }
            User user = userStore.findByUsername(username).orElseThrow();
            user.setLastLoginAt(Instant.now());
            return ResponseEntity.ok(buildTokenResponse(user, instanceUrl));
        }

        // Fallback: fake token (no users registered)
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", "00D000000000001!FAKE_ACCESS_TOKEN");
        response.put("instance_url", instanceUrl);
        response.put("id", instanceUrl + "/id/00D000000000001AAA/005000000000001AAA");
        response.put("token_type", "Bearer");
        response.put("issued_at", String.valueOf(System.currentTimeMillis()));
        response.put("signature", "fake");
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> buildTokenResponse(User user, String instanceUrl) {
        String accessToken = jwtService.generateAccessToken(user);
        Map<String, Object> response = new LinkedHashMap<>();
        String orgId = user.getOrgId() != null ? user.getOrgId() : "00D000000000001AAA";
        response.put("access_token", orgId + "!" + accessToken);
        response.put("instance_url", instanceUrl);
        response.put("id", instanceUrl + "/id/" + orgId + "/" + user.getId());
        response.put("token_type", "Bearer");
        response.put("issued_at", String.valueOf(System.currentTimeMillis()));
        response.put("signature", "jwt");
        return response;
    }

    private String resolveInstanceUrl(HttpServletRequest request) {
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null) scheme = request.getScheme();
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null) host = request.getHeader("Host");
        if (host == null) host = request.getServerName() + ":" + request.getServerPort();
        if (host.endsWith(":80") || host.endsWith(":443")) {
            host = host.replaceAll(":(80|443)$", "");
        }
        return scheme + "://" + host;
    }

    // === Userinfo & Identity ===

    @GetMapping("/services/oauth2/userinfo")
    public ResponseEntity<Map<String, Object>> userinfo(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Optional<User> user = extractUserFromAuth(authHeader);
        if (user.isPresent()) {
            return ResponseEntity.ok(buildUserInfo(user.get()));
        }
        return ResponseEntity.ok(Map.of(
                "user_id", "005000000000001AAA",
                "organization_id", "00D000000000001AAA",
                "username", "admin@sf-localstack.dev",
                "display_name", "SF LocalStack Admin"));
    }

    @GetMapping("/id/{orgId}/{userId}")
    public ResponseEntity<Map<String, Object>> identity(
            @PathVariable String orgId,
            @PathVariable String userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Optional<User> user = userStore.findById(userId);
        if (user.isEmpty()) {
            user = extractUserFromAuth(authHeader);
        }
        if (user.isPresent()) {
            return ResponseEntity.ok(buildUserInfo(user.get()));
        }
        return ResponseEntity.ok(Map.of(
                "user_id", userId,
                "organization_id", orgId,
                "username", "admin@sf-localstack.dev",
                "display_name", "SF LocalStack Admin"));
    }

    private Optional<User> extractUserFromAuth(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ") && userStore.hasUsers()) {
            String token = authHeader.substring(7);
            if (token.contains("!")) {
                token = token.substring(token.indexOf('!') + 1);
            }
            try {
                String userId = jwtService.extractUserId(token);
                return userStore.findById(userId);
            } catch (Exception ignored) {
            }
        }
        return Optional.empty();
    }

    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("user_id", user.getId());
        info.put("organization_id", user.getOrgId() != null ? user.getOrgId() : "00D000000000001AAA");
        info.put("preferred_username", user.getUsername());
        info.put("username", user.getUsername());
        info.put("display_name", user.getUsername());
        info.put("email", user.getEmail() != null ? user.getEmail() : user.getUsername() + "@sf-localstack.dev");
        return info;
    }

    // === Login Page HTML ===

    private String renderLoginPage(String clientId, String redirectUri, String state, String error) {
        String errorHtml = error != null
                ? "<div style=\"background:#fef0ef;border:1px solid #c23934;color:#c23934;padding:12px 16px;border-radius:4px;margin-bottom:16px;font-size:14px\">" + escapeHtml(error) + "</div>"
                : "";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Log In | SF LocalStack</title>
                  <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { font-family: 'Salesforce Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f4f6f9; display: flex; align-items: center; justify-content: center; min-height: 100vh; }
                    .container { width: 100%%; max-width: 400px; padding: 24px; }
                    .card { background: #fff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); padding: 32px; }
                    h1 { color: #032d60; font-size: 24px; font-weight: 700; text-align: center; margin-bottom: 8px; }
                    .subtitle { color: #706e6b; font-size: 14px; text-align: center; margin-bottom: 24px; }
                    .field { margin-bottom: 16px; }
                    label { display: block; font-size: 13px; font-weight: 600; color: #3e3e3c; margin-bottom: 4px; }
                    input[type="text"], input[type="password"] { width: 100%%; padding: 10px 12px; border: 1px solid #dddbda; border-radius: 4px; font-size: 14px; color: #080707; outline: none; transition: border-color 0.15s; }
                    input:focus { border-color: #0070d2; box-shadow: 0 0 0 1px #0070d2; }
                    .btn { width: 100%%; padding: 12px; background: #0070d2; color: #fff; border: none; border-radius: 4px; font-size: 15px; font-weight: 600; cursor: pointer; transition: background 0.15s; }
                    .btn:hover { background: #005fb2; }
                    .logo { text-align: center; margin-bottom: 24px; font-size: 20px; font-weight: 700; color: #0070d2; }
                  </style>
                </head>
                <body>
                  <div class="container">
                    <div class="card">
                      <div class="logo">&#9729; SF LocalStack</div>
                      <h1>Log In</h1>
                      <p class="subtitle">Sign in to authorize this application</p>
                      %s
                      <form method="POST" action="/services/oauth2/authorize">
                        <input type="hidden" name="response_type" value="code">
                        <input type="hidden" name="client_id" value="%s">
                        <input type="hidden" name="redirect_uri" value="%s">
                        <input type="hidden" name="state" value="%s">
                        <div class="field">
                          <label for="username">Username</label>
                          <input type="text" id="username" name="username" required autocomplete="username">
                        </div>
                        <div class="field">
                          <label for="password">Password</label>
                          <input type="password" id="password" name="password" required autocomplete="current-password">
                        </div>
                        <button type="submit" class="btn">Log In</button>
                      </form>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(errorHtml,
                escapeHtml(clientId != null ? clientId : ""),
                escapeHtml(redirectUri != null ? redirectUri : ""),
                escapeHtml(state != null ? state : ""));
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
