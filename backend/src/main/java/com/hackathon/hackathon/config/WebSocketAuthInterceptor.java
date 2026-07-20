package com.hackathon.hackathon.config;

import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.exception.ForbiddenException;
import com.hackathon.hackathon.exception.UnauthorizedException;
import com.hackathon.hackathon.repository.ChatRepository;
import com.hackathon.hackathon.security.JwtUtil;
import com.hackathon.hackathon.security.StompUserPrincipal;
import io.jsonwebtoken.Claims;
import java.security.Principal;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

  private static final String CHAT_TOPIC_PREFIX = "/topic/chat/";

  private final ChatRepository chatRepository;

  public WebSocketAuthInterceptor(ChatRepository chatRepository) {
    this.chatRepository = chatRepository;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null) {
      return message;
    }

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      return handleConnect(accessor, message);
    }

    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
      handleSubscribe(accessor);
    }

    return message;
  }

  private Message<?> handleConnect(StompHeaderAccessor accessor, Message<?> message) {
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

  private void handleSubscribe(StompHeaderAccessor accessor) {
    String destination = accessor.getDestination();
    if (destination == null || !destination.startsWith(CHAT_TOPIC_PREFIX)) {
      return;
    }

    Principal user = accessor.getUser();
    if (!(user instanceof StompUserPrincipal principal)) {
      throw new UnauthorizedException("Invalid or missing token.");
    }

    String roomId = destination.substring(CHAT_TOPIC_PREFIX.length()).trim();
    if (roomId.isEmpty()) {
      throw new BadRequestException("Invalid chat subscription destination.");
    }

    if (!chatRepository.isRoomMember(roomId, principal.getUserId())) {
      throw new ForbiddenException("Not a member of this chat room.");
    }
  }
}
