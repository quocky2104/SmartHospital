package com.example.SmartHospital.service.chat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.example.SmartHospital.dtos.ChatDtos.ChatConversationDTO;
import com.example.SmartHospital.dtos.ChatDtos.ChatMessageRequest;
import com.example.SmartHospital.dtos.ChatDtos.ChatMessageResponse;
import com.example.SmartHospital.enums.MessageStatus;
import com.example.SmartHospital.model.Doctor;
import com.example.SmartHospital.model.DoctorPatientMessages;
import com.example.SmartHospital.model.Patient;
import com.example.SmartHospital.repository.DoctorPatientChatRepository;
import com.example.SmartHospital.repository.DoctorRepository;
import com.example.SmartHospital.repository.PatientRepository;
import com.example.SmartHospital.repository.UserRepository;
import com.example.SmartHospital.service.messaging.WebSocketMessagingService;
import com.example.SmartHospital.service.storage.MinioStorageService;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class ChatService {
    private final DoctorPatientChatRepository chatRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final OnlineStatusServicePort onlineStatusService;
    private final WebSocketMessagingService websocketMessagingService;
    private final MinioStorageService minioStorageService;

    public ChatMessageResponse createMessage(String senderId, ChatMessageRequest request, UsernamePasswordAuthenticationToken authentication) {
        String receiverId = request.getReceiverId();
        boolean isDoctorSender = authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_DOCTOR"));

        Doctor doctor;
        Patient patient;
        String senderName = userRepository.findById(senderId)
            .map(user -> user.getFullName())
            .orElse("Unknown Sender");
        if(isDoctorSender){
            doctor = doctorRepository.findById(senderId)
            .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
            patient = patientRepository.findById(receiverId)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        } else {
            patient = patientRepository.findById(senderId)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
            doctor = doctorRepository.findById(receiverId)
            .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        }
        DoctorPatientMessages message = new DoctorPatientMessages();
        message.setDoctor(doctor);
        message.setPatient(patient);
        message.setMessageText(request.getMessageText());
        message.setSenderId(senderId);
        message.setAttachments(request.getAttachmentUrls());
        message.setStatus(MessageStatus.SENT);

        ChatMessageResponse response = convertToResponse(chatRepository.save(message), senderName);
        websocketMessagingService.sendMessageToUser(senderId, receiverId, response);
        websocketMessagingService.sendMessageToUser(senderId, senderId, response);
        return response;

    }

    public List<ChatConversationDTO> getAllChats(String userId, Authentication authentication) {
        // Determine if the user is a doctor or patient based on their roles
        boolean isDoctor = authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_DOCTOR"));
        List<DoctorPatientMessages> messages;
        if(isDoctor){
            messages = chatRepository.findByDoctorId(userId);
        } else {
            messages = chatRepository.findByPatientId(userId);
        }
        return messages.stream()
            .collect(Collectors.groupingBy(msg -> {
                // Group by the other participant's ID (doctor or patient)
                return isDoctor ? msg.getPatient().getId() : msg.getDoctor().getId();
            }))
            .entrySet().stream()
            .map(entry -> {
                String otherUserId = entry.getKey();
                List<DoctorPatientMessages> groupMessages = entry.getValue();
                DoctorPatientMessages lastMessage = groupMessages.get(groupMessages.size() - 1);
                String otherUserName = isDoctor ? 
                    groupMessages.get(0).getPatient().getFullName() : 
                    groupMessages.get(0).getDoctor().getFullName();
                ChatConversationDTO conversation = new ChatConversationDTO();
                conversation.setConversationId(
                    isDoctor?
                    userId + "_" + otherUserId :
                    otherUserId + "_" + userId
                );
                conversation.setOtherUserId(otherUserId);
                conversation.setOtherUserName(otherUserName);
                conversation.setLastMessage(lastMessage.getMessageText());
                conversation.setLastMessageTimestamp(lastMessage.getTimestamp());
                conversation.setOnlineStatus(onlineStatusService.isOnline(otherUserId));
                conversation.setUnreadMessageCount(0);
                return conversation;
            })
            .sorted(Comparator.comparing(ChatConversationDTO::getLastMessageTimestamp).reversed()) // Sort by last message timestamp
            .toList();
    }
    private ChatMessageResponse convertToResponse(DoctorPatientMessages message, String senderName) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setDoctorId(message.getDoctor().getId());
        response.setDoctorName(message.getDoctor().getFullName());
        response.setPatientId(message.getPatient().getId());
        response.setPatientName(message.getPatient().getFullName());
        response.setMessageText(message.getMessageText());
        response.setSenderId(message.getSenderId());
        response.setSenderName(senderName);
        response.setTimestamp(message.getTimestamp());
        response.setStatus(message.getStatus());
        response.setAttachmentUrls(resolveAttachmentUrls(message.getAttachments()));
        return response;
    }

    // Resolves attachment URLs by checking if they are already full URLs or 
    // if they need to be converted to presigned URLs using MinioStorageService
    // so that the frontend can directly use them to display or download attachments
    private List<String> resolveAttachmentUrls(List<String> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }

        List<String> resolvedUrls = new ArrayList<>(attachments.size());
        for (String attachment : attachments) {
            if (attachment == null || attachment.isBlank()) {
                continue;
            }

            String trimmed = attachment.trim();
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                resolvedUrls.add(trimmed);
            } else {
                resolvedUrls.add(minioStorageService.toPresignedGetUrl(trimmed));
            }
        }

        return resolvedUrls;
    }

    public List<ChatMessageResponse> getSpecificChat(String userId, String otherUserId, Authentication authentication) {
        // Determine if the user is a doctor or patient based on their roles
        boolean isDoctor = authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_DOCTOR"));
        
        String doctorId = isDoctor ? userId : otherUserId;
        String patientId = isDoctor ? otherUserId : userId;

        List<DoctorPatientMessages> messages = chatRepository.findByDoctor_IdAndPatient_IdOrderByTimestampAsc(doctorId, patientId);
        return messages.stream()
            .map(msg -> {
                String senderName = isDoctor ? 
                msg.getDoctor().getFullName() : msg.getPatient().getFullName();
                return convertToResponse(msg, senderName);
            })
            .toList();
    }
}
