package com.project.auth_service.service;

import com.project.auth_service.domain.dtos.*;
import com.project.auth_service.domain.entity.RefreshToken;
import com.project.auth_service.domain.entity.UserProfile;
import com.project.auth_service.domain.enums.EventTypeNotification;
import com.project.auth_service.domain.enums.Roles;
import com.project.auth_service.exception.customException.*;
import com.project.auth_service.repository.RefreshTokenRepo;
import com.project.auth_service.repository.UserProfileRepository;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.security.auth.login.AccountNotFoundException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class AuthControllerService {

    private final UserProfileRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisService redisService;
    private final NotificationService notificationService;
    private final JwtService jwtService;
    private final RefreshTokenRepo refreshTokenRepo;
    private final AuthenticationManager authenticationManager;

    private static final String SIGN_PREFIX = "SIGN_";
    private static final String RESET_PREFIX = "RESET_";

    /* ========================= SIGN UP ========================= */

    @Transactional
    public SignUpResponseDto signUpUser(SignUpRequestDto requestDto) {
        try{
            UserProfile user = UserProfile.builder()
                    .email(requestDto.getEmail())
                    .username(requestDto.getUsername())
                    .roles(Roles.USER)
                    .enabled(false)
                    .createdAt(LocalDateTime.now())
                    .password(passwordEncoder.encode(requestDto.getPassword()))
                    .build();

            userRepository.save(user);

            int verificationNumber = emailVerification(user.getEmail() , user.getUsername(), user.getId() , SIGN_PREFIX , EventTypeNotification.EMAIL_VERIFICATION);
            log.info("User with username : {} and email : {} is Sign up and verification code : {}" , user.getUsername() , user.getEmail() , verificationNumber);

            return SignUpResponseDto.builder()
                    .email(requestDto.getEmail())
                    .username(user.getUsername())
                    .id(user.getId())
                    .build();

        }catch (DataIntegrityViolationException exception){
            throw new UsernameOrEmailAlreadyExistsException(requestDto.getEmail() + " & " + requestDto.getUsername());
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }
    /* ========================= EMAIL VERIFICATION ========================= */

    public int emailVerification(String email, String username, UUID id, String prefix , EventTypeNotification typeNotification) {
        SecureRandom random = new SecureRandom();
        Integer code = 100_000 + random.nextInt(900_000);

        redisService.set(prefix + id, code, 300);
        notificationService.createEmailVerificationNotification(email, username, code.toString() , typeNotification);

        return code;
    }

    @Transactional
    public TokenVerificationResponseDto verifyCode(TokenVerificationRequestDto requestDto) {

        Integer redisCode = redisService.get(
                SIGN_PREFIX + requestDto.getId(),
                Integer.class
        );

        if (redisCode == null) {
            throw new VerificationCodeExpiredException("Verification code expired or invalid");
        }

        if (!redisCode.equals(requestDto.getCode())) {
            throw new InvalidVerificationCodeException("Invalid verification code");
        }

        UserProfile user = userRepository.findById(requestDto.getId())
                .orElseThrow(() -> new UserNotFoundException(requestDto.getId().toString()));

        user.setEnabled(true);
        redisService.delete(SIGN_PREFIX + user.getId());

        TokenVerificationResponseDto tokens =
                jwtService.getTokens(user.getId(), user.getRoles().toString());

        refreshTokenRepo.save(
                RefreshToken.builder()
                        .refreshToken(tokens.getRefreshToken())
                        .expireDate(tokens.getTimeStampRefreshToken())
                        .enable(true)
                        .userProfile(user)
                        .build()
        );

        try{
            notificationService.createEmailVerificationNotification(user.getEmail(), user.getUsername() , " " , EventTypeNotification.EMAIL_WELCOME);
        }catch (Exception e){
            log.error(e.getMessage());
        }
        return tokens;
    }

    /* ========================= LOGIN ========================= */

    @Transactional
    public LoginResponseDto logIn(String identifier, String password) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, password)
        );

        if (authentication.getPrincipal() == null) {
            throw new AuthenticationCredentialsNotFoundException(identifier);
        }

        UserDetail userDetails = (UserDetail) authentication.getPrincipal();
        UserProfile user = userDetails.getUser();

        refreshTokenRepo.disableAllByUserId(user.getId());

        TokenVerificationResponseDto tokens =
                jwtService.getTokens(user.getId(), user.getRoles().toString());

        refreshTokenRepo.save(
                RefreshToken.builder()
                        .refreshToken(tokens.getRefreshToken())
                        .expireDate(tokens.getTimeStampRefreshToken())
                        .enable(true)
                        .userProfile(user)
                        .build()
        );

        return LoginResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .AccessToken(tokens.getAccessToken())
                .TimeStampAccessToken(tokens.getTimeStampAccessToken())
                .RefreshToken(tokens.getRefreshToken())
                .TimeStampRefreshToken(tokens.getTimeStampRefreshToken())
                .build();
    }

    /* ========================= PASSWORD RESET ========================= */

    @Transactional
    public void passwordResetEmail(String identifier) {
        UserProfile user = userRepository.findByUsernameOrEmail(identifier, identifier);

        if (user == null) {
            throw new UserNotFoundException(identifier);
        }

        if (redisService.get(RESET_PREFIX + user.getId(), Integer.class) != null) {
            throw new EmailAlreadySendException("password reset email");
        }

        emailVerification(user.getEmail(), user.getUsername(), user.getId(), RESET_PREFIX , EventTypeNotification.EMAIL_VERIFICATION);
    }

    @Transactional
    public LoginResponseDto passwordResetLogin(PasswordResetLoginDto requestDto) {

        UserProfile user = userRepository
                .findByUsernameOrEmail(requestDto.getIdentifier(), requestDto.getIdentifier());

        if (user == null) {
            throw new UserNotFoundException(requestDto.getIdentifier());
        }

        Integer redisCode = redisService.get(
                RESET_PREFIX + user.getId(),
                Integer.class
        );

        if (redisCode == null) {
            throw new VerificationCodeExpiredException("Verification code expired or invalid");
        }

        if (!redisCode.equals(requestDto.getVerificationCode())) {
            throw new InvalidVerificationCodeException("Invalid verification code");
        }

        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        redisService.delete(RESET_PREFIX + user.getId());

        refreshTokenRepo.disableAllByUserId(user.getId());

        TokenVerificationResponseDto tokens =
                jwtService.getTokens(user.getId(), user.getRoles().toString());

        refreshTokenRepo.save(
                RefreshToken.builder()
                        .refreshToken(tokens.getRefreshToken())
                        .expireDate(tokens.getTimeStampRefreshToken())
                        .enable(true)
                        .userProfile(user)
                        .build()
        );

        return LoginResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .AccessToken(tokens.getAccessToken())
                .TimeStampAccessToken(tokens.getTimeStampAccessToken())
                .RefreshToken(tokens.getRefreshToken())
                .TimeStampRefreshToken(tokens.getTimeStampRefreshToken())
                .build();
    }

    /* ========================= ACCESS TOKEN ========================= */

    public TokenResponseDto getAccessToken(String refreshToken, UUID userId) {

        TokenInfo token;
        try {
            token = jwtService.extractClaim(refreshToken);
        } catch (JwtException ex) {
            throw new ExpireOrWrongRefreshTokenException("Invalid or expired refresh token");
        }

        if (!token.getId().equals(userId.toString())) {
            throw new ExpireOrWrongRefreshTokenException("Refresh token does not match user");
        }

        RefreshToken entity = refreshTokenRepo
                .findByRefreshTokenAndEnableTrue(refreshToken)
                .orElseThrow(() -> new ExpireOrWrongRefreshTokenException("Refresh token not found"));

        if (entity.getExpireDate().before(new Date())) {
            throw new ExpireOrWrongRefreshTokenException("Refresh token expired");
        }

        return jwtService.createAccessToken(userId, token.getRoles());
    }
}
