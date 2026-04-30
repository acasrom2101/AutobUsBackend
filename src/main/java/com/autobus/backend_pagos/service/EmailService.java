package com.autobus.backend_pagos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarCorreoBillete(String destinatario, String paymentIntentId) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(destinatario);
            message.setSubject("Tu billete de autobús - Compra Confirmada");
            
            String texto = "¡Gracias por tu compra!\n\n" +
                           "Tu pago se ha procesado correctamente. Puedes ver, descargar y presentar " +
                           "tu billete (con su código QR) entrando en el siguiente enlace:\n\n" +
                           "http://localhost:4200/pago-completado?payment_intent=" + paymentIntentId + "\n\n" +
                           "¡Buen viaje!";
            
            message.setText(texto);
            mailSender.send(message);
            System.out.println("📧 Correo enviado con éxito a: " + destinatario);
            
        } catch (Exception e) {
            System.err.println("❌ Error al enviar el correo: " + e.getMessage());
        }
    }
}