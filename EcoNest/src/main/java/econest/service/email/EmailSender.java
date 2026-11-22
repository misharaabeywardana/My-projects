package econest.service.email;

/**
 * Anything that can send an e-mail. The application talks only to this
 * interface, so the real SMTP sender and the simulation sender can be
 * swapped without changing the rest of the code (polymorphism).
 */
public interface EmailSender {

    void send(EmailMessage message) throws EmailException;

    /** True when e-mails are only recorded, not really sent. */
    boolean isSimulation();

    /** Short description shown in the settings screen. */
    String describe();
}
