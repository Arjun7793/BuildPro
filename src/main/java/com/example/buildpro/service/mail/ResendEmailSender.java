package com.example.buildpro.service.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import static com.example.buildpro.service.mail.HttpEmailSupport.quote;

/**
 * Resend (resend.com) over HTTPS - works where SMTP ports are blocked (Railway).
 * MAIL_PROVIDER=resend, MAIL_API_KEY=re_..., MAIL_FROM = an address on a domain
 * verified in Resend, or "BuildPro &lt;onboarding@resend.dev&gt;" without one
 * (then Resend only delivers to your own Resend account's email address).
 */
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "resend")
public class ResendEmailSender implements EmailSender {

    private final RestClient client;
    private final String apiKey;

    public ResendEmailSender(@Value("${app.mail.api-key:}") String apiKey,
                             @Value("${app.mail.resend-url:https://api.resend.com}") String baseUrl) {
        this.apiKey = apiKey;
        this.client = HttpEmailSupport.restClient(baseUrl);
    }

    @Override
    public void send(EmailMessage message) {
        String key = HttpEmailSupport.requireApiKey(apiKey, "resend");
        HttpEmailSupport.postJson(client, "Resend", "/emails", h -> h.setBearerAuth(key), payload(message));
    }

    static String payload(EmailMessage m) {
        return "{\"from\":" + quote(m.from())
                + ",\"to\":[" + quote(m.to()) + "]"
                + ",\"subject\":" + quote(m.subject())
                + ",\"text\":" + quote(m.text()) + "}";
    }
}
