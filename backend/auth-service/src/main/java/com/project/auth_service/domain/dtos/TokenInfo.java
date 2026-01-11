package com.project.auth_service.domain.dtos;

import lombok.*;

import java.util.Date;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenInfo {
    private String id;
    private String roles;
    private Date expirationAt;
    private String tokenType;
}
