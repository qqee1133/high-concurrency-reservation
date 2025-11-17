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
    @DisplayName("[1단계 증명] @Transactional 환경에서는 초과 예약이 발생한다.")
    void proveRaceCondition() throws InterruptedException {

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
        System.out.println("[1단계] 단순 @Transactional 결과");
        System.out.println("총 수행 시간: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("Atomic 성공 건수: " + successCount.get());
        System.out.println("Atomic 실패 건수: " + failCount.get());
        System.out.println("----------------------------------------");
        System.out.println("DB 예약 건수 (count): " + reservationCount);
        System.out.println("DB 남은 재고 (stock): " + finalStock);
        System.out.println("========================================");

        assertThat(reservationCount)
                .as("초과 예약이 발생해야 Race Condition이 증명됨 (기대: 100 초과)")
                .isGreaterThan(INITIAL_STOCK);

        assertThat(finalStock)
                .as("재고가 0이 되어야 함 (단, 예약 건수는 100을 초과해야 함)")
                .isEqualTo(0);
    }
}
