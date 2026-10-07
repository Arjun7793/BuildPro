package com.example.buildpro.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// The notification text rules - no Firebase connection needed.
class FcmPushNotificationServiceImplTest {

    @Test
    void usesTheMessageWhenThereIsOne() {
        assertEquals("Need a quote for a duplex", FcmPushNotificationServiceImpl.buildBody("  Need a quote\n for a   duplex ", "+91 98450 12345"));
    }

    @Test
    void truncatesLongMessagesTo100Characters() {
        String body = FcmPushNotificationServiceImpl.buildBody("x".repeat(250), null);
        assertEquals(100, body.length());
        assertTrue(body.endsWith("…"));
    }

    @Test
    void fallsBackToPhoneThenGenericText() {
        assertEquals("Phone: +91 98450 12345", FcmPushNotificationServiceImpl.buildBody(" ", "+91 98450 12345"));
        assertEquals("Tap to view the enquiry", FcmPushNotificationServiceImpl.buildBody(null, null));
    }

    @Test
    void disabledServiceDoesNothingAndNeedsNoFirebase() {
        FcmPushNotificationServiceImpl service = new FcmPushNotificationServiceImpl(null, false, "", "new_leads");
        assertDoesNotThrow(() -> service.notifyNewLead(new com.example.buildpro.entity.Lead()));
    }
}
