package com.ucompensar.kstoreapp.UI

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.CHAT.ChatAdapter
import com.ucompensar.kstoreapp.UI.CHAT.ChatMensajeItem
import com.ucompensar.kstoreapp.process.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private lateinit var rvMensajes    : RecyclerView
    private lateinit var etMensaje     : EditText
    private lateinit var btnEnviar     : ImageButton
    private lateinit var tvNombreChat  : TextView
    private lateinit var btnVolver     : ImageView
    private lateinit var adapter       : ChatAdapter

    private var conversacionId : String = ""
    private var otroNombre     : String = ""
    private var otroId         : String = ""
    private var uid            : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        conversacionId = intent.getStringExtra("conversacion_id") ?: ""
        otroNombre     = intent.getStringExtra("otro_nombre")     ?: "Chat"
        otroId         = intent.getStringExtra("otro_id")         ?: ""

        rvMensajes   = findViewById(R.id.rvMensajes)
        etMensaje    = findViewById(R.id.etMensaje)
        btnEnviar    = findViewById(R.id.btnEnviar)
        tvNombreChat = findViewById(R.id.tvNombreChat)
        btnVolver    = findViewById(R.id.btnVolver)

        tvNombreChat.text = otroNombre

        adapter = ChatAdapter(mutableListOf())
        rvMensajes.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMensajes.adapter = adapter

        btnVolver.setOnClickListener { finish() }
        btnEnviar.setOnClickListener { enviarMensaje() }

        lifecycleScope.launch {
            uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: ""
            if (conversacionId.isEmpty()) {
                conversacionId = obtenerOCrearConversacion()
            }
            cargarMensajes()
            iniciarPolling()
        }
    }

    // ── Obtener o crear conversación ──────────────────────────
    private suspend fun obtenerOCrearConversacion(): String {
        return try {

            // Obtener perfil actual
            val profileActual = SupabaseClient.client.postgrest
                .from("profiles")
                .select {
                    filter { eq("id", uid) }
                }
                .decodeSingle<Map<String, JsonElement>>()

            val rolActual = profileActual["rol"]
                ?.jsonPrimitive
                ?.content
                ?.lowercase() ?: ""

            // Determinar quién es cliente y quién profesional
            val clienteId: String
            val profesionalId: String

            if (rolActual == "cliente") {
                clienteId = uid
                profesionalId = otroId
            } else {
                clienteId = otroId
                profesionalId = uid
            }

            // Buscar conversación existente
            val existentes = SupabaseClient.client.postgrest
                .from("conversaciones")
                .select {
                    filter {
                        eq("cliente_id", clienteId)
                        eq("profesional_id", profesionalId)
                    }
                }
                .decodeList<Map<String, JsonElement>>()

            if (existentes.isNotEmpty()) {
                existentes.first()["id"]?.jsonPrimitive?.content ?: ""
            } else {

                // Crear conversación correctamente
                val nueva = SupabaseClient.client.postgrest
                    .from("conversaciones")
                    .insert(
                        buildJsonObject {
                            put("cliente_id", clienteId)
                            put("profesional_id", profesionalId)
                            put("ultimo_mensaje", "")
                        }
                    )
                    .decodeSingle<Map<String, JsonElement>>()

                nueva["id"]?.jsonPrimitive?.content ?: ""
            }

        } catch (e: Exception) {
            Log.e("CHAT", "Error conversación: ${e.message}", e)
            ""
        }
    }

    // ── Cargar mensajes ───────────────────────────────────────
    private suspend fun cargarMensajes() {
        try {
            if (conversacionId.isEmpty()) return

            val mensajesRaw = SupabaseClient.client.postgrest
                .from("mensajes")
                .select {
                    filter { eq("conversacion_id", conversacionId) }
                }
                .decodeList<Map<String, JsonElement>>()

            val lista = mensajesRaw.map { m ->
                val remitenteId = m["remitente_id"]?.jsonPrimitive?.content ?: ""
                val createdAt   = m["created_at"]?.jsonPrimitive?.content ?: ""
                ChatMensajeItem(
                    id             = m["id"]?.jsonPrimitive?.content ?: "",
                    conversacionId = conversacionId,
                    remitenteId    = remitenteId,
                    contenido      = m["contenido"]?.jsonPrimitive?.content ?: "",
                    leido          = m["leido"]?.jsonPrimitive?.content == "true",
                    hora           = formatearHora(createdAt),
                    esPropio       = remitenteId == uid
                )
            }

            adapter.actualizar(lista)
            if (lista.isNotEmpty()) {
                rvMensajes.scrollToPosition(lista.size - 1)
            }

            // Marcar mensajes como leídos
            marcarComoLeidos()

        } catch (e: Exception) {
            Log.e("CHAT", "Error cargar mensajes: ${e.message}", e)
        }
    }

    // ── Enviar mensaje ────────────────────────────────────────
    private fun enviarMensaje() {
        val contenido = etMensaje.text.toString().trim()
        if (contenido.isEmpty()) return
        if (conversacionId.isEmpty()) {
            Toast.makeText(this, "Error: sin conversación", Toast.LENGTH_SHORT).show()
            return
        }

        etMensaje.setText("")

        lifecycleScope.launch {
            try {
                // Insertar mensaje
                SupabaseClient.client.postgrest
                    .from("mensajes")
                    .insert(buildJsonObject {
                        put("conversacion_id", conversacionId)
                        put("remitente_id",    uid)
                        put("contenido",       contenido)
                        put("leido",           false)
                    })

                // Actualizar último mensaje en conversación
                SupabaseClient.client.postgrest
                    .from("conversaciones")
                    .update(buildJsonObject {
                        put("ultimo_mensaje",    contenido)
                        put("ultimo_mensaje_at", java.time.Instant.now().toString())
                    }) { filter { eq("id", conversacionId) } }

                cargarMensajes()

            } catch (e: Exception) {
                Log.e("CHAT", "Error enviando: ${e.message}", e)
                Toast.makeText(this@ChatActivity,
                    "Error al enviar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Marcar mensajes como leídos ───────────────────────────
    private suspend fun marcarComoLeidos() {
        try {
            SupabaseClient.client.postgrest
                .from("mensajes")
                .update(buildJsonObject { put("leido", true) }) {
                    filter {
                        eq("conversacion_id", conversacionId)
                        eq("leido", false)
                        neq("remitente_id", uid)
                    }
                }
        } catch (e: Exception) {
            Log.e("CHAT", "Error marcando leídos: ${e.message}", e)
        }
    }

    // ── Polling cada 5 segundos ───────────────────────────────
    private fun iniciarPolling() {
        lifecycleScope.launch {
            while (isActive) {
                delay(5000)
                cargarMensajes()
            }
        }
    }

    // ── Helper hora ───────────────────────────────────────────
    private fun formatearHora(fecha: String): String {
        return try {
            val instant = java.time.Instant.parse(fecha)
            val local   = java.time.ZoneId.systemDefault()
            val ldt     = java.time.LocalDateTime.ofInstant(instant, local)
            String.format("%02d:%02d", ldt.hour, ldt.minute)
        } catch (_: Exception) { "" }
    }
}