package econest.dao;

import econest.model.EmailLog;
import econest.model.EmailStatus;
import econest.model.RecipientType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** SQL for the email_log table (every notification attempt is recorded). */
public class EmailLogDao extends BaseDao {

    private static final String SELECT =
            "SELECT l.log_id, l.recipient_email, l.recipient_type, l.subject, l.body, l.order_id, o.order_ref, "
            + "l.status, l.error_message, l.created_at "
            + "FROM email_log l LEFT JOIN orders o ON o.order_id = l.order_id ";

    private EmailLog map(ResultSet rs) throws SQLException {
        EmailLog log = new EmailLog();
        log.setId(rs.getInt("log_id"));
        log.setRecipientEmail(rs.getString("recipient_email"));
        log.setRecipientType(RecipientType.valueOf(rs.getString("recipient_type")));
        log.setSubject(rs.getString("subject"));
        log.setBody(rs.getString("body"));
        int orderId = rs.getInt("order_id");
        log.setOrderId(rs.wasNull() ? null : orderId);
        log.setOrderRef(rs.getString("order_ref"));
        log.setStatus(EmailStatus.valueOf(rs.getString("status")));
        log.setErrorMessage(rs.getString("error_message"));
        log.setCreatedAt(getDateTime(rs, "created_at"));
        return log;
    }

    public int insert(EmailLog log) {
        String error = log.getErrorMessage();
        if (error != null && error.length() > 250) {
            error = error.substring(0, 250);
        }
        return insert("INSERT INTO email_log (recipient_email, recipient_type, subject, body, order_id, status, "
                + "error_message, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                log.getRecipientEmail(), log.getRecipientType(), log.getSubject(), log.getBody(), log.getOrderId(),
                log.getStatus(), error, LocalDateTime.now().withNano(0));
    }

    public List<EmailLog> findAll() {
        return query(SELECT + "ORDER BY l.created_at DESC, l.log_id DESC", this::map);
    }

    public List<EmailLog> findByStatus(EmailStatus status) {
        return query(SELECT + "WHERE l.status = ? ORDER BY l.created_at DESC, l.log_id DESC", this::map, status);
    }

    public List<EmailLog> findByOrder(int orderId) {
        return query(SELECT + "WHERE l.order_id = ? ORDER BY l.created_at DESC, l.log_id DESC", this::map, orderId);
    }

    public EmailLog findById(int id) {
        return queryOne(SELECT + "WHERE l.log_id = ?", this::map, id);
    }

    public int countByStatus(EmailStatus status) {
        return queryInt("SELECT COUNT(*) FROM email_log WHERE status = ?", status);
    }
}
