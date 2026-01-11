package com.project.content_service.domain.dto.response;

import com.project.content_service.domain.enums.Category;
import com.project.content_service.domain.enums.Genre;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContentResponse {

    private UUID id;

    private String title;

    private Set<Category> animeCategories;

    private Set<Genre> genre;

    private String bio;

    private Boolean enable;

    private LocalDateTime created;
}
