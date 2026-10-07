package com.example.buildpro.service.impl;

import com.example.buildpro.entity.Lead;
import com.example.buildpro.service.CompanyInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

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
        JavaMailSender mailSender = mock(JavaMailSender.class);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch sent = new CountDownLatch(1);
        doAnswer(invocation -> {
            release.await(5, TimeUnit.SECONDS); // a slow SMTP server
            sent.countDown();
            return null;
        }).when(mailSender).send(any(SimpleMailMessage.class));
        LeadNotificationServiceImpl service = new LeadNotificationServiceImpl(
                mailSender, mock(CompanyInfoService.class), true, "admin@example.com", "");

        long start = System.nanoTime();
        service.notifyNewLead(lead());
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue(tookMs < 1000, "notifyNewLead waited for SMTP (" + tookMs + " ms)");
        release.countDown();
        assertTrue(sent.await(5, TimeUnit.SECONDS), "email was never sent");
        verify(mailSender).send(argThat((SimpleMailMessage m) ->
                "New lead: Ravi".equals(m.getSubject()) && m.getTo()[0].equals("admin@example.com")));
        service.shutdown();
    }

    @Test
    void aFailedSendIsOnlyLogged() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("Couldn't connect to host")).when(mailSender).send(any(SimpleMailMessage.class));
        LeadNotificationServiceImpl service = new LeadNotificationServiceImpl(
                mailSender, mock(CompanyInfoService.class), true, "admin@example.com", "");

        assertDoesNotThrow(() -> service.notifyNewLead(lead()));
        service.shutdown(); // waits for the background attempt
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void disabledSendsNothing() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        LeadNotificationServiceImpl service = new LeadNotificationServiceImpl(
                mailSender, mock(CompanyInfoService.class), false, "admin@example.com", "");
        service.notifyNewLead(lead());
        service.shutdown();
        verifyNoInteractions(mailSender);
    }
}
