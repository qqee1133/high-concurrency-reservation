package com.example.reservation_system.reservation.service;

import com.example.reservation_system.product.domain.Product;
import com.example.reservation_system.product.domain.ProductRepository;
import com.example.reservation_system.reservation.domain.ReservationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private static final long INITIAL_STOCK = 100L;
    private Long productId;

    @BeforeEach
    void setUp() {
        Product savedProduct = productRepository.save(new Product("테스트 티켓", INITIAL_STOCK));
        productId = savedProduct.getId();
    }

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("[2-2단계: 낙관적 락] 1000명이 동시에 100개의 재고를 예약하면, 충돌로 인해 1건만 성공한다.")
    void optimistic_lock_test() throws InterruptedException {

        int threadCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        for (int i = 0; i < threadCount; i++) {
            long userId = i + 1;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    reservationService.reserve(productId, userId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        stopWatch.stop();

        long reservationCount = reservationRepository.count();
        long finalStock = productRepository.findById(productId).get().getStock();

        System.out.println("========================================");
        System.out.println("[2-2단계] 낙관적 락(@Version) 결과");
        System.out.println("총 수행 시간: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("Atomic 성공 건수: " + successCount.get());
        System.out.println("Atomic 실패 건수: " + failCount.get());
        System.out.println("----------------------------------------");
        System.out.println("DB 예약 건수 (count): " + reservationCount);
        System.out.println("DB 남은 재고 (stock): " + finalStock);
        System.out.println("========================================");

        assertThat(reservationCount)
                .as("낙관적 락으로 인해 100건만 예약되어야 함")
                .isEqualTo(INITIAL_STOCK);

        assertThat(finalStock)
                .as("낙관적 락으로 100건만 차감되어 재고는 0이어야 함")
                .isEqualTo(0L);
    }
}
