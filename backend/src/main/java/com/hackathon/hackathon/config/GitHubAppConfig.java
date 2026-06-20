package com.hackathon.hackathon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "github.app")
@Data
public class GitHubAppConfig {
  private String clientId;
  private String privateKeyPath;
  private String installationId;
  private String apiBaseUrl = "https://api.github.com";
}
