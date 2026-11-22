package econest.dao;

import econest.model.Role;
import econest.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** SQL for the users table (login and admin user management). */
public class UserDao extends BaseDao implements CrudDao<User> {

    private static final String SELECT =
            "SELECT user_id, username, password_hash, salt, full_name, role, active FROM users ";

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setSalt(rs.getString("salt"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setActive(rs.getInt("active") == 1);
        return u;
    }

    @Override
    public List<User> findAll() {
        return query(SELECT + "ORDER BY username", this::map);
    }

    @Override
    public User findById(int id) {
        return queryOne(SELECT + "WHERE user_id = ?", this::map, id);
    }

    public User findByUsername(String username) {
        return queryOne(SELECT + "WHERE LOWER(username) = LOWER(?)", this::map, username);
    }

    @Override
    public int insert(User u) {
        return insert("INSERT INTO users (username, password_hash, salt, full_name, role, active) "
                + "VALUES (?, ?, ?, ?, ?, ?)",
                u.getUsername(), u.getPasswordHash(), u.getSalt(), u.getFullName(), u.getRole(), u.isActive());
    }

    /** Updates name, role and active flag (password is changed separately). */
    @Override
    public void update(User u) {
        update("UPDATE users SET username = ?, full_name = ?, role = ?, active = ? WHERE user_id = ?",
                u.getUsername(), u.getFullName(), u.getRole(), u.isActive(), u.getId());
    }

    public void updatePassword(int userId, String hash, String salt) {
        update("UPDATE users SET password_hash = ?, salt = ? WHERE user_id = ?", hash, salt, userId);
    }

    @Override
    public void delete(int id) {
        update("DELETE FROM users WHERE user_id = ?", id);
    }

    public int countActiveAdmins() {
        return queryInt("SELECT COUNT(*) FROM users WHERE role = 'ADMIN' AND active = 1");
    }
}
