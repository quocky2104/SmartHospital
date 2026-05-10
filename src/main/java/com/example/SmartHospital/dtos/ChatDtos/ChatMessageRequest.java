package com.example.SmartHospital.dtos.ChatDtos;

import java.util.List;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private String receiverId;
    private String recipientId;
    private String messageText;
    private List<String> attachmentUrls;
}
