package com.hackathon.hackathon.config;

import com.hackathon.hackathon.exception.UnauthorizedException;
import com.hackathon.hackathon.security.JwtUtil;
import com.hackathon.hackathon.security.StompUserPrincipal;
import io.jsonwebtoken.Claims;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
      return message;
    }

    String authHeader = accessor.getFirstNativeHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new UnauthorizedException("Invalid or missing token.");
    }

    Claims claims = JwtUtil.extractClaims(authHeader.substring(7));
    String userId = claims.get("userId", String.class);
    String fullName = claims.get("fullName", String.class);
    if (userId == null || userId.isBlank()) {
      throw new UnauthorizedException("Invalid token.");
    }

    accessor.setUser(
        new StompUserPrincipal(userId.trim(), fullName != null ? fullName : userId.trim()));
    return message;
  }
}
