package com.mbprime.MBPrime.controller;

import com.mbprime.MBPrime.dto.LoginRequest;
import com.mbprime.MBPrime.dto.LoginResponse;
import com.mbprime.MBPrime.entity.Admin;
import com.mbprime.MBPrime.entity.FormSubmission;
import com.mbprime.MBPrime.repository.AdminRepository;
import com.mbprime.MBPrime.repository.FormSubmissionRepository;
import com.mbprime.MBPrime.config.JwtUtil;
import com.mbprime.MBPrime.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173", "https://mbprimeprojects.com", "https://www.mbprimeprojects.com"}, allowCredentials = "true")
public class AdminController {

    private final AdminRepository adminRepository;
    private final FormSubmissionRepository submissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Value("${mbprime.admin.email:}")
    private String adminEmail;

    @Value("${mbprime.frontend.url:http://localhost:5173, https://mbprimeprojects.com, https://www.mbprimeprojects.com}")
    private String frontendUrl;

    @Autowired(required = false)
    public AdminController(AdminRepository adminRepository, FormSubmissionRepository submissionRepository,
                           PasswordEncoder passwordEncoder, JwtUtil jwtUtil, EmailService emailService) {
        this.adminRepository = adminRepository;
        this.submissionRepository = submissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank() ||
            request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(new LoginResponse(false, null, "Username and password required."));
        }
        Admin admin = adminRepository.findByUsername(request.getUsername().trim()).orElse(null);
        if (admin == null || !passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            return ResponseEntity.status(401).body(new LoginResponse(false, null, "Invalid username or password."));
        }
        String token = jwtUtil.generateToken(admin.getUsername());
        return ResponseEntity.ok(new LoginResponse(true, token, "Login successful."));
    }

    @GetMapping("/submissions")
    public ResponseEntity<?> getSubmissions() {
        List<FormSubmission> list = submissionRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> body = list.stream().map(s -> Map.<String, Object>of(
            "id", s.getId(),
            "formType", s.getFormType() != null ? s.getFormType() : "",
            "name", s.getName() != null ? s.getName() : "",
            "email", s.getEmail() != null ? s.getEmail() : "",
            "phone", s.getPhone() != null ? s.getPhone() : "",
            "message", s.getMessage() != null ? s.getMessage() : "",
            "createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : ""
        )).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, Object> body) {
        String username = body != null && body.get("username") != null ? body.get("username").toString().trim() : "";
        if (username.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Username is required."));
        }
        Admin admin = adminRepository.findByUsername(username).orElse(null);
        if (admin == null) {
            return ResponseEntity.ok(Map.of("success", true, "message", "If an account exists with this username, a reset link has been sent to the admin email."));
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        admin.setPasswordResetToken(token);
        admin.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        adminRepository.save(admin);
        String resetLink = frontendUrl.replaceAll("/$", "") + "/admin-reset-password?token=" + token;
        if (adminEmail != null && !adminEmail.isBlank() && emailService != null) {
            emailService.sendPasswordResetEmail(adminEmail, resetLink);
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "If an account exists with this username, a reset link has been sent to the admin email."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, Object> body) {
        String token = body != null && body.get("token") != null ? body.get("token").toString().trim() : "";
        String newPassword = body != null && body.get("newPassword") != null ? body.get("newPassword").toString() : null;
        if (token.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Reset token is required."));
        }
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "New password must be at least 6 characters."));
        }
        Admin admin = adminRepository.findByPasswordResetToken(token).orElse(null);
        if (admin == null || admin.getPasswordResetTokenExpiry() == null || admin.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid or expired reset link. Please request a new one."));
        }
        admin.setPasswordHash(passwordEncoder.encode(newPassword));
        admin.setPasswordResetToken(null);
        admin.setPasswordResetTokenExpiry(null);
        adminRepository.save(admin);
        return ResponseEntity.ok(Map.of("success", true, "message", "Password has been reset. You can now log in."));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, Object> body, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Not authenticated."));
        }
        String currentPassword = body != null && body.get("currentPassword") != null ? body.get("currentPassword").toString() : null;
        String newPassword = body != null && body.get("newPassword") != null ? body.get("newPassword").toString() : null;
        if (currentPassword == null || currentPassword.isBlank() || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Current password and new password are required."));
        }
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "New password must be at least 6 characters."));
        }
        Admin admin = adminRepository.findByUsername(auth.getName()).orElse(null);
        if (admin == null || !passwordEncoder.matches(currentPassword, admin.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Current password is incorrect."));
        }
        admin.setPasswordHash(passwordEncoder.encode(newPassword));
        adminRepository.save(admin);
        return ResponseEntity.ok(Map.of("success", true, "message", "Password has been changed."));
    }
}
