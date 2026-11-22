package econest.service;

import econest.dao.UserDao;
import econest.model.Role;
import econest.model.User;
import econest.util.PasswordUtil;
import econest.util.Session;
import econest.util.Validator;
import java.util.List;

/** Login and user management for the admin panel. */
public class UserService {

    public static final int MIN_PASSWORD_LENGTH = 6;

    private final UserDao userDao;

    public UserService() {
        this(new UserDao());
    }

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    /** Checks the username and password; on success the user is stored in the Session. */
    public User login(String username, String password) {
        if (Validator.isBlank(username) || password == null || password.isEmpty()) {
            throw new ValidationException("Please enter your username and password.");
        }
        User user = userDao.findByUsername(username.trim());
        if (user == null || !PasswordUtil.matches(password, user.getSalt(), user.getPasswordHash())) {
            throw new ValidationException("Incorrect username or password.");
        }
        if (!user.isActive()) {
            throw new ValidationException("This account has been disabled. Please contact an administrator.");
        }
        Session.setCurrentUser(user);
        return user;
    }

    public List<User> getAll() {
        requireAdmin();
        return userDao.findAll();
    }

    private void validate(User u) {
        if (!Validator.isValidUsername(u.getUsername())) {
            throw new ValidationException("Username must be 3-50 letters, digits, dots or underscores.");
        }
        if (Validator.isBlank(u.getFullName())) {
            throw new ValidationException("Full name is required.");
        }
        if (u.getRole() == null) {
            throw new ValidationException("Please choose a role.");
        }
        User sameName = userDao.findByUsername(u.getUsername());
        if (sameName != null && sameName.getId() != u.getId()) {
            throw new ValidationException("The username \"" + u.getUsername() + "\" is already taken.");
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
    }

    public int createUser(User u, String password) {
        requireAdmin();
        u.setUsername(u.getUsername() == null ? null : u.getUsername().trim());
        u.setFullName(u.getFullName() == null ? null : u.getFullName().trim());
        validate(u);
        validatePassword(password);
        String salt = PasswordUtil.newSalt();
        u.setSalt(salt);
        u.setPasswordHash(PasswordUtil.hash(password, salt));
        int id = userDao.insert(u);
        u.setId(id);
        return id;
    }

    public void updateUser(User u) {
        requireAdmin();
        u.setUsername(u.getUsername() == null ? null : u.getUsername().trim());
        u.setFullName(u.getFullName() == null ? null : u.getFullName().trim());
        validate(u);
        User current = userDao.findById(u.getId());
        if (current == null) {
            throw new ValidationException("This user no longer exists.");
        }
        boolean wasActiveAdmin = current.getRole() == Role.ADMIN && current.isActive();
        boolean staysActiveAdmin = u.getRole() == Role.ADMIN && u.isActive();
        if (wasActiveAdmin && !staysActiveAdmin && userDao.countActiveAdmins() <= 1) {
            throw new ValidationException("At least one active administrator is required.");
        }
        if (isSelf(u.getId()) && !u.isActive()) {
            throw new ValidationException("You cannot disable your own account.");
        }
        userDao.update(u);
    }

    public void resetPassword(int userId, String newPassword) {
        requireAdmin();
        validatePassword(newPassword);
        String salt = PasswordUtil.newSalt();
        userDao.updatePassword(userId, PasswordUtil.hash(newPassword, salt), salt);
    }

    /** Any logged-in user can change their own password. */
    public void changeOwnPassword(String oldPassword, String newPassword) {
        User me = Session.getCurrentUser();
        if (me == null) {
            throw new ValidationException("Not logged in.");
        }
        User fresh = userDao.findById(me.getId());
        if (fresh == null || !PasswordUtil.matches(oldPassword, fresh.getSalt(), fresh.getPasswordHash())) {
            throw new ValidationException("Your current password is not correct.");
        }
        validatePassword(newPassword);
        String salt = PasswordUtil.newSalt();
        userDao.updatePassword(me.getId(), PasswordUtil.hash(newPassword, salt), salt);
    }

    public void deleteUser(int userId) {
        requireAdmin();
        if (isSelf(userId)) {
            throw new ValidationException("You cannot delete your own account.");
        }
        User u = userDao.findById(userId);
        if (u != null && u.getRole() == Role.ADMIN && u.isActive() && userDao.countActiveAdmins() <= 1) {
            throw new ValidationException("At least one active administrator is required.");
        }
        userDao.delete(userId);
    }

    private static boolean isSelf(int userId) {
        User me = Session.getCurrentUser();
        return me != null && me.getId() == userId;
    }

    private static void requireAdmin() {
        if (!Session.isAdmin()) {
            throw new ValidationException("Only administrators can manage users.");
        }
    }
}
