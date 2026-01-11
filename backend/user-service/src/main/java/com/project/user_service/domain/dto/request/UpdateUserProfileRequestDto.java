package com.project.user_service.domain.dto.request;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserProfileRequestDto {

    private String displayName;
    private String bio;
    private String location;
    private LocalDate dateOfBirth;
}

