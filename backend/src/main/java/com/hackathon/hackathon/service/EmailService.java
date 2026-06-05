package com.hackathon.hackathon.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EmailService {

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String SENDER_EMAIL = "quocannguyen385@gmail.com";
    private static final String SENDER_NAME = "Hackathon System";

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean sendResetPasswordOtpEmail(String toEmail, String otp) {
        return sendEmail(toEmail, "Reset Password OTP", "<h2>Your OTP is: " + escapeHtml(otp) + "</h2>");
    }

    public boolean sendRegisterOtpEmail(String toEmail, String otp) {
        return sendEmail(toEmail, "Register OTP", "<h2>Your OTP is: " + escapeHtml(otp) + "</h2>");
    }

    public boolean sendStaffAccountInvite(
            String toEmail,
            String fullName,
            String password,
            String role) {
        String subject = "SEAL Hackathon — Tài khoản " + role;
        String html = "<h2>Xin chào " + escapeHtml(fullName) + "</h2>"
                + "<p>Tài khoản <b>" + escapeHtml(role) + "</b> của bạn đã được tạo trên hệ thống SEAL Hackathon.</p>"
                + "<p><b>Email đăng nhập:</b> " + escapeHtml(toEmail) + "</p>"
                + "<p><b>Mật khẩu tạm:</b> " + escapeHtml(password) + "</p>"
                + "<p>Sau khi đăng nhập, vui lòng vào <b>Cập nhật hồ sơ</b> để bổ sung <b>số điện thoại</b> liên hệ và đổi mật khẩu nếu cần.</p>"
                + "<p>Trân trọng,<br/>Ban tổ chức SEAL Hackathon</p>";
        return sendEmail(toEmail, subject, html);
    }

    public boolean sendAnnouncement(String toEmail, String fullName, String title, String content) {
        String html = "<h2>Xin chào " + escapeHtml(fullName) + "</h2><p>" + escapeHtml(content) + "</p>";
        return sendEmail(toEmail, title, html);
    }

    private boolean sendEmail(String toEmail, String subject, String htmlContent) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            System.err.println("BREVO_API_KEY is not configured in backend/.env.properties");
            return false;
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("sender", Map.of("name", SENDER_NAME, "email", SENDER_EMAIL));
            body.put("to", List.of(Map.of("email", toEmail)));
            body.put("subject", subject);
            body.put("htmlContent", htmlContent);

            String json = objectMapper.writeValueAsString(body);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    BREVO_URL, HttpMethod.POST, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                System.err.println("Brevo API error: " + response.getStatusCode() + " — " + response.getBody());
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
