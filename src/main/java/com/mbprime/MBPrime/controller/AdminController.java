package com.mbprime.MBPrime.controller;

import com.mbprime.MBPrime.dto.LoginRequest;
import com.mbprime.MBPrime.dto.LoginResponse;
import com.mbprime.MBPrime.entity.Admin;
import com.mbprime.MBPrime.entity.FormSubmission;
import com.mbprime.MBPrime.repository.AdminRepository;
import com.mbprime.MBPrime.repository.FormSubmissionRepository;
import com.mbprime.MBPrime.config.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class AdminController {

    private final AdminRepository adminRepository;
    private final FormSubmissionRepository submissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AdminController(AdminRepository adminRepository, FormSubmissionRepository submissionRepository,
                           PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.adminRepository = adminRepository;
        this.submissionRepository = submissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
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
        List<java.util.Map<String, Object>> body = list.stream().map(s -> java.util.Map.<String, Object>of(
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
}
