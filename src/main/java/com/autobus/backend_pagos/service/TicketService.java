package com.autobus.backend_pagos.service;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class TicketService {

    public void generarYGuardarBillete(String paymentIntentId, String rutasCompradas, Long montoPagado, String usuarioId) {
        // Obtenemos la referencia al nodo "billetes" en Firebase
        DatabaseReference billetesRef = FirebaseDatabase.getInstance().getReference("billetes");
        DatabaseReference lineasRef = FirebaseDatabase.getInstance().getReference("lineas");
        
        // Generamos un ID único para el billete en Firebase
        String ticketId = billetesRef.push().getKey();
        
        // Generamos un texto único para el código QR
        String qrData = "TICKET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        String[] ids = rutasCompradas.split(",");
        ArrayList<Map<String, String>> rutasCompradasList = new ArrayList<>();
        
        new Thread(() -> {
            for (String id : ids) {
                CompletableFuture<String> nombreFuturo = new CompletableFuture<>();
                
                // Buscamos solo el nombre de esa línea específica en Firebase
                lineasRef.child(id).child("nombre").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            nombreFuturo.complete(snapshot.getValue(String.class));
                        } else {
                            nombreFuturo.complete("Línea Desconocida");
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                        nombreFuturo.complete("Error de lectura");
                    }
                });

                try {
                    String nombreLinea = nombreFuturo.get(); // Esperamos a que Firebase responda
                    
                    // Creamos el subnodo {id: "x", nombre: "y"}
                    Map<String, String> rutaInfo = new HashMap<>();
                    rutaInfo.put("id", id);
                    rutaInfo.put("nombre", nombreLinea);
                    rutasCompradasList.add(rutaInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Ya tenemos todos los nombres. Preparamos el billete final:
            Map<String, Object> billeteData = new HashMap<>();
            billeteData.put("id_compra", paymentIntentId);
            billeteData.put("usuario_id", usuarioId); // Guardamos de quién es
            billeteData.put("rutas", rutasCompradasList); // Guardamos el array estructurado
            billeteData.put("precio", montoPagado / 100.0);
            billeteData.put("codigo_qr_data", qrData);
            billeteData.put("estado", "valido");
            billeteData.put("timestamp", ServerValue.TIMESTAMP); // Timestamp oficial de Firebase

            if (ticketId != null) {
                billetesRef.child(ticketId).setValueAsync(billeteData);
                System.out.println("✅ Billete generado y guardado en Firebase con ID: " + ticketId);
            }
        }).start();
    }
}