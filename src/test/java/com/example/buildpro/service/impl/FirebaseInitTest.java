package com.example.buildpro.service.impl;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Builds Firebase the same way FcmPushNotificationServiceImpl does when push is
// enabled, with dummy credentials (no network call is made). This fails if a
// library Firebase needs at startup is missing from the classpath - which once
// took production down (NoClassDefFoundError: JacksonFactory) while every
// push-disabled test still passed.
class FirebaseInitTest {

    @Test
    void firebaseStartsWithTheBuildClasspath() {
        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken("test", null));
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId("buildpro-test")
                .build();
        FirebaseApp app = FirebaseApp.initializeApp(options, "firebase-init-test");
        try {
            assertNotNull(FirebaseMessaging.getInstance(app));
        } finally {
            app.delete();
        }
    }
}
