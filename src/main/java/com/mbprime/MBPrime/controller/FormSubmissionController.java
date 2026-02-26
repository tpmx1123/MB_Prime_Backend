package com.mbprime.MBPrime.controller;

import com.mbprime.MBPrime.dto.FormSubmissionRequest;
import com.mbprime.MBPrime.entity.FormSubmission;
import com.mbprime.MBPrime.repository.FormSubmissionRepository;
import com.mbprime.MBPrime.service.FormSubmissionEmailSender;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173", "https://mbprimeprojects.com", "https://www.mbprimeprojects.com"}, allowCredentials = "true")
public class FormSubmissionController {

    private final FormSubmissionRepository repository;
    private final FormSubmissionEmailSender emailSender;

    public FormSubmissionController(FormSubmissionRepository repository, FormSubmissionEmailSender emailSender) {
        this.repository = repository;
        this.emailSender = emailSender;
    }

    @PostMapping("/submissions")
    public ResponseEntity<?> submitForm(@RequestBody FormSubmissionRequest request) {
        if (request.getFormType() == null || request.getFormType().isBlank()) {
            request.setFormType("enquiry");
        }
        String type = request.getFormType().trim().toLowerCase();
        if (!"brochure".equals(type) && !"enquiry".equals(type) && !"contact_us".equals(type)) {
            request.setFormType("enquiry");
        } else {
            request.setFormType(type);
        }
        FormSubmission entity = new FormSubmission();
        entity.setFormType(request.getFormType());
        entity.setName(request.getName() != null ? request.getName().trim() : "");
        entity.setEmail(request.getEmail() != null ? request.getEmail().trim() : "");
        entity.setPhone(request.getPhone() != null ? request.getPhone().trim() : "");
        entity.setMessage(request.getMessage() != null ? request.getMessage().trim() : "");
        repository.save(entity);
        emailSender.sendFormSubmissionEmails(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(java.util.Map.of("success", true, "message", "Submission saved."));
    }
}
