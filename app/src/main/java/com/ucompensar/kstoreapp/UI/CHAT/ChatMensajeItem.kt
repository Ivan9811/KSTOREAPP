package com.ucompensar.kstoreapp.UI.CHAT

data class ChatMensajeItem(
    val id           : String,
    val conversacionId: String,
    val remitenteId  : String,
    val contenido    : String,
    val leido        : Boolean,
    val hora         : String,
    val esPropio     : Boolean // true = enviado, false = recibido
)
