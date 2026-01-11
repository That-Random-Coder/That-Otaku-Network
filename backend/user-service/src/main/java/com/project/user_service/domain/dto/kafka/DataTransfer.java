package com.project.user_service.domain.dto.kafka;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataTransfer {
    private String type;
    private Map<String , String> map;
}
