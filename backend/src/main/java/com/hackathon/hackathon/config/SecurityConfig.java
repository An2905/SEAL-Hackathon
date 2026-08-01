package com.hackathon.hackathon.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.hackathon.model.dto.response.ErrorResponse;
import com.hackathon.hackathon.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final ObjectMapper objectMapper;

  public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.objectMapper = objectMapper;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    // CSRF protection is disabled because this is a stateless REST API using header-based JWT
    // authentication. OTP flows still use HttpSession cookies with SameSite=None in production.
    // CodeQL [java/spring-disabled-csrf-protection]
    http.csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.POST, "/api/auth/login")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/register/otp")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/register")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/password/reset-otp")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/password/reset")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/events")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/universities/all")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/auth/github/callback")
                    .permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                        (request, response, authException) ->
                            writeJsonError(
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "Invalid or missing token."))
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) ->
                            writeJsonError(
                                response, HttpStatus.FORBIDDEN, "Forbidden access.")))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  private void writeJsonError(HttpServletResponse response, HttpStatus status, String message)
      throws java.io.IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    ErrorResponse body = new ErrorResponse(status.value(), message);
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }

  @Bean
  public static ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
