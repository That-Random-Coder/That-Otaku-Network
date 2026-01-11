package com.project.content_service.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddCommentRequest {

    @NotNull
    private UUID contentId;

    @NotNull
    private UUID userId;

    @NotBlank
    private String userName;

    @NotBlank
    private String comment;
}
