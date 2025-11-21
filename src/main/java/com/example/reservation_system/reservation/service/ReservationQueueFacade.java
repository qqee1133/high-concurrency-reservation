package com.example.reservation_system.reservation.service;

import com.example.reservation_system.reservation.dto.ReservationRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationQueueFacade {

    private final StringRedisTemplate redisTemplate;
    private final ReservationService reservationService;
    private final ObjectMapper objectMapper;

    private static final String QUEUE_KEY = "reservation:queue";

    public void reserve(Long productId, Long userId) {
        try {
            ReservationRequest request = new ReservationRequest(productId, userId);
            String json = objectMapper.writeValueAsString(request);
            redisTemplate.opsForList().leftPush(QUEUE_KEY, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("요청을 큐에 저장하는 중 오류가 발생했습니다.", e);
        }
    }

    @Scheduled(fixedDelay = 10)
    public void processQueue() {
        String json = redisTemplate.opsForList().rightPop(QUEUE_KEY);

        if (json != null) {
            try {
                ReservationRequest request = objectMapper.readValue(json, ReservationRequest.class);
                reservationService.reserve(request.getProductId(), request.getUserId());

                log.info("예약 성공 (Async): User ID {}", request.getUserId());

            } catch (Exception e) {
                log.error("예약 처리 실패: {}", e.getMessage());
            }
        }
    }
}
