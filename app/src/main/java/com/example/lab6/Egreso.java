package com.example.lab6;

import java.util.Date;

public class Egreso {
    private String id;
    private String titulo;
    private double monto;
    private String descripcion;
    private Date fecha;
    private String userId;
    private String comprobanteUrl;
    private String comprobantePublicId;
    private String comprobanteNombre;

    public Egreso() {

    }

    /*public Egreso(String titulo, double monto, String descripcion, Date fecha, String userId) {
        this.titulo = titulo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.userId = userId;
    }*/
    public Egreso(String titulo, double monto, String descripcion, Date fecha, String userId,
                  String comprobanteUrl, String comprobantePublicId, String comprobanteNombre) {
        this.titulo = titulo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.userId = userId;
        this.comprobanteUrl = comprobanteUrl;
        this.comprobantePublicId = comprobantePublicId;
        this.comprobanteNombre = comprobanteNombre;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Date getFecha() {
        return fecha;
    }
    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getComprobanteUrl() {
        return comprobanteUrl;
    }

    public void setComprobanteUrl(String comprobanteUrl) {
        this.comprobanteUrl = comprobanteUrl;
    }

    public String getComprobantePublicId() {
        return comprobantePublicId;
    }

    public void setComprobantePublicId(String comprobantePublicId) {
        this.comprobantePublicId = comprobantePublicId;
    }

    public String getComprobanteNombre() {
        return comprobanteNombre;
    }

    public void setComprobanteNombre(String comprobanteNombre) {
        this.comprobanteNombre = comprobanteNombre;
    }

    // Método de utilidad
    public boolean tieneComprobante() {
        return comprobanteUrl != null && !comprobanteUrl.isEmpty();
    }
}