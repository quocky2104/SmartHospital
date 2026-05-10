package com.example.SmartHospital.dtos.SocialDtos.Request;
import lombok.Data;
import java.util.List;

@Data
public class SocialPostRequest {
    private String title;
    private String shortDescription;
    private String coverImage;
    private String content;
    private List<String> imageUrls;    
}
