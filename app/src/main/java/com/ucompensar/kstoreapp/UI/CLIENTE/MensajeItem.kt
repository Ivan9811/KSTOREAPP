package com.ucompensar.kstoreapp.UI.CLIENTE.fragments

data class MensajeItem(
    val iniciales: String,
    val nombre: String,
    val ultimoMensaje: String,
    val hora: String,
    val online: Boolean = false,
    val noLeidos: Int = 0
)