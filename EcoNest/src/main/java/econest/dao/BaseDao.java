package econest.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared JDBC code for all DAO classes (inheritance removes duplicated
 * try/catch/close code). Every query uses PreparedStatement parameters,
 * which protects against SQL injection.
 */
public abstract class BaseDao {

    /** Converts one row of a ResultSet into a Java object. */
    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    // ------------------------------------------------------------ queries

    protected <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection con = DBConnection.getConnection()) {
            return query(con, sql, mapper, params);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    protected <T> List<T> query(Connection con, String sql, RowMapper<T> mapper, Object... params) {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapper.map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /** Returns the first row, or null when nothing matches. */
    protected <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = query(sql, mapper, params);
        return list.isEmpty() ? null : list.get(0);
    }

    protected <T> T queryOne(Connection con, String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = query(con, sql, mapper, params);
        return list.isEmpty() ? null : list.get(0);
    }

    protected int queryInt(String sql, Object... params) {
        Integer value = queryOne(sql, rs -> rs.getInt(1), params);
        return value == null ? 0 : value;
    }

    // ------------------------------------------------------------ updates

    /** Runs INSERT/UPDATE/DELETE and returns the number of changed rows. */
    protected int update(String sql, Object... params) {
        try (Connection con = DBConnection.getConnection()) {
            return update(con, sql, params);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    protected int update(Connection con, String sql, Object... params) {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /** Runs an INSERT and returns the AUTO_INCREMENT id created by MySQL. */
    protected int insert(String sql, Object... params) {
        try (Connection con = DBConnection.getConnection()) {
            return insert(con, sql, params);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    protected int insert(Connection con, String sql, Object... params) {
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("No id was returned by the database");
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    // ------------------------------------------------------------ helpers

    /** Puts Java values into the ? placeholders, choosing the right JDBC setter. */
    protected static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            int index = i + 1;
            if (p == null) {
                ps.setNull(index, Types.NULL);
            } else if (p instanceof String) {
                ps.setString(index, (String) p);
            } else if (p instanceof Integer) {
                ps.setInt(index, (Integer) p);
            } else if (p instanceof BigDecimal) {
                ps.setBigDecimal(index, (BigDecimal) p);
            } else if (p instanceof Boolean) {
                ps.setInt(index, ((Boolean) p) ? 1 : 0);
            } else if (p instanceof Enum) {
                ps.setString(index, ((Enum<?>) p).name());
            } else if (p instanceof LocalDateTime || p instanceof LocalDate) {
                ps.setObject(index, p);
            } else {
                ps.setObject(index, p);
            }
        }
    }

    protected static LocalDateTime getDateTime(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDateTime.class);
    }

    protected static LocalDate getDate(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDate.class);
    }

    protected static BigDecimal getMoney(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? BigDecimal.ZERO : value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /** Converts MySQL error codes into friendly messages. */
    public static DataAccessException translate(SQLException e) {
        if (e.getErrorCode() == 1062) {
            return new DataAccessException("A record with the same unique value already exists.",
                    e, DataAccessException.Kind.DUPLICATE);
        }
        if (e.getErrorCode() == 1451) {
            return new DataAccessException("This record is used by other records and cannot be deleted.",
                    e, DataAccessException.Kind.IN_USE);
        }
        if (e.getErrorCode() == 1452) {
            return new DataAccessException("A related record does not exist (foreign key check failed).", e);
        }
        String state = e.getSQLState();
        if (state != null && state.startsWith("08")) {
            return new DataAccessException(DBConnection.explain(e), e, DataAccessException.Kind.CONNECTION);
        }
        return new DataAccessException("Database error: " + e.getMessage(), e);
    }
}
