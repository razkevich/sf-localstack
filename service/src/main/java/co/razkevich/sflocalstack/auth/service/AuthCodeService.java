package co.razkevich.sflocalstack.auth.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthCodeService {

    private static final long CODE_TTL_SECONDS = 300; // 5 minutes

    private final Map<String, AuthCodeEntry> codes = new ConcurrentHashMap<>();

    public String generateCode(String userId, String redirectUri) {
        cleanup();
        String code = UUID.randomUUID().toString();
        codes.put(code, new AuthCodeEntry(userId, redirectUri, Instant.now(), false));
        return code;
    }

    public Optional<String> exchangeCode(String code, String redirectUri) {
        AuthCodeEntry entry = codes.get(code);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.consumed) {
            return Optional.empty();
        }
        if (entry.createdAt.plusSeconds(CODE_TTL_SECONDS).isBefore(Instant.now())) {
            codes.remove(code);
            return Optional.empty();
        }
        if (!entry.redirectUri.equals(redirectUri)) {
            return Optional.empty();
        }
        // Mark consumed (single-use)
        entry.consumed = true;
        return Optional.of(entry.userId);
    }

    private void cleanup() {
        Instant cutoff = Instant.now().minusSeconds(CODE_TTL_SECONDS * 2);
        codes.entrySet().removeIf(e -> e.getValue().createdAt.isBefore(cutoff));
    }

    // Visible for testing
    int codeCount() {
        return codes.size();
    }

    private static class AuthCodeEntry {
        final String userId;
        final String redirectUri;
        final Instant createdAt;
        volatile boolean consumed;

        AuthCodeEntry(String userId, String redirectUri, Instant createdAt, boolean consumed) {
            this.userId = userId;
            this.redirectUri = redirectUri;
            this.createdAt = createdAt;
            this.consumed = consumed;
        }
    }
}
