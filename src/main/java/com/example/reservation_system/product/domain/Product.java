package com.example.reservation_system.product.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    private static final long MIN_STOCK = 0L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private long stock;

    @Version
    private Long version;

    public Product(String name, long stock) {
        this.name = name;
        this.stock = stock;
    }

    public void decreaseStock() {
        if (this.stock <= MIN_STOCK) {
            throw new RuntimeException("재고가 소진되었습니다.");
        }
        this.stock -= 1;
    }
}
