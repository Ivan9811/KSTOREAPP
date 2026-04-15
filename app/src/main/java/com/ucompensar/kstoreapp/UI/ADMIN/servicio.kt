package com.ucompensar.kstoreapp.UI.ADMIN

data class Servicio(
    val nombre: String,
    val prestador: String,
    val precio: String,
    val estado: String,      // "Activo", "Revision", "Reportado"
    val calificacion: Float,
    val resenas: Int,
    val reservas: String,    // "342 reservas" o "Nuevo"
    val reporte: String = "" // solo si estado == "Reportado"
)