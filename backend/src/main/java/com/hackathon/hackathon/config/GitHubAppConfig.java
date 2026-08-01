package com.hackathon.hackathon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "github.app")
@Data
public class GitHubAppConfig {
  private String clientId;
  /** PEM private key content (env GITHUB_PRIVATE_KEY). Supports literal \\n. */
  private String privateKey;
  private String installationId;
  private String apiBaseUrl = "https://api.github.com";
  private String organization;
}
