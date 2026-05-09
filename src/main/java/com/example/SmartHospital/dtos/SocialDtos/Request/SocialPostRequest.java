package com.example.SmartHospital.dtos.SocialDtos.Request;
import lombok.Data;
import java.util.List;

@Data
public class SocialPostRequest {
    private String content;
    private List<String> imageUrls;    
}
