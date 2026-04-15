package com.ucompensar.kstoreapp.UI.ADMIN

data class Usuario(
    val nombre: String,
    val email: String,
    val tipo: String,        // "Cliente" o "Prestador"
    val estado: String,      // "Activo", "Pendiente"
    val fechaRegistro: String,
    val pedidos: Int,
    val calificacion: Float,
    val iniciales: String    // "MG", "JR", etc.
)