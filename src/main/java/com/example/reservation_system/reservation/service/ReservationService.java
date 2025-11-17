package com.example.reservation_system.reservation.service;

import com.example.reservation_system.product.domain.Product;
import com.example.reservation_system.product.domain.ProductRepository;
import com.example.reservation_system.reservation.domain.Reservation;
import com.example.reservation_system.reservation.domain.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;

    // 2-2단계 낙관적 락 적용
    @Transactional
    public void reserve(Long productId, Long userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        product.decreaseStock();

        Reservation reservation = new Reservation(userId, product);
        reservationRepository.save(reservation);
    }
}
