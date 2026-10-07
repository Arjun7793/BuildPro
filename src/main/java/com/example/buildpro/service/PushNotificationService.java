package com.example.buildpro.service;

import com.example.buildpro.entity.Lead;

// Push alert to every phone registered for new-lead notifications (see
// service/impl/FcmPushNotificationServiceImpl.java and DeviceController). Same
// contract as LeadNotificationService: fire-and-forget, returns immediately, and
// never throws - a push problem must never fail the public contact form.
public interface PushNotificationService {
    void notifyNewLead(Lead lead);
}
