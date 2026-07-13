package com.hackathon.hackathon.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

@Service
public class EmailService {

  private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";
  private static final String SENDER_EMAIL = "quocannguyen385@gmail.com";
  private static final String SENDER_NAME = "Hackathon System";

  @Value("${brevo.api.key}")
  private String brevoApiKey;

  @Value("${email.dev-bypass:false}")
  private boolean devBypass;

  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper objectMapper = new ObjectMapper();

  public boolean sendResetPasswordOtpEmail(String toEmail, String otp) {
    return sendEmail(
        toEmail, "Reset Password OTP", "<h2>Your OTP is: " + escapeHtml(otp) + "</h2>", otp);
  }

  public boolean sendRegisterOtpEmail(String toEmail, String otp) {
    return sendEmail(toEmail, "Register OTP", "<h2>Your OTP is: " + escapeHtml(otp) + "</h2>", otp);
  }

  public boolean sendAccountCredentialsEmail(String toEmail, String fullName, String password) {
    String html =
        "<h2>Tài khoản Hackathon của bạn đã được tạo</h2>"
            + "<p>Xin chào "
            + escapeHtml(fullName)
            + ",</p>"
            + "<p>Email đăng nhập: <b>"
            + escapeHtml(toEmail)
            + "</b></p>"
            + "<p>Mật khẩu tạm: <b>"
            + escapeHtml(password)
            + "</b></p>"
            + "<p>Vui lòng đăng nhập và đổi mật khẩu ngay sau lần đăng nhập đầu tiên.</p>";
    return sendEmail(toEmail, "Thông tin tài khoản Hackathon", html, password);
  }

  private String resolveApiKey() {
    if (brevoApiKey == null) {
      return null;
    }
    String key = brevoApiKey.trim();
    if ((key.startsWith("\"") && key.endsWith("\"")) || (key.startsWith("'") && key.endsWith("'"))) {
      key = key.substring(1, key.length() - 1).trim();
    }
    return key;
  }

  private boolean sendEmail(String toEmail, String subject, String htmlContent, String otp) {
    if (devBypass) {
      System.out.println("=== [DEV BYPASS] Email to " + toEmail + ": " + otp + " ===");
      return true;
    }
    String apiKey = resolveApiKey();
    if (apiKey == null || apiKey.isBlank()) {
      System.err.println(
          "BREVO_API_KEY is not configured — bật email.dev-bypass=true trong .env.properties để test local");
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
      headers.set("api-key", apiKey);
      HttpEntity<String> entity = new HttpEntity<>(json, headers);
      ResponseEntity<String> response =
          restTemplate.exchange(BREVO_URL, HttpMethod.POST, entity, String.class);
      if (!response.getStatusCode().is2xxSuccessful()) {
        System.err.println(
            "Brevo API error: " + response.getStatusCode() + " — " + response.getBody());
        return false;
      }
      return true;
    } catch (Exception e) {
      System.err.println("Brevo exception: " + e.getMessage());
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
