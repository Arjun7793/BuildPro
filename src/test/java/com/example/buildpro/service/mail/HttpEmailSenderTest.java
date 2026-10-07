package com.example.buildpro.service.mail;

import com.example.buildpro.service.mail.EmailSender.EmailMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Request bodies for the HTTPS email APIs (Resend, Brevo) and the JSON escaping
// they rely on - lead messages are free text typed by site visitors.
class HttpEmailSenderTest {

    private final EmailMessage message = new EmailMessage(
            "BuildPro <onboarding@resend.dev>", "owner@example.com",
            "New lead: Ravi \"R\" Kumar", "Line 1\nNeeds a 3BHK – ₹50L\\budget\t");

    @Test
    void resendPayload() {
        assertEquals("{\"from\":\"BuildPro <onboarding@resend.dev>\",\"to\":[\"owner@example.com\"],"
                        + "\"subject\":\"New lead: Ravi \\\"R\\\" Kumar\","
                        + "\"text\":\"Line 1\\nNeeds a 3BHK – ₹50L\\\\budget\\t\"}",
                ResendEmailSender.payload(message));
    }

    @Test
    void brevoPayloadSplitsTheSenderName() {
        String json = BrevoEmailSender.payload(message);
        assertTrue(json.startsWith("{\"sender\":{\"name\":\"BuildPro\",\"email\":\"onboarding@resend.dev\"}"), json);
        assertTrue(json.contains("\"to\":[{\"email\":\"owner@example.com\"}]"), json);
        assertTrue(json.contains("\"textContent\":\"Line 1\\n"), json);
    }

    @Test
    void bareSenderAddressGetsADefaultName() {
        String json = BrevoEmailSender.payload(new EmailMessage("me@gmail.com", "a@b.c", "s", "t"));
        assertTrue(json.startsWith("{\"sender\":{\"name\":\"BuildPro\",\"email\":\"me@gmail.com\"}"), json);
    }

    @Test
    void controlCharactersAreEscaped() {
        assertEquals("\"a\\u0001b\"", HttpEmailSupport.quote("a\u0001b"));
    }

    @Test
    void missingApiKeyIsAClearError() {
        ResendEmailSender sender = new ResendEmailSender("", "https://api.resend.com");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> sender.send(message));
        assertTrue(e.getMessage().contains("MAIL_API_KEY"));
    }
}
