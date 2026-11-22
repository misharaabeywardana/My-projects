package econest.dao;

import econest.model.EmailTemplate;
import java.time.LocalDateTime;

/** SQL for the email_templates table. */
public class EmailTemplateDao extends BaseDao {

    public EmailTemplate find(String key) {
        return queryOne("SELECT template_key, subject, body FROM email_templates WHERE template_key = ?",
                rs -> new EmailTemplate(rs.getString("template_key"), rs.getString("subject"), rs.getString("body")),
                key);
    }

    /** Updates the template, or inserts it if it does not exist yet. */
    public void save(EmailTemplate t) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        int rows = update("UPDATE email_templates SET subject = ?, body = ?, updated_at = ? WHERE template_key = ?",
                t.getSubject(), t.getBody(), now, t.getKey());
        if (rows == 0) {
            update("INSERT INTO email_templates (template_key, subject, body, updated_at) VALUES (?, ?, ?, ?)",
                    t.getKey(), t.getSubject(), t.getBody(), now);
        }
    }
}
