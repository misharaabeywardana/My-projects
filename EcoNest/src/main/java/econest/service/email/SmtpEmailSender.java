package econest.service.email;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Sends real e-mail through an SMTP server (for example Gmail with an App
 * Password) using only the standard Java library - no extra JAR needed.
 *
 * Conversation with the server: EHLO, STARTTLS (or SSL), AUTH LOGIN,
 * MAIL FROM, RCPT TO, DATA, QUIT.
 */
public class SmtpEmailSender implements EmailSender {

    private static final int TIMEOUT_MS = 20_000;

    private final EmailConfig config;

    public SmtpEmailSender(EmailConfig config) {
        this.config = config;
    }

    @Override
    public boolean isSimulation() {
        return false;
    }

    @Override
    public String describe() {
        return "SMTP " + config.getHost() + ":" + config.getPort() + " (" + config.getSecurity() + ") as "
                + config.getUsername();
    }

    @Override
    public void send(EmailMessage message) throws EmailException {
        if (config.getHost().isEmpty()) {
            throw new EmailException("SMTP host is not set. Open Settings > E-mail.");
        }
        String from = config.getEffectiveFrom();
        if (from.isEmpty()) {
            throw new EmailException("Sender address is not set. Open Settings > E-mail.");
        }
        if (message.getTo() == null || message.getTo().isBlank()) {
            throw new EmailException("No recipient e-mail address");
        }
        Socket socket = null;
        try {
            socket = openSocket();
            Conversation c = new Conversation(socket);
            c.expect(220);
            c.command("EHLO econest.local", 250);

            if (config.getSecurity() == EmailConfig.Security.STARTTLS) {
                c.command("STARTTLS", 220);
                socket = upgradeToTls(socket);
                c = new Conversation(socket);
                c.command("EHLO econest.local", 250);
            }

            if (!config.getUsername().isEmpty()) {
                c.command("AUTH LOGIN", 334);
                c.command(base64(config.getUsername()), 334);
                c.commandSecret(base64(config.getPassword()), 235);
            }

            c.command("MAIL FROM:<" + from + ">", 250);
            c.command("RCPT TO:<" + message.getTo().trim() + ">", 250, 251);
            c.command("DATA", 354);
            c.write(buildMime(from, message));
            c.command(".", 250);
            c.command("QUIT", 221);
        } catch (IOException e) {
            throw new EmailException("Could not send e-mail: " + e.getMessage(), e);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // nothing more to do
                }
            }
        }
    }

    private Socket openSocket() throws IOException {
        if (config.getSecurity() == EmailConfig.Security.SSL) {
            SSLSocket ssl = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
            ssl.connect(new InetSocketAddress(config.getHost(), config.getPort()), TIMEOUT_MS);
            verifyHostname(ssl);
            ssl.setSoTimeout(TIMEOUT_MS);
            ssl.startHandshake();
            return ssl;
        }
        Socket plain = new Socket();
        plain.connect(new InetSocketAddress(config.getHost(), config.getPort()), TIMEOUT_MS);
        plain.setSoTimeout(TIMEOUT_MS);
        return plain;
    }

    private Socket upgradeToTls(Socket plain) throws IOException {
        SSLSocket ssl = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault())
                .createSocket(plain, config.getHost(), config.getPort(), true);
        verifyHostname(ssl);
        ssl.setSoTimeout(TIMEOUT_MS);
        ssl.startHandshake();
        return ssl;
    }

    /** Makes Java check that the certificate really belongs to the mail server. */
    private static void verifyHostname(SSLSocket ssl) {
        SSLParameters params = ssl.getSSLParameters();
        params.setEndpointIdentificationAlgorithm("HTTPS");
        ssl.setSSLParameters(params);
    }

    private String buildMime(String from, EmailMessage m) {
        String fromHeader = config.getFromName().isEmpty() ? from : encodeWord(config.getFromName()) + " <" + from + ">";
        StringBuilder sb = new StringBuilder();
        sb.append("From: ").append(fromHeader).append("\r\n");
        sb.append("To: <").append(m.getTo().trim()).append(">\r\n");
        sb.append("Subject: ").append(encodeWord(m.getSubject())).append("\r\n");
        sb.append("Date: ").append(DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now())).append("\r\n");
        sb.append("Message-ID: <").append(UUID.randomUUID()).append("@econest.local>\r\n");
        sb.append("MIME-Version: 1.0\r\n");
        sb.append("Content-Type: text/plain; charset=UTF-8\r\n");
        sb.append("Content-Transfer-Encoding: base64\r\n");
        sb.append("\r\n");
        // Base64 body: safe for any character and never contains a line with a single "."
        String body = m.getBody().replace("\r\n", "\n").replace("\n", "\r\n");
        sb.append(Base64.getMimeEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8)));
        sb.append("\r\n");
        return sb.toString();
    }

    private static String encodeWord(String text) {
        return "=?UTF-8?B?" + Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)) + "?=";
    }

    private static String base64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    /** Reads and writes SMTP lines and checks the server's reply codes. */
    private static final class Conversation {
        private final BufferedReader in;
        private final OutputStream out;

        Conversation(Socket socket) throws IOException {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            this.out = socket.getOutputStream();
        }

        void write(String data) throws IOException {
            out.write(data.getBytes(StandardCharsets.US_ASCII));
            out.flush();
        }

        void command(String line, int... okCodes) throws IOException {
            write(line + "\r\n");
            expect(line, okCodes);
        }

        /** Same as command, but the text is hidden in error messages (passwords). */
        void commandSecret(String line, int... okCodes) throws IOException {
            write(line + "\r\n");
            expect("(password)", okCodes);
        }

        void expect(int... okCodes) throws IOException {
            expect("(connect)", okCodes);
        }

        private void expect(String sent, int... okCodes) throws IOException {
            String line;
            StringBuilder reply = new StringBuilder();
            do {
                line = in.readLine();
                if (line == null) {
                    throw new IOException("Mail server closed the connection");
                }
                reply.append(line).append(' ');
            } while (line.length() > 3 && line.charAt(3) == '-');
            int code;
            try {
                code = Integer.parseInt(line.substring(0, 3));
            } catch (RuntimeException e) {
                throw new IOException("Unexpected reply from mail server: " + line);
            }
            for (int ok : okCodes) {
                if (code == ok) {
                    return;
                }
            }
            String shown = sent.startsWith("AUTH") || sent.length() > 40 ? sent.split(" ")[0] : sent;
            if (code == 535 || code == 534) {
                throw new IOException("Login rejected by mail server (" + code + "). For Gmail, use an App "
                        + "Password, not your normal password.");
            }
            throw new IOException("Server replied " + reply.toString().trim() + " to " + shown);
        }
    }
}
