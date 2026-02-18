package com.mbprime.MBPrime.dto;

public class FormSubmissionRequest {
    private String formType; // "enquiry" or "connect_with_us"
    private String name;
    private String email;
    private String phone;
    private String message;

    public String getFormType() { return formType; }
    public void setFormType(String formType) { this.formType = formType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
