package com.project.user_service.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRequestDto {

    @Pattern(
            regexp = "^[a-zA-Z0-9_]+$",
            message = "Username can only contain letters, digits, and underscores"
    )
    @Size(min = 6 , max = 15 , message = "GroupName length should be between 6 to 15")
    @NotBlank
    private String groupName;

    @NotBlank
    @Size(min = 10 , max = 160 , message = "Message length between 10 to 160")
    private String groupBio;

    @NonNull
    private UUID leaderId;
}
