package com.project.content_service.domain.dto.request;

import com.project.content_service.domain.enums.Category;
import com.project.content_service.domain.enums.Genre;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateContentRequest {

    @NotBlank
    private String title;

    @NotNull
    private Set<Category> animeCategories;

    @NotNull
    private Set<Genre> genres;

    @NotNull
    private Set<String> tags;

    @NotBlank
    private String bio;

    @NotNull
    private UUID userID;

    @NotBlank
    private String userName;

    @NotBlank
    private String displayName;
}
