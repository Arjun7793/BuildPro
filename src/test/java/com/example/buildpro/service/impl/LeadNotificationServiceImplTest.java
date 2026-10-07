package com.example.buildpro.service.impl;

import com.example.buildpro.entity.Lead;
import com.example.buildpro.service.CompanyInfoService;
import org.junit.jupiter.api.Test;
import com.example.buildpro.service.mail.EmailSender;
import com.example.buildpro.service.mail.EmailSender.EmailMessage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// The contact form must answer at once even when SMTP hangs or fails - the
// email goes out on a background thread.
class LeadNotificationServiceImplTest {

    private static Lead lead() {
        Lead lead = new Lead();
        lead.setId(42L);
        lead.setName("Ravi");
        lead.setMessage("Need a 3BHK plan");
        return lead;
    }

    @Test
    void returnsImmediatelyWhileTheEmailIsStillSending() throws Exception {
        EmailSender mailSender = mock(EmailSender.class);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch sent = new CountDownLatch(1);
        doAnswer(invocation -> {
            release.await(5, TimeUnit.SECONDS); // a slow SMTP server
            sent.countDown();
            return null;
        }).when(mailSender).send(any(EmailMessage.class));
        LeadNotificationServiceImpl service = new LeadNotificationServiceImpl(
                mailSender, mock(CompanyInfoService.class), true, "admin@example.com", "");

        long start = System.nanoTime();
        service.notifyNewLead(lead());
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue(tookMs < 1000, "notifyNewLead waited for SMTP (" + tookMs + " ms)");
        release.countDown();
        assertTrue(sent.await(5, TimeUnit.SECONDS), "email was never sent");
        verify(mailSender).send(argThat((EmailMessage m) ->
                "New lead: Ravi".equals(m.subject()) && "admin@example.com".equals(m.to())));
        service.shutdown();
    }

    @Test
    void aFailedSendIsOnlyLogged() throws Exception {
        EmailSender mailSender = mock(EmailSender.class);
        doThrow(new IllegalStateException("Resend rejected the email: HTTP 403")).when(mailSender).send(any(EmailMessage.class));
        LeadNotificationServiceImpl service = new LeadNotificationServiceImpl(
                mailSender, mock(CompanyInfoService.class), true, "admin@example.com", "");

        assertDoesNotThrow(() -> service.notifyNewLead(lead()));
        service.shutdown(); // waits for the background attempt
        verify(mailSender).send(any(EmailMessage.class));
    }

    @Test
    void disabledSendsNothing() throws Exception {
        EmailSender mailSender = mock(EmailSender.class);
        LeadNotificationServiceImpl service = new LeadNotificationServiceImpl(
                mailSender, mock(CompanyInfoService.class), false, "admin@example.com", "");
        service.notifyNewLead(lead());
        service.shutdown();
        verifyNoInteractions(mailSender);
    }
}
