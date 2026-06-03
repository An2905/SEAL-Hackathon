package com.hackathon.hackathon.service;

import com.hackathon.hackathon.repository.ChatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ChatRoomCloseScheduler {

    @Autowired
    private ChatRepository chatRepository;

    @Scheduled(fixedRate = 60000)
    public void closeExpiredRooms() {
        chatRepository.closeExpiredRooms();
    }
}
