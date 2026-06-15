package com.hackathon.hackathon.service;

import com.hackathon.hackathon.model.dto.response.CaptchaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("secret", secretKey);
    map.add("response", token);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

    try {
      CaptchaResponse response =
          restTemplate.postForObject(VERIFY_URL, request, CaptchaResponse.class);
      return response != null && response.isSuccess();
    } catch (RestClientException ex) {
      return false;
    }
  }
}
