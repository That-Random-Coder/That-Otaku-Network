package com.project.content_service.domain.dto.request;

import com.project.content_service.domain.enums.Category;
import com.project.content_service.domain.enums.Genre;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateContentRequest {

    private String title;

    private Set<Category> animeCategories;

    private Set<Genre> genre;

    private String bio;
}
