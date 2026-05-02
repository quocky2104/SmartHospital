package com.example.SmartHospital.controller;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.SmartHospital.config.CustomUserDetails;
import com.example.SmartHospital.config.jwt.JwtProvider;
import com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests.LoginRequest;
import com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests.OtpVerificationRequest;
import com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests.RegisterRequest;
import com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests.ResetPasswordRequest;
import com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests.SendOtpRequest;
import com.example.SmartHospital.dtos.AuthDtos.Response.ApiResponse;
import com.example.SmartHospital.dtos.AuthDtos.Response.ResetTokenResponse;
import com.example.SmartHospital.dtos.AuthDtos.Response.TokenResponse;
import com.example.SmartHospital.dtos.AuthDtos.Response.VerifyTokenResponse;
import com.example.SmartHospital.model.User;
import com.example.SmartHospital.service.auth.ForgotPasswordService;
import com.example.SmartHospital.service.token.RedisTokenService;
import com.example.SmartHospital.service.user.CustomUserDetailsService;
import com.example.SmartHospital.service.user.UserService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController //for swagger
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpiration;

    private final UserService userService;
    private final ForgotPasswordService forgotPasswordService;
    private final JwtProvider jwtProvider;
    private final RedisTokenService redisTokenService;
    private final CustomUserDetailsService customUserDetailsService;
    private final AuthenticationManager authenticationManager;

    @Operation(
        summary = "Get current authenticated user",
        description = "Validate the current access token and return the authenticated user id and roles"
    )
    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(401, "Unauthorized: Authentication required. Please login first", null));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", authentication.getName());
        data.put("roles", authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList());
        return ResponseEntity.ok(new ApiResponse<>(200, "Authenticated", data));
    }

    @Operation(
        summary = "User login",
        description = "Authenticate user with email and password. Returns access token and sets refresh token in HTTP-only secure cookie"
    )
    @PostMapping("/login")
    public  ResponseEntity<ApiResponse<TokenResponse>> login(
        @RequestBody LoginRequest loginRequest,
        HttpServletResponse response
    ) {
        try{
            // UsernamePasswordAuthenticationToken(Object principal, Object credentials)
            // principal: who is trying to log in
            // credentials: password
            Authentication auth = authenticationManager.authenticate( // returns authenticated user with authorities
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );
            CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();  
            Map<String, Object> claims = Map.of(
                "userId", user.getId(),
                "email", user.getUsername(),
                "roles", user.getAuthorities().stream().map(a -> a.getAuthority()).toList()
            );

            // Access token is NOT stored because JWT is stateless and short-lived
            // It is only stored in Redis if revoked (blacklisted)
            String accessToken = jwtProvider.generateAccessToken(claims); 
            userService.updateLastLogin(user.getEmail());  
            // Refresh token is long-lived and MUST be stored server-side
            // so it can be revoked, rotated, and validated
            String refreshToken = jwtProvider.generateRefreshToken(claims);
            Claims refreshClaims = jwtProvider.getClaims(refreshToken);
            redisTokenService.storeRefreshToken(
                refreshClaims.get("email", String.class), 
                refreshClaims.getId(), 
                refreshToken, 
                Duration.ofMillis(refreshTokenExpiration)
            );        

            //Set httpOnly cookie
            String cookieValue = 
            "refreshToken=" + refreshToken      
            + "; HttpOnly"                       // JS cannot access this cookie (prevents XSS)
            + "; Secure"                         // Cookie is sent ONLY over HTTPS
            + "; SameSite=Strict"                // Prevents CSRF (cookie not sent cross-site)
            + "; Max-Age=" + refreshTokenExpiration / 1000; // Cookie lifetime (seconds)

            response.setHeader("Set-Cookie", cookieValue);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(new ApiResponse<>(200, "Login successful", new TokenResponse(accessToken)));
        } catch (BadCredentialsException | JwtException | IllegalArgumentException e) {
            log.error("Wrong credentials: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ApiResponse<>(401, "Login failed", null));
        } catch (RuntimeException e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponse<>(500, "Login failed", null));
        }
    }

    @Operation(
        summary = "User registration for patients",
        description = "Register a new patient account without file uploads"
    )
    @PostMapping(value = "/register")
    public  ResponseEntity<ApiResponse<TokenResponse>> register(
        @Valid @RequestBody RegisterRequest registerRequest,
        HttpServletResponse response
    ) {
        try {
            User user = userService.registerUser(registerRequest);
            if(user == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(400, "Registration failed", null));
            }
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(registerRequest.getEmail(), registerRequest.getPassword())
            );
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            Map<String, Object> claims = Map.of(
                "userId", userDetails.getId(),
                "email", userDetails.getUsername(),
                "roles", userDetails.getAuthorities().stream().map(a -> a.getAuthority()).toList()
            );
            String accessToken = jwtProvider.generateAccessToken(claims);
            userService.updateLastLogin(userDetails.getEmail());
            String refreshToken = jwtProvider.generateRefreshToken(claims);
            Claims refreshClaims = jwtProvider.getClaims(refreshToken);
            // Store refresh token in Redis
            redisTokenService.storeRefreshToken(
                refreshClaims.get("email", String.class), 
                refreshClaims.getId(), 
                refreshToken, 
                Duration.ofMillis(refreshTokenExpiration)
            );
            String cookieValue = 
            "refreshToken=" + refreshToken      
            + "; HttpOnly"                       // JS cannot access this cookie (prevents XSS)
            + "; Secure"                         // Cookie is sent ONLY over HTTPS
            + "; SameSite=Strict"                // Prevents CSRF (cookie not sent cross-site)
            + "; Max-Age=" + refreshTokenExpiration / 1000; // Cookie lifetime (seconds)
            response.setHeader("Set-Cookie", cookieValue);

            return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(201, "Registration successful", new TokenResponse(accessToken)));
        } catch(BadCredentialsException | JwtException | IllegalArgumentException e) {
            log.error("Wrong credentials: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ApiResponse<>(401, "Registration failed", null));
        }
        catch (RuntimeException e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponse<>(500, "Registration failed", null));
        }
    }

    @Operation(
        summary = "User logout",
        description = "Logout user by blacklisting access token, invalidating refresh token, and clearing HTTP-only cookie"
    )
    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> logout(
        HttpServletRequest request,
        HttpServletResponse response) {
        try {
            String authorizationHeader = request.getHeader("Authorization"); // Bearer token
            // blacklist access token until it expires (JWT is stateless so we can't delete it, we can only mark it as invalid in Redis)
            String token = authorizationHeader.replace("Bearer ", ""); // Remove "Bearer " to get token
            Claims claims = jwtProvider.getClaims(token);
            long expirationMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
            redisTokenService.blacklistToken(token, Duration.ofMillis(expirationMillis));
            // delete refresh token (logout, rotation)
            String email = claims.get("email", String.class);
            String jti = claims.getId();
            redisTokenService.deleteRefreshToken(email, jti);
            // delete httpOnly cookie so that browser will stop sending it (logout)
            response.setHeader("Set-Cookie", "refreshToken=deleted; HttpOnly; Secure; SameSite=Strict; Max-Age=0");
            return ResponseEntity.status(HttpStatus.OK)
            .body(new ApiResponse<>(200, "Logout successful", null));
        } catch (JwtException e) {
            log.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ApiResponse<>(401, "Logout failed", null));
        }
        catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponse<>(500, "Logout failed", null));
        }
    }

    // This endpoint is called to refresh access and refresh tokens 
    // This is neccessary because access tokens are short-lived
    // and refresh tokens need to be rotated for security
    @Operation(
        summary = "Refresh access and refresh tokens",
        description = "Generate new access token and rotate refresh token from HTTP-only cookie. Requires valid stored refresh token"
    )
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
        HttpServletRequest request,
        HttpServletResponse response) {
        String refreshToken = getRefreshTokenFromCookie(request);
        // If no refresh token cookie is found, return 401 Unauthorized to indicate that the user needs to log in again
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ApiResponse<>(401, "Refresh token not found", null));
        }
        try {
            Claims refreshClaims = jwtProvider.getClaims(refreshToken);
            String email = refreshClaims.get("email", String.class);
            // Validate refresh token against stored token in Redis
            String jti = refreshClaims.getId();
            if (!redisTokenService.isValidRefreshToken(email, jti, refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(401, "Invalid refresh token", null));
            }
            CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(email);

            // Generate new access and refresh tokens
            Map<String, Object> claims = Map.of(
                "userId", userDetails.getId(),
                "email", userDetails.getUsername(),
                "roles", userDetails.getAuthorities().stream().map(a -> a.getAuthority()).toList()
            );

            String accessToken = jwtProvider.generateAccessToken(claims);
            String newRefreshToken = jwtProvider.generateRefreshToken(claims);
            // Store new refresh token and delete old one
            redisTokenService.deleteRefreshToken(email, jti);
            Claims newRefreshClaims = jwtProvider.getClaims(newRefreshToken);
            redisTokenService.storeRefreshToken(
                email, 
                newRefreshClaims.getId(), 
                newRefreshToken, 
                Duration.ofMillis(refreshTokenExpiration)
            );
            // Set new refresh token in httpOnly cookie
            String cookieValue = 
                "refreshToken=" + newRefreshToken      
                + "; HttpOnly"                       // JS cannot access this cookie (prevents XSS)
                + "; Secure"                         // Cookie is sent ONLY over HTTPS
                + "; SameSite=Strict"                // Prevents CSRF (cookie not sent cross-site)
                + "; Max-Age=" + refreshTokenExpiration / 1000; // Cookie lifetime (seconds)
            response.setHeader("Set-Cookie", cookieValue);

            return ResponseEntity.status(HttpStatus.OK)
            .body(new ApiResponse<>(200, "Tokens refreshed successfully", new TokenResponse(accessToken)));
        } catch (JwtException e) {
            log.error("Failed to refresh tokens: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ApiResponse<>(401, "Failed to refresh tokens", null));
        }
    }
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (cookie.getName().equals("refreshToken")) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    @Operation(
        summary = "Send password reset OTP",
        description = "Send one-time password to user's email for password reset. OTP expires in 5 minutes and is queued via RabbitMQ for reliability"
    )
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<VerifyTokenResponse>> forgetPassword(@RequestBody @Valid SendOtpRequest request) {
        String email = request.getEmail();
        try {
            CompletableFuture<String> verifyToken = forgotPasswordService.sendOtp(email);
            return ResponseEntity.status(HttpStatus.OK)
            .body(new ApiResponse<>(200, "OTP sent successfully", new VerifyTokenResponse(verifyToken.join()))); // join() to get the token from CompletableFuture
        } catch (Exception e) {
            log.error("Failed to send password reset email: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponse<>(500, "Failed to send password reset email", null));
        }
    }

    @Operation(
        summary = "Verify password reset OTP",
        description = "Validate the OTP sent to user's email. Returns a temporary reset token valid for 15 minutes"
    )
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<ResetTokenResponse>> verifyOtp(@RequestBody OtpVerificationRequest request) {
        String token = request.getToken();
        String otp = request.getOtp();
        try {
            CompletableFuture<String> passwordResetToken = forgotPasswordService.verifyOtp(token, otp);
            return ResponseEntity.status(HttpStatus.OK)
            .body(new ApiResponse<>(200, "OTP verified successfully", new ResetTokenResponse(passwordResetToken.join())));
        } catch (Exception e) {
            log.error("OTP verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponse<>(500, "OTP verification failed", null));
        }
    }

    @Operation(
        summary = "Reset password",
        description = "Set a new password using the temporary reset token obtained after OTP verification. Token is valid for 15 minutes"
    )
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody ResetPasswordRequest request) {
        String token = request.getToken();
        String newPassword = request.getNewPassword();
        try {
            forgotPasswordService.resetPassword(token, newPassword);
            return ResponseEntity.status(HttpStatus.OK)
            .body(new ApiResponse<>(200, "Password reset successful! Please login with new password!", null));
        } catch (Exception e) {
            log.error("Password reset failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponse<>(500, "Password reset failed", null));
        }
    }
    
}
