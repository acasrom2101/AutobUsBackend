package com.autobus.backend_pagos.service;

import com.autobus.backend_pagos.dto.PaymentRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        // Inicializamos la librería de Stripe con tu clave secreta
        Stripe.apiKey = stripeApiKey;
    }

    public String createPaymentIntent(PaymentRequest request) throws StripeException, ExecutionException, InterruptedException {
        // 1. Calculamos el precio total en el servidor
    	List<String> rutasIds = request.getRutasIds();
        long montoTotalCents = calcularPrecioTotalDesdeFirebase(rutasIds);

        // 2. Creamos los parámetros para Stripe
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(montoTotalCents) // Stripe requiere el importe SIEMPRE en céntimos
                .setCurrency("eur")
                .putAllMetadata(Map.of(
                		"rutas_compradas", String.join(",", rutasIds),
                		"usuario_id", request.getUsuarioId() != null ? request.getUsuarioId() : "invitado",
                		"email_comprador", request.getEmail() != null ? request.getEmail() : "sin_email"
                ))
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        // 3. Hacemos la petición a Stripe para crear la intención de pago
        PaymentIntent intent = PaymentIntent.create(params);

        // 4. Devolvemos el secreto que el frontend necesita para cargar el formulario
        return intent.getClientSecret();
    }

    private long calcularPrecioTotalDesdeFirebase(List<String> rutasIds) throws ExecutionException, InterruptedException {
        long totalCents = 0;
        DatabaseReference rutasRef = FirebaseDatabase.getInstance().getReference("lineas"); // Ajusta el nombre del nodo si es "rutas" o "lineas"

        for (String rutaId : rutasIds) {
            // Usamos CompletableFuture para convertir la llamada asíncrona de Firebase en síncrona
            CompletableFuture<Double> precioFuturo = new CompletableFuture<>();

            rutasRef.child(rutaId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Intentamos obtener el precio. Si no existe, por defecto 0.0
                        Double precio = snapshot.child("precio").getValue(Double.class);
                        precioFuturo.complete(precio != null ? precio : 0.0);
                    } else {
                        System.err.println("La ruta " + rutaId + " no existe en Firebase.");
                        precioFuturo.complete(0.0);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    System.err.println("Error al consultar Firebase: " + error.getMessage());
                    precioFuturo.complete(0.0); // O podrías lanzar una excepción
                }
            });

            // Esperamos a que Firebase responda para esta línea
            Double precioEuros = precioFuturo.get();
            
            // Convertimos euros a céntimos (ej. 1.50 * 100 = 150)
            totalCents += Math.round(precioEuros * 100);
        }

        // Medida de seguridad: mínimo 50 céntimos exigido por Stripe
        return totalCents >= 50 ? totalCents : 50;
    }
}