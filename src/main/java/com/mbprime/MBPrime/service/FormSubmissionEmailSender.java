package com.mbprime.MBPrime.service;

import com.mbprime.MBPrime.entity.FormSubmission;

public interface FormSubmissionEmailSender {
    void sendFormSubmissionEmails(FormSubmission submission);
}
