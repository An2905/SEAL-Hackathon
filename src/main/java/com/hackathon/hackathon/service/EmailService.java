package com.hackathon.hackathon.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;    


import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
public class EmailService {
    @Value("${brevo.api.key}")
    private String brevoApiKey;

    public boolean sendResetPasswordOtpEmail(String toEmail, String otp) {

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.brevo.com/v3/smtp/email";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);
            String body = """
            {
                "sender":{
                    "name":"Hackathon System",
                    "email":"quocannguyen385@gmail.com"
                },
                "to":[{"email":"%s"}],
                "subject":"Reset Password OTP",
                "htmlContent":"<h2>Your OTP is: %s</h2>"
            }
            """.formatted(toEmail, otp);

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url,HttpMethod.POST,entity,String.class);
            
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }
    }


    public boolean sendRegisterOtpEmail(String toEmail, String otp) {

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.brevo.com/v3/smtp/email";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);
            String body = """
            {
                "sender":{
                    "name":"Hackathon System",
                    "email":"quocannguyen385@gmail.com"
                },
                "to":[{"email":"%s"}],
                "subject":"Register OTP",
                "htmlContent":"<h2>Your OTP is: %s</h2>"
            }
            """.formatted(toEmail, otp);

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url,HttpMethod.POST,entity,String.class);
            
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }
    }
}

    



