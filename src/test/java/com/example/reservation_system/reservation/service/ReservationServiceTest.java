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
    private ReservationLockFacade reservationLockFacade;

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
    @DisplayName("[3단계: Redis 분산 락] 1000명이 동시에 예약하면, 락을 획득한 소수만 성공하고 나머지는 즉시 실패한다.")
    void redis_distributed_lock_test() throws InterruptedException {

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
                    reservationLockFacade.reserve(productId, userId);
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
        System.out.println("[3단계] Redis 분산 락(Simple/SETNX) 결과");
        System.out.println("총 수행 시간: " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("Atomic 성공 건수: " + successCount.get());
        System.out.println("Atomic 실패 건수: " + failCount.get());
        System.out.println("----------------------------------------");
        System.out.println("DB 예약 건수 (count): " + reservationCount);
        System.out.println("DB 남은 재고 (stock): " + finalStock);
        System.out.println("========================================");

        assertThat(reservationCount).isEqualTo(successCount.get());

        assertThat(finalStock).isEqualTo(INITIAL_STOCK - successCount.get());
    }
}
