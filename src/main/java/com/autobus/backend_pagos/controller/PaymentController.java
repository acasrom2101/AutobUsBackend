package com.autobus.backend_pagos.controller;

import com.autobus.backend_pagos.dto.PaymentRequest;
import com.autobus.backend_pagos.dto.PaymentResponse;
import com.autobus.backend_pagos.service.StripeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
@CrossOrigin(origins = "*") 
public class PaymentController {

    @Autowired
    private StripeService stripeService;

    @GetMapping("/status")
    public String checkStatus() {
        return "¡El backend de pagos de autobuses está funcionando correctamente!";
    }

    // NUEVO ENDPOINT
    @PostMapping("/create-payment-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody PaymentRequest request) {
        try {
            // Validamos que nos envíen rutas
            if (request.getRutasIds() == null || request.getRutasIds().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El carrito está vacío"));
            }

            // Llamamos a nuestro servicio
            String clientSecret = stripeService.createPaymentIntent(request);

            // Devolvemos el JSON de éxito
            return ResponseEntity.ok(new PaymentResponse(clientSecret));

        } catch (Exception e) {
            System.err.println("Error al crear el PaymentIntent: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al procesar el pago"));
        }
    }
}