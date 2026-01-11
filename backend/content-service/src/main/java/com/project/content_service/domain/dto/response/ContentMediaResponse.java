package com.project.content_service.domain.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContentMediaResponse {

    private UUID id;

    private byte[] media;

    private String mediaType;
}
