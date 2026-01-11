package com.project.recommendation_service.domain.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KafkaDto {

    private String TopicType;
    private Map<String , Object> map;

}
