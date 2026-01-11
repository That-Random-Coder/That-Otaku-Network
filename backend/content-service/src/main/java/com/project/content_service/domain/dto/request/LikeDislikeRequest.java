package com.project.content_service.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LikeDislikeRequest {

    @NotNull
    private UUID contentId;

    @NotNull
    private UUID userId;
}
