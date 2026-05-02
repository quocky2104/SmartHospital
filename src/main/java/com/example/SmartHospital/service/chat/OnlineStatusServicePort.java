package com.example.SmartHospital.service.chat;

public interface OnlineStatusServicePort {
    void setOnline(String userId);
    void setOffline(String userId);
    boolean isOnline(String userId);
}
