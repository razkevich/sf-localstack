package co.razkevich.sflocalstack.auth.store;

import co.razkevich.sflocalstack.auth.model.Role;
import co.razkevich.sflocalstack.auth.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Profile("!test")
public class FileBasedUserStore implements UserStore {

    private static final Logger log = LoggerFactory.getLogger(FileBasedUserStore.class);
    private static final Path USERS_FILE = Path.of("data/users.json");

    private final ObjectMapper mapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public FileBasedUserStore() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.passwordEncoder = new BCryptPasswordEncoder();
        ensureFileExists();
    }

    private void ensureFileExists() {
        try {
            if (!Files.exists(USERS_FILE)) {
                Files.createDirectories(USERS_FILE.getParent());
                Files.writeString(USERS_FILE, "[]");
            }
        } catch (IOException e) {
            log.error("Failed to create users file", e);
        }
    }

    private List<User> readUsers() {
        try {
            String content = Files.readString(USERS_FILE);
            return mapper.readValue(content, new TypeReference<List<User>>() {});
        } catch (IOException e) {
            log.error("Failed to read users file", e);
            return new ArrayList<>();
        }
    }

    private synchronized void writeUsers(List<User> users) {
        try {
            Files.writeString(USERS_FILE, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(users));
        } catch (IOException e) {
            log.error("Failed to write users file", e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return readUsers().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }

    @Override
    public Optional<User> findById(String id) {
        return readUsers().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();
    }

    @Override
    public synchronized User createUser(String username, String email, String password, Role role) {
        List<User> users = readUsers();
        User user = new User(
                UUID.randomUUID().toString(),
                username,
                email,
                passwordEncoder.encode(password),
                role,
                Instant.now(),
                null
        );
        users.add(user);
        writeUsers(users);
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
        return readUsers();
    }

    @Override
    public synchronized boolean deleteUser(String id) {
        List<User> users = readUsers();
        boolean removed = users.removeIf(u -> u.getId().equals(id));
        if (removed) {
            writeUsers(users);
        }
        return removed;
    }

    @Override
    public boolean hasUsers() {
        return !readUsers().isEmpty();
    }
}
