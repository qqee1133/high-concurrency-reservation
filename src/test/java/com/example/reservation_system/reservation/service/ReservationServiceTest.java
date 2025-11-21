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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StopWatch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReservationServiceTest {

    @Autowired
    private ReservationQueueFacade reservationQueueFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final long INITIAL_STOCK = 100L;
    private Long productId;

    @BeforeEach
    void setUp() {
        Product savedProduct = productRepository.save(new Product("테스트 티켓", INITIAL_STOCK));
        productId = savedProduct.getId();
        redisTemplate.delete("reservation:queue");
    }

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        redisTemplate.delete("reservation:queue");
    }

    @Test
    @DisplayName("[4단계: Redis 메시지 큐] 1000명이 동시에 요청해도, 큐에 담아두고 순차적으로 처리하므로 정합성이 보장된다.")
    void redis_queue_test() throws InterruptedException {

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
                    reservationQueueFacade.reserve(productId, userId);
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

        System.out.println("========================================");
        System.out.println("[4단계] Redis 메시지 큐 (API 접수 속도)");
        System.out.println("총 수행 시간 (API): " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("접수 성공 건수: " + successCount.get());
        System.out.println("접수 실패 건수: " + failCount.get());
        System.out.println("========================================");

        System.out.println("Consumer가 큐를 처리하는 동안 대기합니다... (약 10초)");
        Thread.sleep(10000);

        long reservationCount = reservationRepository.count();
        long finalStock = productRepository.findById(productId).get().getStock();

        System.out.println("========================================");
        System.out.println("[4단계] 최종 처리 결과 (DB)");
        System.out.println("DB 예약 건수 (count): " + reservationCount);
        System.out.println("DB 남은 재고 (stock): " + finalStock);
        System.out.println("========================================");

        assertThat(successCount.get()).isEqualTo(1000);

        assertThat(reservationCount).isEqualTo(INITIAL_STOCK);
        assertThat(finalStock).isEqualTo(0L);
    }
}
