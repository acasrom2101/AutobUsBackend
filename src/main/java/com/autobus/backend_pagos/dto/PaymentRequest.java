package com.autobus.backend_pagos.dto;

import java.util.List;

public class PaymentRequest {
    private List<String> rutasIds; // Angular nos enviará los IDs de las líneas (ej. ["linea_1", "linea_4"])
    private String usuarioId;

    public List<String> getRutasIds() {
        return rutasIds;
    }

    public void setRutasIds(List<String> rutasIds) {
        this.rutasIds = rutasIds;
    }
    
    public String getUsuarioId() { 
    	return usuarioId; 
    }
    
    public void setUsuarioId(String usuarioId) { 
    	this.usuarioId = usuarioId; 
    }
    
    private String email;

    public String getEmail() { 
    	return email; 
    }
    
    public void setEmail(String email) {
    	this.email = email;
    }
}