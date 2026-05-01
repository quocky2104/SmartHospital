package com.example.SmartHospital.service.user;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.SmartHospital.config.CustomUserDetails;
import com.example.SmartHospital.enums.UserStatus;
import com.example.SmartHospital.model.User;
import com.example.SmartHospital.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    // AuthenticationManager looks for a UserDetailsService
    @Override 
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if(user.getStatus() == UserStatus.DELETED) {
            throw new DisabledException("User is deleted");
        }
        // Convert user to CustomUserDetails
        return new CustomUserDetails(
            user.getId(),
            user.getEmail(),
            user.getHashedPassword(),
            user.getStatus() == UserStatus.ACTIVE,
            user.getRole()
        );
    }
}
