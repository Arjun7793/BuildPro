package com.example.buildpro.service.mail;

/**
 * Sends one plain-text email. Which implementation is active is chosen by
 * app.mail.provider (MAIL_PROVIDER):
 * <ul>
 *   <li>{@code smtp} (default) - {@link SmtpEmailSender}, via spring.mail.* (e.g. Gmail).
 *       Railway blocks outbound SMTP on its Trial/Hobby plans, so use an API there.</li>
 *   <li>{@code resend} - {@link ResendEmailSender}, Resend's HTTPS API (MAIL_API_KEY).</li>
 *   <li>{@code brevo} - {@link BrevoEmailSender}, Brevo's HTTPS API (MAIL_API_KEY).</li>
 * </ul>
 * Implementations throw on failure; callers decide whether to log or propagate.
 */
public interface EmailSender {

    void send(EmailMessage message);

    /** {@code from} may be "Name &lt;address&gt;" or a bare address. */
    record EmailMessage(String from, String to, String subject, String text) {
    }
}
