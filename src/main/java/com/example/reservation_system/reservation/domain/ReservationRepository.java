package com.example.reservation_system.reservation.domain;

import com.example.reservation_system.reservation.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
