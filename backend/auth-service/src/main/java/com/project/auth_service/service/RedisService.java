package com.project.auth_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@AllArgsConstructor
public class RedisService {

    private final RedisTemplate redisTemplate;

    public <T> T get(String key, Class<T> responseClass) {
        try {
            Object o = redisTemplate.opsForValue().get(key);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(o.toString(), responseClass);
        } catch (Exception e) {
            log.error("Redis service : {}", e.getMessage());
            return null;
        }
    }

    public void set(String key, Object o, long Ttl) {
        try {
            redisTemplate.opsForValue().set(key, o.toString(), Ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis service : {}", e.getMessage());
        }
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

}
