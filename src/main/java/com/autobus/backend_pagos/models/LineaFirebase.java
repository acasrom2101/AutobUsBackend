package com.autobus.backend_pagos.models;

public class LineaFirebase {
	
	private String nombre;
    private Double precio;

    public LineaFirebase() {}

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }
}