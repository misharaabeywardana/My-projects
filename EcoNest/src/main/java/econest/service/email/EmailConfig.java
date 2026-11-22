package econest.service.email;

import econest.util.ConfigFile;
import java.io.IOException;
import java.util.Properties;

/**
 * E-mail settings stored in config/email.properties (never in the source
 * code, never committed to GitHub).
 */
public class EmailConfig {

    public enum Mode { SIMULATE, SMTP }

    public enum Security { STARTTLS, SSL, NONE }

    private static final ConfigFile FILE = new ConfigFile("email.properties", defaults());

    private Mode mode = Mode.SIMULATE;
    private String host = "smtp.gmail.com";
    private int port = 587;
    private Security security = Security.STARTTLS;
    private String username = "";
    private String password = "";
    private String fromAddress = "";
    private String fromName = "EcoNest";

    private static Properties defaults() {
        Properties p = new Properties();
        p.setProperty("email.mode", "SIMULATE");
        p.setProperty("smtp.host", "smtp.gmail.com");
        p.setProperty("smtp.port", "587");
        p.setProperty("smtp.security", "STARTTLS");
        p.setProperty("smtp.username", "");
        p.setProperty("smtp.password", "");
        p.setProperty("mail.from.address", "");
        p.setProperty("mail.from.name", "EcoNest");
        return p;
    }

    public static EmailConfig load() {
        Properties p = FILE.load();
        EmailConfig c = new EmailConfig();
        c.mode = parseEnum(Mode.class, p.getProperty("email.mode"), Mode.SIMULATE);
        c.host = p.getProperty("smtp.host", "").trim();
        try {
            c.port = Integer.parseInt(p.getProperty("smtp.port", "587").trim());
        } catch (NumberFormatException e) {
            c.port = 587;
        }
        c.security = parseEnum(Security.class, p.getProperty("smtp.security"), Security.STARTTLS);
        c.username = p.getProperty("smtp.username", "").trim();
        c.password = p.getProperty("smtp.password", "");
        c.fromAddress = p.getProperty("mail.from.address", "").trim();
        c.fromName = p.getProperty("mail.from.name", "EcoNest").trim();
        return c;
    }

    public void save() throws IOException {
        Properties p = new Properties();
        p.setProperty("email.mode", mode.name());
        p.setProperty("smtp.host", host);
        p.setProperty("smtp.port", String.valueOf(port));
        p.setProperty("smtp.security", security.name());
        p.setProperty("smtp.username", username);
        p.setProperty("smtp.password", password);
        p.setProperty("mail.from.address", fromAddress);
        p.setProperty("mail.from.name", fromName);
        FILE.save(p, "EcoNest e-mail settings - keep this file private, do NOT commit it");
    }

    public static String getFilePath() {
        return FILE.getPath().toString();
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /** The sender to use for the current settings. */
    public EmailSender createSender() {
        return mode == Mode.SMTP ? new SmtpEmailSender(this) : new SimulatedEmailSender();
    }

    /** Address shown in the From: header. */
    public String getEffectiveFrom() {
        return fromAddress.isEmpty() ? username : fromAddress;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }
}
