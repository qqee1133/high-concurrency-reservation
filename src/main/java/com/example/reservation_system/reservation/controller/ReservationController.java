package com.example.reservation_system.reservation.controller;

import com.example.reservation_system.reservation.dto.ReservationRequest;
import com.example.reservation_system.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<Void> reserve(
            @RequestBody ReservationRequest request
    ) {
        reservationService.reserve(request.getProductId(), request.getUserId());
        return ResponseEntity.ok().build();
    }
}
