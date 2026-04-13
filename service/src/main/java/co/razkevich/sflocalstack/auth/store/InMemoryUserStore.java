package co.razkevich.sflocalstack.auth.store;

import co.razkevich.sflocalstack.auth.model.Role;
import co.razkevich.sflocalstack.auth.model.User;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("test")
public class InMemoryUserStore implements UserStore {

    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Optional<User> findByUsername(String username) {
        return users.values().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public User createUser(String username, String email, String password, Role role) {
        return createUser(username, email, password, role, null);
    }

    @Override
    public User createUser(String username, String email, String password, Role role, String orgId) {
        User user = new User(
                UUID.randomUUID().toString(),
                username,
                email,
                passwordEncoder.encode(password),
                role,
                orgId,
                Instant.now(),
                null
        );
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public boolean validateCredentials(String username, String password) {
        return findByUsername(username)
                .map(u -> passwordEncoder.matches(password, u.getPasswordHash()))
                .orElse(false);
    }

    @Override
    public List<User> listUsers() {
        return new ArrayList<>(users.values());
    }

    @Override
    public boolean deleteUser(String id) {
        return users.remove(id) != null;
    }

    @Override
    public boolean hasUsers() {
        return !users.isEmpty();
    }
}
