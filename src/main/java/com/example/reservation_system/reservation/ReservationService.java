package com.example.reservation_system.reservation;

import com.example.reservation_system.product.Product;
import com.example.reservation_system.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;

    // 1단계 문제 증명을 위한 동시성 제어가 없는 단순 재고 차감 로직
    @Transactional
    public void reserve(Long productId, Long userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        product.decreaseStock();

        Reservation reservation = new Reservation(userId, product);
        reservationRepository.save(reservation);
    }
}
