package com.mbprime.MBPrime.service;

import com.mbprime.MBPrime.entity.FormSubmission;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * No-op implementation when mail is not on classpath or not configured.
 * Form submissions are still saved; emails are simply not sent.
 */
@Service
@ConditionalOnMissingBean(FormSubmissionEmailSender.class)
public class NoOpFormSubmissionEmailSender implements FormSubmissionEmailSender {
    @Override
    public void sendFormSubmissionEmails(FormSubmission submission) {
        // no-op
    }
}
