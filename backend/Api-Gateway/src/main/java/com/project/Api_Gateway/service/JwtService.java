package com.project.api_gateway.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.project.api_gateway.enums.TokenType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final String secret;
    private final JWTVerifier verifier;

    public JwtService(@Value("${security.jwt.secret}") String secret) {
        this.secret = secret;
        Algorithm algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).build();
    }

    public boolean verify(String token) {
        try {
            verifier.verify(token);
            DecodedJWT decodedJWT = JWT.decode(token);
            return decodedJWT.getClaim("type").asString().equals(TokenType.ACCESS.toString());
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    public DecodedJWT decode(String token) {
        return JWT.decode(token);
    }

    public String getUserId(String token) {
        DecodedJWT decodedJWT = JWT.decode(token);
        return decodedJWT.getSubject();
    }

    public String getUserRole(String token) {
        DecodedJWT decodedJWT = JWT.decode(token);
        return decodedJWT.getClaim("role").asString();
    }
}
