package econest.model;

/** Editable subject/body of an automatic e-mail (additional feature - Rakshana). */
public class EmailTemplate {

    public static final String ORDER_DISPATCHED = "ORDER_DISPATCHED";
    public static final String DELIVERY_ASSIGNED = "DELIVERY_ASSIGNED";

    private String key;
    private String subject;
    private String body;

    public EmailTemplate() {
    }

    public EmailTemplate(String key, String subject, String body) {
        this.key = key;
        this.subject = subject;
        this.body = body;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
