package com.concesionario.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;

    @CreatedDate
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fechaCreacion;

    private String nombreUser;
    private String apellidoUser;
    private String identificacionUser;
    private String correoUser;
    private String passwordUser;
    private Rol rol = Rol.USUARIO;
    private String resetPasswordToken;
    private LocalDateTime resetPasswordTokenExpiry;

    // NUEVOS CAMPOS PARA PREDICCIÓN (se calcularán automáticamente)
    private Integer cantidadCitas = 0;
    private Integer antiguedadCuenta = 0;
    private String estadoUltimaCita = "Pendiente";
    private String interesVehiculo = "No";
    private Integer tiempoEntreCitas = 0;

    // VARIABLES DE CALIFICACIÓN COMERCIAL Y FINANCIERA (Post-Cita)
    private String asistioCita = "No";
    private String tieneCuotaInicial = "No";
    private Double montoCuotaInicial = 0.0;
    private String capacidadEndeudamiento = "No";
    private String capacidadFinanciacion = "No";

    // RESULTADO DE PREDICCIÓN
    private String clientePotencial; // Si, No
    private Double probabilidad;
    private String observaciones;
    private String fcmToken;

    // Constructor vacío
    public Usuario() {
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombreUser() {
        return nombreUser;
    }

    public void setNombreUser(String nombreUser) {
        this.nombreUser = nombreUser;
    }

    public String getApellidoUser() {
        return apellidoUser;
    }

    public void setApellidoUser(String apellidoUser) {
        this.apellidoUser = apellidoUser;
    }

    public String getIdentificacionUser() {
        return identificacionUser;
    }

    public void setIdentificacionUser(String identificacionUser) {
        this.identificacionUser = identificacionUser;
    }

    public String getCorreoUser() {
        return correoUser;
    }

    public void setCorreoUser(String correoUser) {
        this.correoUser = correoUser;
    }

    public String getPasswordUser() {
        return passwordUser;
    }

    public void setPasswordUser(String passwordUser) {
        this.passwordUser = passwordUser;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public String getResetPasswordToken() {
        return resetPasswordToken;
    }

    public void setResetPasswordToken(String resetPasswordToken) {
        this.resetPasswordToken = resetPasswordToken;
    }

    public LocalDateTime getResetPasswordTokenExpiry() {
        return resetPasswordTokenExpiry;
    }

    public void setResetPasswordTokenExpiry(LocalDateTime resetPasswordTokenExpiry) {
        this.resetPasswordTokenExpiry = resetPasswordTokenExpiry;
    }

    // NUEVOS GETTERS Y SETTERS PARA PREDICCIÓN
    public Integer getCantidadCitas() {
        return cantidadCitas;
    }

    public void setCantidadCitas(Integer cantidadCitas) {
        this.cantidadCitas = cantidadCitas;
    }

    public Integer getAntiguedadCuenta() {
        return antiguedadCuenta;
    }

    public void setAntiguedadCuenta(Integer antiguedadCuenta) {
        this.antiguedadCuenta = antiguedadCuenta;
    }

    public String getEstadoUltimaCita() {
        return estadoUltimaCita;
    }

    public void setEstadoUltimaCita(String estadoUltimaCita) {
        this.estadoUltimaCita = estadoUltimaCita;
    }

    public String getInteresVehiculo() {
        return interesVehiculo;
    }

    public void setInteresVehiculo(String interesVehiculo) {
        this.interesVehiculo = interesVehiculo;
    }

    public Integer getTiempoEntreCitas() {
        return tiempoEntreCitas;
    }

    public void setTiempoEntreCitas(Integer tiempoEntreCitas) {
        this.tiempoEntreCitas = tiempoEntreCitas;
    }

    // Getters y Setters de variables financieras post-cita
    public String getAsistioCita() {
        return asistioCita;
    }

    public void setAsistioCita(String asistioCita) {
        this.asistioCita = asistioCita;
    }

    public String getTieneCuotaInicial() {
        return tieneCuotaInicial;
    }

    public void setTieneCuotaInicial(String tieneCuotaInicial) {
        this.tieneCuotaInicial = tieneCuotaInicial;
    }

    public Double getMontoCuotaInicial() {
        return montoCuotaInicial;
    }

    public void setMontoCuotaInicial(Double montoCuotaInicial) {
        this.montoCuotaInicial = montoCuotaInicial;
    }

    public String getCapacidadEndeudamiento() {
        return capacidadEndeudamiento;
    }

    public void setCapacidadEndeudamiento(String capacidadEndeudamiento) {
        this.capacidadEndeudamiento = capacidadEndeudamiento;
    }

    public String getCapacidadFinanciacion() {
        return capacidadFinanciacion;
    }

    public void setCapacidadFinanciacion(String capacidadFinanciacion) {
        this.capacidadFinanciacion = capacidadFinanciacion;
    }

    public String getClientePotencial() {
        return clientePotencial;
    }

    public void setClientePotencial(String clientePotencial) {
        this.clientePotencial = clientePotencial;
    }

    public Double getProbabilidad() {
        return probabilidad;
    }

    public void setProbabilidad(Double probabilidad) {
        this.probabilidad = probabilidad;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}