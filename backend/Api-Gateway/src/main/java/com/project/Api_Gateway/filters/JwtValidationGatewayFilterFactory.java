package com.project.api_gateway.filters;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.project.api_gateway.service.JwtService;
import lombok.AllArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class JwtValidationGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final JwtService jwtService;

    @Override
    public GatewayFilter apply(Object config) {
        return ((exchange, chain) -> {

            String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (token == null || !token.startsWith("Bearer ")) {
                throw new JWTDecodeException("Jwt Token");
            }

            token = token.substring(7);

            if (jwtService.verify(token)) {
                DecodedJWT decodedJWT = JWT.decode(token);
                String userId = decodedJWT.getSubject();
                String role = decodedJWT.getClaim("role").asString();

                if (userId == null || userId.isBlank() || role == null || role.isBlank()) {
                    throw new JWTDecodeException("Invalid token payload");
                }

                return chain.filter(exchange.mutate()
                        .request(r -> r
                                .header("User-Id", userId)
                                .header("User-Role", role))
                        .build());

            } else {
                throw new JWTDecodeException("Jwt Token");
            }
        });
    }
}
