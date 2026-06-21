package com.hackathon.hackathon.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.hackathon.model.dto.request.CreateChatRoomRequest;
import com.hackathon.hackathon.model.dto.request.OpenChatRoomRequest;
import com.hackathon.hackathon.model.dto.response.ChatMessageResponse;
import com.hackathon.hackathon.model.dto.response.ChatRoomResponse;
import com.hackathon.hackathon.service.ChatService;



@RestController
@RequestMapping(value = "/api/chat", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
public class ChatController {

  @Autowired private ChatService chatService;

  @PostMapping("/rooms")
  public ResponseEntity<ChatRoomResponse> createRoom(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody CreateChatRoomRequest request) {
    return ResponseEntity.ok(chatService.createRoom(authHeader, request));
  }

  @PostMapping("/rooms/open")
  public ResponseEntity<ChatRoomResponse> openRoom(
      @RequestHeader("Authorization") String authHeader, @RequestBody OpenChatRoomRequest request) {
    return ResponseEntity.ok(chatService.openRoom(authHeader, request));
  }

  @GetMapping("/rooms")
  public ResponseEntity<List<ChatRoomResponse>> listRooms(
      @RequestHeader("Authorization") String authHeader,
      @RequestParam(value = "eventId", required = false) String eventId,
      @RequestParam(value = "roundId", required = false) String roundId) {
    return ResponseEntity.ok(chatService.listRooms(authHeader, eventId, roundId));
  }

  @GetMapping("/rooms/{roomId}")
  public ResponseEntity<ChatRoomResponse> getRoom(
      @RequestHeader("Authorization") String authHeader, @PathVariable("roomId") String roomId) {
    return ResponseEntity.ok(chatService.getRoomDetail(authHeader, roomId));
  }

  @GetMapping("/rooms/{roomId}/messages")
  public ResponseEntity<List<ChatMessageResponse>> getMessages(
      @RequestHeader("Authorization") String authHeader, @PathVariable("roomId") String roomId) {
    return ResponseEntity.ok(chatService.getRoomMessages(authHeader, roomId));
  }
}
