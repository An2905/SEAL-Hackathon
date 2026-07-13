package com.hackathon.hackathon.service.github;

import com.hackathon.hackathon.config.GitHubAppConfig;
import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GitHubJwtService {

  @Autowired private GitHubAppConfig config;

  public String generateJwt() {
    try {
      String pemContent = normalizePem(config.getPrivateKey());
      if (pemContent == null || pemContent.isBlank()) {
        throw new IllegalStateException("GITHUB_PRIVATE_KEY is not configured.");
      }

      PrivateKey privateKey = getPrivateKey(pemContent);

      long now = Instant.now().getEpochSecond();

      // Build and sign the JWT with RS256
      return Jwts.builder()
          .issuedAt(Date.from(Instant.ofEpochSecond(now - 60))) // 60s in past for clock drift
          .expiration(Date.from(Instant.ofEpochSecond(now + 540))) // iat+600s = 10 min max
          .issuer(config.getClientId())
          .signWith(privateKey, Jwts.SIG.RS256)
          .compact();

    } catch (Exception e) {
      throw new RuntimeException("Failed to generate GitHub JWT", e);
    }
  }

  private static String normalizePem(String raw) {
    if (raw == null) {
      return null;
    }
    String pem = raw.trim();
    // Railway / .env often stores PEM as a single line with literal \n
    if (pem.contains("\\n")) {
      pem = pem.replace("\\n", "\n");
    }
    return pem;
  }

  private PrivateKey getPrivateKey(String pemContent) throws Exception {
    boolean isPkcs1 = false;
    if (pemContent.contains("BEGIN RSA PRIVATE KEY")) {
      isPkcs1 = true;
      pemContent =
          pemContent
              .replace("-----BEGIN RSA PRIVATE KEY-----", "")
              .replace("-----END RSA PRIVATE KEY-----", "");
    } else {
      pemContent =
          pemContent
              .replace("-----BEGIN PRIVATE KEY-----", "")
              .replace("-----END PRIVATE KEY-----", "");
    }
    pemContent = pemContent.replaceAll("\\s", "");
    byte[] keyBytes = Base64.getDecoder().decode(pemContent);

    if (isPkcs1) {
      // Wrap PKCS#1 DER bytes in a PKCS#8 PrivateKeyInfo structure
      int n = keyBytes.length;
      byte[] pkcs8Bytes = new byte[26 + n];
      pkcs8Bytes[0] = 0x30; // SEQUENCE
      pkcs8Bytes[1] = (byte) 0x82;
      pkcs8Bytes[2] = (byte) (((22 + n) >> 8) & 0xFF);
      pkcs8Bytes[3] = (byte) ((22 + n) & 0xFF);
      pkcs8Bytes[4] = 0x02; // version = 0
      pkcs8Bytes[5] = 0x01;
      pkcs8Bytes[6] = 0x00;

      // AlgorithmIdentifier
      pkcs8Bytes[7] = 0x30; // SEQUENCE
      pkcs8Bytes[8] = 0x0d;
      // OID 1.2.840.113549.1.1.1 (rsaEncryption)
      byte[] oid = {
        0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01
      };
      System.arraycopy(oid, 0, pkcs8Bytes, 9, oid.length);
      pkcs8Bytes[20] = 0x05; // NULL
      pkcs8Bytes[21] = 0x00;

      // Octet String
      pkcs8Bytes[22] = 0x04; // OCTET STRING
      pkcs8Bytes[23] = (byte) 0x82;
      pkcs8Bytes[24] = (byte) ((n >> 8) & 0xFF);
      pkcs8Bytes[25] = (byte) (n & 0xFF);

      System.arraycopy(keyBytes, 0, pkcs8Bytes, 26, n);
      keyBytes = pkcs8Bytes;
    }

    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
    return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
  }
}
