package com.autobus.backend_pagos.controller;

import com.autobus.backend_pagos.service.EmailService;
import com.autobus.backend_pagos.service.TicketService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @Autowired
    private TicketService ticketService;
    
    @Autowired
    private EmailService emailService;

    // A diferencia de otros endpoints, Stripe necesita el cuerpo de la petición como un String crudo (RAW)
    // para poder verificar la firma criptográfica correctamente.
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            // Verificamos que la petición viene realmente de Stripe y no de un atacante
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            System.err.println("⚠️ Alerta: Firma del Webhook inválida.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Firma inválida");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error en el payload");
        }

        // Manejamos el evento específico de "Pago completado"
        if ("payment_intent.succeeded".equals(event.getType())) {
            
            // Extraemos el objeto PaymentIntent del evento
            StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (stripeObject instanceof PaymentIntent paymentIntent) {
                
                String paymentIntentId = paymentIntent.getId();
                Long amount = paymentIntent.getAmount();
                
                // Recuperamos los metadatos que pusimos en el Paso 1
                String rutasCompradas = "Desconocida";
                if (paymentIntent.getMetadata() != null && paymentIntent.getMetadata().containsKey("rutas_compradas")) {
                    rutasCompradas = paymentIntent.getMetadata().get("rutas_compradas");
                }
                
                String usuarioId = "desconocido";
                if (paymentIntent.getMetadata() != null && paymentIntent.getMetadata().containsKey("usuario_id")) {
                    usuarioId = paymentIntent.getMetadata().get("usuario_id");
                }
                
                String emailComprador = "sin_email";
                if (paymentIntent.getMetadata() != null && paymentIntent.getMetadata().containsKey("email_comprador")) {
                    emailComprador = paymentIntent.getMetadata().get("email_comprador");
                }

                System.out.println("💰 Pago recibido con éxito! Generando billete para la ruta: " + rutasCompradas);
                
                // Llamamos a nuestro servicio para guardar en Firebase
                ticketService.generarYGuardarBillete(paymentIntentId, rutasCompradas, amount, usuarioId);
                
                if (!emailComprador.equals("sin_email") && emailComprador.contains("@")) {
                    emailService.enviarCorreoBillete(emailComprador, paymentIntentId);
                }
            }
        } else {
            // Ignoramos otros eventos (como payment_intent.created, etc.)
            System.out.println("Evento no manejado: " + event.getType());
        }

        // Siempre debemos devolver un status 200 a Stripe para decirle "Mensaje recibido",
        // de lo contrario, Stripe seguirá intentando enviarlo durante días.
        return ResponseEntity.ok("");
    }
}