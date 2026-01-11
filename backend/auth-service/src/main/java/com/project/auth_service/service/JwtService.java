package com.project.auth_service.service;

import com.project.auth_service.domain.dtos.TokenInfo;
import com.project.auth_service.domain.dtos.TokenResponseDto;
import com.project.auth_service.domain.dtos.TokenVerificationResponseDto;
import com.project.auth_service.domain.enums.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {
    @Value("${security.jwt.expiration}")
    private long expirationMsAccess;

    @Value("${security.jwt.refresh-expiration}")
    private long expirationMsRefresh;

    @Value("${SECURITY_JWT_SECRET}")
    private String secret;

    public String generateToken(UUID id , String role , long expireTime , String type){
        Map<String , String> claims = new HashMap<>();
        claims.put("role" , role);
        claims.put("type" , type);

        return Jwts.builder()
                .claims(claims)
                .subject(id.toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expireTime))
                .signWith(key())
                .compact();
    }

    public TokenVerificationResponseDto getTokens(UUID id , String role){
        String AccessToken = createAccessToken(id , role).getToken();
        Date TimeStampAccessToken = new Date(System.currentTimeMillis() + expirationMsAccess);
        String RefreshToken = createRefreshToken(id , role);
        Date TimeStampRefreshToken = new Date(System.currentTimeMillis() + expirationMsRefresh);

        return TokenVerificationResponseDto
                .builder()
                .AccessToken(AccessToken)
                .TimeStampAccessToken(TimeStampAccessToken)
                .RefreshToken(RefreshToken)
                .TimeStampRefreshToken(TimeStampRefreshToken)
                .build();

    }

    public TokenResponseDto createAccessToken(UUID id , String role){
       String token =  generateToken(id , role , expirationMsAccess , TokenType.ACCESS.toString());
       Date expireAt = new Date(System.currentTimeMillis() + expirationMsAccess);

       return TokenResponseDto
               .builder()
               .token(token)
               .expireAt(expireAt)
               .build();
    }

    public String createRefreshToken(UUID id , String role ){
        return generateToken(id , role , expirationMsRefresh , TokenType.REFRESH.toString());
    }

    public SecretKey key(){
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractAllClaims(String token){
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public TokenInfo extractClaim(String token){
        final Claims claims = extractAllClaims(token);
        TokenInfo tokenInfo = new TokenInfo();
        tokenInfo.setId(claims.getSubject());
        tokenInfo.setRoles(claims.get("role", String.class));
        tokenInfo.setExpirationAt(claims.getExpiration());
        tokenInfo.setTokenType(claims.get("type" , String.class));

        return tokenInfo;

    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
