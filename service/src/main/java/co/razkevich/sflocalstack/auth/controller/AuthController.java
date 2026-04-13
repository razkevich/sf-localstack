package co.razkevich.sflocalstack.auth.controller;

import co.razkevich.sflocalstack.auth.model.Role;
import co.razkevich.sflocalstack.auth.model.User;
import co.razkevich.sflocalstack.auth.service.JwtService;
import co.razkevich.sflocalstack.auth.store.UserStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserStore userStore;
    private final JwtService jwtService;

    public AuthController(UserStore userStore, JwtService jwtService) {
        this.userStore = userStore;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body,
                                      @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");

        if (username == null || email == null || password == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Bad Request", "message", "username, email, and password are required"));
        }

        if (userStore.findByUsername(username).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Conflict", "message", "Username already exists"));
        }

        // First user becomes ADMIN, all others are USER. Open registration — no auth required.
        boolean firstUser = !userStore.hasUsers();
        Role role = firstUser ? Role.ADMIN : Role.USER;

        // Generate unique org ID per user
        String orgId = firstUser
                ? "00D000000000001AAA"
                : "00D" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 15).toUpperCase();

        User user = userStore.createUser(username, email, password, role, orgId);
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("user", toUserResponse(user));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Bad Request", "message", "username and password are required"));
        }

        if (!userStore.validateCredentials(username, password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Invalid username or password"));
        }

        User user = userStore.findByUsername(username).orElseThrow();
        user.setLastLoginAt(Instant.now());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("user", toUserResponse(user));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Bad Request", "message", "refreshToken is required"));
        }

        try {
            var claims = jwtService.validateToken(refreshToken);
            if (!"refresh".equals(claims.get("type", String.class))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized", "message", "Not a refresh token"));
            }

            String userId = claims.get("userId", String.class);
            Optional<User> userOpt = userStore.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized", "message", "User not found"));
            }

            String newAccessToken = jwtService.generateAccessToken(userOpt.get());
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Invalid refresh token"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Bearer token required"));
        }

        try {
            String userId = jwtService.extractUserId(authHeader.substring(7));
            Optional<User> userOpt = userStore.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized", "message", "User not found"));
            }
            return ResponseEntity.ok(toUserResponse(userOpt.get()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Invalid token"));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Bearer token required"));
        }

        try {
            String role = jwtService.extractRole(authHeader.substring(7));
            if (!"ADMIN".equals(role)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Forbidden", "message", "Admin role required"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Invalid token"));
        }

        List<Map<String, Object>> users = userStore.listUsers().stream()
                .map(this::toUserResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id,
                                        @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Bearer token required"));
        }

        try {
            String callerRole = jwtService.extractRole(authHeader.substring(7));
            String callerId = jwtService.extractUserId(authHeader.substring(7));

            if (!"ADMIN".equals(callerRole)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Forbidden", "message", "Admin role required"));
            }
            if (callerId.equals(id)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Bad Request", "message", "Cannot delete yourself"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Invalid token"));
        }

        if (userStore.deleteUser(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private Map<String, Object> toUserResponse(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("role", user.getRole().name());
        map.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        map.put("lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null);
        return map;
    }
}
