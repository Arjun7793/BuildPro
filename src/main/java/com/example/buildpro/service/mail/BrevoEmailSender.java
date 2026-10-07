package com.example.buildpro.service.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import static com.example.buildpro.service.mail.HttpEmailSupport.quote;

/**
 * Brevo (brevo.com) transactional email over HTTPS - works where SMTP ports are
 * blocked (Railway). MAIL_PROVIDER=brevo, MAIL_API_KEY=xkeysib-..., MAIL_FROM =
 * a sender verified in Brevo (Senders, domains & IPs -> Senders); a single
 * verified address such as your Gmail works without owning a domain.
 */
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "brevo")
public class BrevoEmailSender implements EmailSender {

    private final RestClient client;
    private final String apiKey;

    public BrevoEmailSender(@Value("${app.mail.api-key:}") String apiKey,
                            @Value("${app.mail.brevo-url:https://api.brevo.com}") String baseUrl) {
        this.apiKey = apiKey;
        this.client = HttpEmailSupport.restClient(baseUrl);
    }

    @Override
    public void send(EmailMessage message) {
        String key = HttpEmailSupport.requireApiKey(apiKey, "brevo");
        HttpEmailSupport.postJson(client, "Brevo", "/v3/smtp/email", h -> h.set("api-key", key), payload(message));
    }

    static String payload(EmailMessage m) {
        String[] sender = HttpEmailSupport.nameAndAddress(m.from());
        String name = sender[0].isEmpty() ? "BuildPro" : sender[0];
        return "{\"sender\":{\"name\":" + quote(name) + ",\"email\":" + quote(sender[1]) + "}"
                + ",\"to\":[{\"email\":" + quote(m.to()) + "}]"
                + ",\"subject\":" + quote(m.subject())
                + ",\"textContent\":" + quote(m.text()) + "}";
    }
}
