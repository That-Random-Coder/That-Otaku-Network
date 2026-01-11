package com.project.search_service.domain.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "users")
public class Users {

    @Id
    private String id;

    @Field(name = "username", type = FieldType.Text)
    private String username;

    @Field(name = "display_name", type = FieldType.Text, analyzer = "standard")
    private String displayName;

    @Field(name = "bio", type = FieldType.Text, analyzer = "standard")
    private String bio;

    private Boolean enable;
}
