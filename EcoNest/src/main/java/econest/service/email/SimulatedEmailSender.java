package econest.service.email;

/**
 * Used when no mail server is configured (the default). Nothing leaves the
 * computer: the e-mail is only stored in the Notification Centre, which is
 * safe for class demonstrations.
 */
public class SimulatedEmailSender implements EmailSender {

    @Override
    public void send(EmailMessage message) throws EmailException {
        if (message.getTo() == null || message.getTo().isBlank()) {
            throw new EmailException("No recipient e-mail address");
        }
        System.out.println("[SIMULATED E-MAIL] to=" + message.getTo() + " subject=" + message.getSubject());
    }

    @Override
    public boolean isSimulation() {
        return true;
    }

    @Override
    public String describe() {
        return "Simulation mode - e-mails are recorded in the Notification Centre but not sent";
    }
}
