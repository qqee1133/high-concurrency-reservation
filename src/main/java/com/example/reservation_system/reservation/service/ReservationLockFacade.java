package com.example.reservation_system.reservation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ReservationLockFacade {

    private final ReservationService reservationService;
    private final StringRedisTemplate redisTemplate;

    private static final long LOCK_TIMEOUT_SECONDS = 3L;

    public void reserve(Long productId, Long userId) {
        String lockKey = "product:lock:" + productId;

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", Duration.ofSeconds(LOCK_TIMEOUT_SECONDS));

        if (Boolean.FALSE.equals(acquired)) {
            throw new RuntimeException("예약 락 획득에 실패했습니다.");
        }

        try {
            reservationService.reserve(productId, userId);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
}
