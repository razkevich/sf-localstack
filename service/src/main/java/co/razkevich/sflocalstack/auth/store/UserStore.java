package co.razkevich.sflocalstack.auth.store;

import co.razkevich.sflocalstack.auth.model.Role;
import co.razkevich.sflocalstack.auth.model.User;

import java.util.List;
import java.util.Optional;

public interface UserStore {
    Optional<User> findByUsername(String username);
    Optional<User> findById(String id);
    User createUser(String username, String email, String password, Role role);
    default User createUser(String username, String email, String password, Role role, String orgId) {
        User user = createUser(username, email, password, role);
        user.setOrgId(orgId);
        return user;
    }
    boolean validateCredentials(String username, String password);
    List<User> listUsers();
    boolean deleteUser(String id);
    boolean hasUsers();
}
