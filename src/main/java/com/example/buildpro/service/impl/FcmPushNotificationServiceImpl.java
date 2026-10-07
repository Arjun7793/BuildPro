package com.example.buildpro.service.impl;

import com.example.buildpro.entity.DeviceToken;
import com.example.buildpro.entity.Lead;
import com.example.buildpro.service.DeviceTokenService;
import com.example.buildpro.service.PushNotificationService;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Sends a "New lead" push alert to every phone registered via POST /api/devices,
 * through Firebase Cloud Messaging (FCM delivers to Android directly and to iOS
 * via APNs - the Flutter app uses the same firebase_messaging plugin for both).
 *
 * Off by default (app.push.enabled / PUSH_NOTIFICATIONS_ENABLED), like email
 * notifications, so local dev needs no Firebase project. When enabled it needs a
 * Firebase service-account key: FIREBASE_CREDENTIALS_JSON (the whole JSON file's
 * contents), or, if that's empty, Google's standard GOOGLE_APPLICATION_CREDENTIALS
 * lookup. If Firebase can't be started (bad credentials, missing library) the
 * error is logged loudly and the app runs with push off - a push problem must
 * never take the website down. Look for "Push notifications" in the startup log.
 *
 * Sending happens on a background thread, so the contact form's response never
 * waits on Firebase. Failures are logged, never thrown. Tokens Firebase reports
 * as dead (app uninstalled, token rotated) are deleted so they aren't retried.
 */
@Slf4j
@Component
public class FcmPushNotificationServiceImpl implements PushNotificationService {

    // FCM's limit on tokens per multicast request.
    private static final int MAX_TOKENS_PER_BATCH = 500;
    private static final int MAX_BODY_LENGTH = 100;
    private static final Set<MessagingErrorCode> DEAD_TOKEN_ERRORS =
            Set.of(MessagingErrorCode.UNREGISTERED, MessagingErrorCode.INVALID_ARGUMENT, MessagingErrorCode.SENDER_ID_MISMATCH);

    private final DeviceTokenService deviceTokenService;
    private final boolean enabled;
    private final String androidChannelId;
    private final FirebaseMessaging messaging;
    private final ExecutorService executor;

    public FcmPushNotificationServiceImpl(
            DeviceTokenService deviceTokenService,
            @Value("${app.push.enabled:false}") boolean enabled,
            @Value("${app.push.firebase-credentials-json:}") String credentialsJson,
            @Value("${app.push.android-channel-id:new_leads}") String androidChannelId) {
        this.deviceTokenService = deviceTokenService;
        this.androidChannelId = androidChannelId;
        FirebaseMessaging started = enabled ? start(credentialsJson) : null;
        this.enabled = started != null;
        this.messaging = started;
        this.executor = this.enabled
                ? Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "push-notifications");
                    thread.setDaemon(true);
                    return thread;
                })
                : null;
        if (!enabled) {
            log.info("Push notifications are off (PUSH_NOTIFICATIONS_ENABLED is not true)");
        }
    }

    // Returns null (push stays off) instead of failing startup. LinkageError covers
    // a library missing from the build (NoClassDefFoundError), as seen once in prod.
    private static FirebaseMessaging start(String credentialsJson) {
        try {
            FirebaseApp app = initFirebase(credentialsJson);
            log.info("Push notifications enabled - Firebase started");
            return FirebaseMessaging.getInstance(app);
        } catch (RuntimeException | LinkageError e) {
            log.error("PUSH NOTIFICATIONS DISABLED: PUSH_NOTIFICATIONS_ENABLED is true but Firebase could not be "
                    + "started. The site keeps running without push alerts. Cause:", e);
            return null;
        }
    }

    private static FirebaseApp initFirebase(String credentialsJson) {
        try {
            GoogleCredentials credentials = (credentialsJson == null || credentialsJson.isBlank())
                    ? GoogleCredentials.getApplicationDefault()
                    : GoogleCredentials.fromStream(new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)));
            FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();
            // Named app, so this can't clash with any other Firebase initialisation.
            return FirebaseApp.initializeApp(options, "buildpro-push");
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("app.push.enabled is true but Firebase could not be initialised - "
                    + "check FIREBASE_CREDENTIALS_JSON (the service-account key JSON)", e);
        }
    }

    @Override
    public void notifyNewLead(Lead lead) {
        if (!enabled) {
            return;
        }
        // Copy what we need now - the entity isn't touched from the other thread.
        String title = "New lead: " + lead.getName();
        String body = buildBody(lead.getMessage(), lead.getPhone());
        String leadId = String.valueOf(lead.getId());
        try {
            executor.submit(() -> send(title, body, leadId));
        } catch (RuntimeException e) {
            log.error("Could not queue push notification for lead {}", leadId, e);
        }
    }

    private void send(String title, String body, String leadId) {
        try {
            List<String> tokens = deviceTokenService.findAll().stream().map(DeviceToken::getToken).toList();
            if (tokens.isEmpty()) {
                log.debug("No devices registered for push - skipping lead {}", leadId);
                return;
            }
            List<String> deadTokens = new ArrayList<>();
            int sent = 0;
            for (int start = 0; start < tokens.size(); start += MAX_TOKENS_PER_BATCH) {
                List<String> batch = tokens.subList(start, Math.min(start + MAX_TOKENS_PER_BATCH, tokens.size()));
                BatchResponse response = messaging.sendEachForMulticast(buildMessage(batch, title, body, leadId));
                sent += response.getSuccessCount();
                List<SendResponse> results = response.getResponses();
                for (int i = 0; i < results.size(); i++) {
                    FirebaseMessagingException error = results.get(i).getException();
                    if (error != null && DEAD_TOKEN_ERRORS.contains(error.getMessagingErrorCode())) {
                        deadTokens.add(batch.get(i));
                    } else if (error != null) {
                        log.warn("Push to one device failed for lead {}: {}", leadId, error.getMessagingErrorCode());
                    }
                }
            }
            log.info("Sent new-lead push for lead {} to {} of {} device(s)", leadId, sent, tokens.size());
            deviceTokenService.removeTokens(deadTokens);
        } catch (Exception e) {
            log.error("Failed to send new-lead push notification for lead {}", leadId, e);
        }
    }

    private MulticastMessage buildMessage(List<String> tokens, String title, String body, String leadId) {
        return MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                // The app reads these to open the right Lead detail screen when tapped.
                .putData("type", "NEW_LEAD")
                .putData("leadId", leadId)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder().setChannelId(androidChannelId).build())
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder().setSound("default").build())
                        .build())
                .build();
    }

    // Notification text under the title: the start of the enquiry if there is
    // one, otherwise the phone number, otherwise a generic prompt.
    static String buildBody(String message, String phone) {
        if (message != null && !message.isBlank()) {
            String text = message.strip().replaceAll("\\s+", " ");
            return text.length() <= MAX_BODY_LENGTH ? text : text.substring(0, MAX_BODY_LENGTH - 1) + "…";
        }
        if (phone != null && !phone.isBlank()) {
            return "Phone: " + phone.strip();
        }
        return "Tap to view the enquiry";
    }

    @PreDestroy
    void shutdown() throws InterruptedException {
        if (executor != null) {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
