package com.hackathon.hackathon.controller;

import com.hackathon.hackathon.model.dto.request.SendChatMessageRequest;
import com.hackathon.hackathon.security.StompUserPrincipal;
import com.hackathon.hackathon.service.ChatService;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    @Autowired
    private ChatService chatService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendChatMessageRequest request, Principal principal) {
        if (!(principal instanceof StompUserPrincipal user)) {
            return;
        }
        chatService.sendMessage(user.getUserId(), request);
    }
}
