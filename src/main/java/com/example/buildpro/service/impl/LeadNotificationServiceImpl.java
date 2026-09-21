package com.example.buildpro.service.impl;

import com.example.buildpro.entity.CompanyInfo;
import com.example.buildpro.entity.Lead;
import com.example.buildpro.service.CompanyInfoService;
import com.example.buildpro.service.LeadNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Emails a recipient whenever a new lead comes in through the public contact
 * form, so the admin doesn't have to keep checking /admin/leads manually. Off
 * by default (app.lead-notifications.enabled, LEAD_NOTIFICATIONS_ENABLED env
 * var) - a fresh local checkout with no SMTP configured just skips sending
 * rather than failing.
 *
 * Recipient resolution: app.lead-notifications.to (LEAD_NOTIFICATION_EMAIL),
 * when set, always wins - that's an explicit override for routing
 * notifications somewhere other than the public-facing company address, e.g.
 * to a different inbox than the one shown to site visitors. Otherwise it
 * falls back to CompanyInfo's own "email" field (the same one shown in the
 * site footer/contact section, editable via the admin panel's Company Info
 * form) - looked up fresh on every send rather than cached, so editing it in
 * the admin panel takes effect immediately with no redeploy or restart. If
 * neither is set, the notification is skipped with a warning logged.
 *
 * A failed send (bad credentials, SMTP provider down, no recipient resolved)
 * is only logged, never thrown - by the time this runs the lead is already
 * saved (see LeadServiceImpl.create()), and the visitor who submitted the
 * form should still get their normal success response regardless of whether
 * the email side of this works.
 */
@Slf4j
@Component
public class LeadNotificationServiceImpl implements LeadNotificationService {

    private final JavaMailSender mailSender;
    private final CompanyInfoService companyInfoService;
    private final boolean enabled;
    private final String configuredToAddress;
    private final String fromAddress;

    public LeadNotificationServiceImpl(
            JavaMailSender mailSender,
            CompanyInfoService companyInfoService,
            @Value("${app.lead-notifications.enabled:false}") boolean enabled,
            @Value("${app.lead-notifications.to:}") String configuredToAddress,
            @Value("${app.lead-notifications.from:}") String fromAddress) {
        this.mailSender = mailSender;
        this.companyInfoService = companyInfoService;
        this.enabled = enabled;
        this.configuredToAddress = configuredToAddress;
        this.fromAddress = fromAddress;
    }

    @Override
    public void notifyNewLead(Lead lead) {
        if (!enabled) {
            return;
        }
        String toAddress = resolveToAddress();
        if (toAddress == null || toAddress.isBlank()) {
            log.warn("app.lead-notifications.enabled is true but no recipient could be resolved "
                    + "(no LEAD_NOTIFICATION_EMAIL set, and no Company Info email configured in the "
                    + "admin panel) - skipping notification for lead {}", lead.getId());
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()) {
                message.setFrom(fromAddress);
            }
            message.setTo(toAddress);
            message.setSubject("New lead: " + lead.getName());
            message.setText(buildBody(lead));
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send new-lead notification email for lead {}", lead.getId(), e);
        }
    }

    private String resolveToAddress() {
        if (configuredToAddress != null && !configuredToAddress.isBlank()) {
            return configuredToAddress;
        }
        // Company Info is the same singleton-style section as Home/Cover and
        // About Us - usually exactly one row. findAll() rather than a
        // dedicated findFirst()/findById(1) so this keeps working even if
        // that assumption ever changes.
        List<CompanyInfo> companyInfos = companyInfoService.findAll();
        return companyInfos.stream()
                .map(CompanyInfo::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String buildBody(Lead lead) {
        StringBuilder body = new StringBuilder();
        body.append("A new lead came in through the BuildPro contact form.\n\n");
        body.append("Name: ").append(lead.getName()).append("\n");
        if (lead.getEmail() != null && !lead.getEmail().isBlank()) {
            body.append("Email: ").append(lead.getEmail()).append("\n");
        }
        if (lead.getPhone() != null && !lead.getPhone().isBlank()) {
            body.append("Phone: ").append(lead.getPhone()).append("\n");
        }
        if (lead.getMessage() != null && !lead.getMessage().isBlank()) {
            body.append("\nMessage:\n").append(lead.getMessage()).append("\n");
        }
        body.append("\nSubmitted: ").append(lead.getCreatedAt()).append("\n");
        body.append("\nView it in the admin panel: /admin/leads\n");
        return body.toString();
    }
}
