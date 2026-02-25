package com.mbprime.MBPrime.service;

import com.mbprime.MBPrime.entity.FormSubmission;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnClass(name = "org.springframework.mail.javamail.JavaMailSender")
public class EmailService implements FormSubmissionEmailSender {

    private final JavaMailSender mailSender;
    private final String adminEmail;
    private final String fromEmail;

    public EmailService(JavaMailSender mailSender,
                        @Value("${mbprime.admin.email:}") String adminEmail,
                        @Value("${spring.mail.username:}") String fromEmail) {
        this.mailSender = mailSender;
        this.adminEmail = adminEmail != null ? adminEmail.trim() : "";
        this.fromEmail = fromEmail != null ? fromEmail.trim() : "";
    }

    /**
     * Sends confirmation to the user (if email provided) and a copy to admin.
     * Does not throw: failures are logged so form submission still succeeds.
     */
    public void sendFormSubmissionEmails(FormSubmission submission) {
        if (fromEmail.isEmpty()) {
            return; // SMTP not configured
        }
        String formTypeLabel = formTypeLabel(submission.getFormType());
        String name = nullToEmpty(submission.getName());
        String email = nullToEmpty(submission.getEmail());
        String phone = nullToEmpty(submission.getPhone());
        String message = nullToEmpty(submission.getMessage());

        // Email to user (only if they provided an email)
        if (!email.isEmpty()) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(fromEmail);
                msg.setTo(email);
                msg.setSubject("Thank you for contacting MB Prime");
                msg.setText(
                    "Dear " + name + ",\n\n" +
                    "Thank you for your " + formTypeLabel.toLowerCase() + ".\n\n" +
                    "We have received your details and our team will get back to you within 24 hours.\n\n" +
                    "Best regards,\nMB Prime"
                );
                mailSender.send(msg);
            } catch (Exception e) {
                // Log but do not fail the request
                e.printStackTrace();
            }
        }

        // Email to admin
        if (!adminEmail.isEmpty()) {
            try {
                SimpleMailMessage adminMsg = new SimpleMailMessage();
                adminMsg.setFrom(fromEmail);
                adminMsg.setTo(adminEmail);
                adminMsg.setSubject("New form submission: " + formTypeLabel + " - " + name);
                adminMsg.setText(
                    "A new " + formTypeLabel + " has been submitted.\n\n" +
                    "Name: " + name + "\n" +
                    "Email: " + (email.isEmpty() ? "(not provided)" : email) + "\n" +
                    "Phone: " + phone + "\n" +
                    (message.isEmpty() ? "" : "Message: " + message + "\n")
                );
                mailSender.send(adminMsg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static String formTypeLabel(String type) {
        if (type == null) return "Enquiry";
        return switch (type) {
            case "brochure" -> "Brochure download";
            case "contact_us" -> "Contact us";
            default -> "Enquiry";
        };
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Sends password reset link to admin email. Does not throw; logs on failure.
     */
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        if (fromEmail.isEmpty() || toEmail == null || toEmail.isBlank()) return;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(toEmail.trim());
            msg.setSubject("MB Prime Admin – Reset your password");
            msg.setText(
                "You requested a password reset for the MB Prime admin account.\n\n" +
                "Click the link below to set a new password (valid for 1 hour):\n\n" +
                resetLink + "\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "Best regards,\nMB Prime"
            );
            mailSender.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
