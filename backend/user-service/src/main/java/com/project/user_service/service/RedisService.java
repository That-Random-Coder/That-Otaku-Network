package com.project.user_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisService {

    private RedisTemplate<String, Object> redisTemplate;
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public <T> T get(String key, Class<T> valueObjectType) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            log.info("User : {} is get" , key);
            return objectMapper.convertValue(value, valueObjectType);
        } catch (Exception e) {
            log.error("Redis service : {}", e.getMessage());
            return null;
        }
    }

    public void set(String key, Object value, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
            log.info("User : {} is saved" , key);
        } catch (Exception e) {
            log.error("Redis service : {}", e.getMessage());
        }
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
