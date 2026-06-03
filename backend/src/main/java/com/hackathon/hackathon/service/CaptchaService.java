package com.hackathon.hackathon.service;

import com.hackathon.hackathon.model.dto.response.CaptchaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class CaptchaService {

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${recaptcha.secret}")
    private String secretKey;

    public boolean verify(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        String url = UriComponentsBuilder.fromUriString(VERIFY_URL)
                .queryParam("secret", secretKey)
                .queryParam("response", token)
                .build()
                .toUriString();

        try {
            CaptchaResponse response = restTemplate.postForObject(url, null, CaptchaResponse.class);
            return response != null && response.isSuccess();
        } catch (RestClientException ex) {
            return false;
        }
    }
}
