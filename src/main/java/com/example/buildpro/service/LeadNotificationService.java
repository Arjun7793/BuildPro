package com.example.buildpro.service;

import com.example.buildpro.entity.Lead;

// Fire-and-forget notification for a newly created lead (currently just email -
// see service/impl/LeadNotificationServiceImpl.java). Deliberately returns
// nothing and is expected to swallow/log its own failures: a notification
// problem must never surface as a failure of the public contact form, which
// is the one thing calling this (see LeadServiceImpl.create()).
public interface LeadNotificationService {
    void notifyNewLead(Lead lead);
}
