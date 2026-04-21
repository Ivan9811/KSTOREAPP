package com.ucompensar.kstoreapp.UI.CLIENTE

data class PedidoClienteItem(
    val estado: String,
    val fecha: String,
    val servicio: String,
    val profesional: String,
    val precio: String,
    val fechaDetalle: String,
    val hora: String,
    val referencia: String,
    val cancelable: Boolean = true
)