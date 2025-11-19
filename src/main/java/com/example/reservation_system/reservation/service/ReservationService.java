package com.example.reservation_system.reservation.service;

import com.example.reservation_system.product.domain.Product;
import com.example.reservation_system.product.domain.ProductRepository;
import com.example.reservation_system.reservation.domain.Reservation;
import com.example.reservation_system.reservation.domain.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;
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
            executeReservationLogic(productId, userId);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Transactional
    public void executeReservationLogic(Long productId, Long userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        product.decreaseStock();

        Reservation reservation = new Reservation(userId, product);
        reservationRepository.save(reservation);
    }
}
