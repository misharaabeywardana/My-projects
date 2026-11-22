package econest.dao;

import econest.util.ConfigFile;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * JDBC connection class. Every DAO asks this class for a connection.
 *
 * Settings are read from config/db.properties so the password is not in the
 * source code. XAMPP default: user "root" with an empty password.
 */
public final class DBConnection {

    public static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/econest_db";

    private static final ConfigFile CONFIG = new ConfigFile("db.properties", defaults());
    private static Properties settings;

    private DBConnection() {
    }

    private static Properties defaults() {
        Properties p = new Properties();
        p.setProperty("db.url", DEFAULT_URL);
        p.setProperty("db.user", "root");
        p.setProperty("db.password", "");
        return p;
    }

    private static synchronized Properties settings() {
        if (settings == null) {
            settings = CONFIG.load();
        }
        return settings;
    }

    public static String getUrl() {
        return settings().getProperty("db.url");
    }

    public static String getUser() {
        return settings().getProperty("db.user");
    }

    public static String getPassword() {
        return settings().getProperty("db.password", "");
    }

    public static String getConfigPath() {
        return CONFIG.getPath().toString();
    }

    /** Saves new settings to config/db.properties and uses them immediately. */
    public static synchronized void saveSettings(String url, String user, String password) throws IOException {
        Properties p = new Properties();
        p.setProperty("db.url", url.trim());
        p.setProperty("db.user", user.trim());
        p.setProperty("db.password", password);
        CONFIG.save(p, "EcoNest database settings");
        settings = p;
    }

    /** Opens a new connection. Always close it (use try-with-resources). */
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(getUrl(), getUser(), getPassword());
        } catch (SQLException e) {
            throw new DataAccessException(explain(e), e, DataAccessException.Kind.CONNECTION);
        }
    }

    /** Opens and closes a connection; returns a short description of the server. */
    public static String testConnection() {
        try (Connection con = getConnection()) {
            DatabaseMetaData meta = con.getMetaData();
            return "Connected to " + meta.getDatabaseProductName() + " "
                    + meta.getDatabaseProductVersion() + " (" + getUrl() + ")";
        } catch (SQLException e) {
            throw new DataAccessException(explain(e), e, DataAccessException.Kind.CONNECTION);
        }
    }

    /** Work that runs inside one database transaction. */
    @FunctionalInterface
    public interface TransactionWork<T> {
        T run(Connection con) throws SQLException;
    }

    /**
     * Runs several SQL statements as one unit: either all succeed (commit)
     * or none are saved (rollback). Used for orders, stock-in, deliveries.
     */
    public static <T> T inTransaction(TransactionWork<T> work) {
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try {
                T result = work.run(con);
                con.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                if (e instanceof SQLException) {
                    throw BaseDao.translate((SQLException) e);
                }
                throw (RuntimeException) e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw BaseDao.translate(e);
        }
    }

    /** Turns common connection problems into instructions a beginner can follow. */
    static String explain(SQLException e) {
        String msg = String.valueOf(e.getMessage());
        String lower = msg.toLowerCase();
        if (lower.contains("no suitable driver")) {
            return "The MySQL JDBC driver was not found.\n"
                    + "Add mysql-connector-j to the project (see the installation guide, step B).";
        }
        if (lower.contains("communications link failure") || lower.contains("connection refused")) {
            return "Cannot reach the MySQL server at " + getUrl() + ".\n"
                    + "Open the XAMPP Control Panel and click Start next to MySQL.";
        }
        if (lower.contains("unknown database")) {
            return "The database econest_db does not exist yet.\n"
                    + "Open phpMyAdmin and import database/schema.sql, then sample-data.sql.";
        }
        if (lower.contains("access denied")) {
            return "MySQL refused the username/password.\n"
                    + "Check db.user and db.password in " + getConfigPath();
        }
        return "Database error: " + msg;
    }
}
