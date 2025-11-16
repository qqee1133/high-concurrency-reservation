package com.example.reservation_system.product.domain;

import com.example.reservation_system.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
