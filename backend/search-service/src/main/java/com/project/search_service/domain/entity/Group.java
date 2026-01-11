package com.project.search_service.domain.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "groups")
public class Group {

    @Id
    private String id;

    @Field(name = "groupName", type = FieldType.Text)
    private String groupName;

    @Field(name = "bio", type = FieldType.Text, analyzer = "standard")
    private String bio;

    @Field(name = "leader_username", type = FieldType.Text)
    private String leaderUsername;

    @Field(name = "leader_display_name", type = FieldType.Text)
    private String leaderDisplayName;

    private Boolean enable;
}
