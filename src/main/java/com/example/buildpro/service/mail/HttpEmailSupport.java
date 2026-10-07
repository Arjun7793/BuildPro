package com.example.buildpro.service.mail;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared bits for the HTTPS email APIs: a RestClient with 10 s timeouts, a POST
 * of a JSON string, readable errors, and minimal JSON string escaping (the
 * payloads are tiny and fixed-shape, so no JSON library is needed).
 */
final class HttpEmailSupport {

    private static final Pattern NAME_AND_ADDRESS = Pattern.compile("^\\s*(.*?)\\s*<\\s*([^>]+?)\\s*>\\s*$");

    private HttpEmailSupport() {
    }

    static RestClient restClient(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    static void postJson(RestClient client, String provider, String path, Consumer<org.springframework.http.HttpHeaders> headers, String json) {
        try {
            client.post()
                    .uri(path)
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(json.getBytes(StandardCharsets.UTF_8))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            // Surface the provider's own explanation, e.g. an unverified sender.
            throw new IllegalStateException(provider + " rejected the email: HTTP " + e.getStatusCode().value()
                    + " " + e.getResponseBodyAsString(), e);
        }
    }

    static String requireApiKey(String apiKey, String provider) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("app.mail.provider is " + provider + " but MAIL_API_KEY is not set");
        }
        return apiKey.strip();
    }

    /** Splits "BuildPro &lt;a@b.com&gt;" into {name, address}; a bare address gives {"", address}. */
    static String[] nameAndAddress(String from) {
        if (from == null) {
            return new String[] {"", ""};
        }
        Matcher m = NAME_AND_ADDRESS.matcher(from);
        return m.matches() ? new String[] {m.group(1).replace("\"", ""), m.group(2)} : new String[] {"", from.strip()};
    }

    static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
