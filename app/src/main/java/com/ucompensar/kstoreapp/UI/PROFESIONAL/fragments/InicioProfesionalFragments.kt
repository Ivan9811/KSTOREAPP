package com.ucompensar.kstoreapp.UI.PROFESIONAL.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.process.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class InicioProfesionalFragments : Fragment() {

    private lateinit var tvNombreBanner   : TextView
    private lateinit var tvIngresos       : TextView
    private lateinit var tvTrabajos       : TextView
    private lateinit var tvCalificacion   : TextView
    private lateinit var tvDisponibilidad : TextView
    private lateinit var tvBadge          : TextView

    private lateinit var tvSolicitud1Titulo  : TextView
    private lateinit var tvSolicitud1Cliente : TextView
    private lateinit var tvSolicitud1Detalle : TextView
    private lateinit var btnAceptar1         : Button
    private lateinit var btnRechazar1        : Button

    private lateinit var tvEnCursoTitulo  : TextView
    private lateinit var tvEnCursoCliente : TextView
    private lateinit var progressEnCurso  : ProgressBar
    private lateinit var btnChatEnCurso   : Button

    private var pedidoPendienteId : String? = null
    private var clienteEnCursoId  : String? = null  // ✅ para abrir chat

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_inicio_profesional, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvNombreBanner   = view.findViewById(R.id.tvNombreBanner)
        tvIngresos       = view.findViewById(R.id.tvIngresos)
        tvTrabajos       = view.findViewById(R.id.tvTrabajos)
        tvCalificacion   = view.findViewById(R.id.tvCalificacion)
        tvDisponibilidad = view.findViewById(R.id.tvDisponibilidad)
        tvBadge          = view.findViewById(R.id.tvBadge)

        tvSolicitud1Titulo  = view.findViewById(R.id.tvSolicitud1Titulo)
        tvSolicitud1Cliente = view.findViewById(R.id.tvSolicitud1Cliente)
        tvSolicitud1Detalle = view.findViewById(R.id.tvSolicitud1Detalle)
        btnAceptar1         = view.findViewById(R.id.btnAceptar1)
        btnRechazar1        = view.findViewById(R.id.btnRechazar1)

        tvEnCursoTitulo  = view.findViewById(R.id.tvEnCursoTitulo)
        tvEnCursoCliente = view.findViewById(R.id.tvEnCursoCliente)
        progressEnCurso  = view.findViewById(R.id.progressEnCurso)
        btnChatEnCurso   = view.findViewById(R.id.btnChatEnCurso)

        btnAceptar1.setOnClickListener  { responderPedido(pedidoPendienteId, "aceptado") }
        btnRechazar1.setOnClickListener { responderPedido(pedidoPendienteId, "rechazado") }

        // ✅ Chat con el cliente del trabajo en curso
        btnChatEnCurso.setOnClickListener { abrirChatEnCurso() }

        cargarDatos()
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
    }

    // ── Cargar todos los datos ────────────────────────────────────────────────
    private fun cargarDatos() {
        lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                    ?: return@launch

                val profile = SupabaseClient.client.postgrest
                    .from("profiles")
                    .select { filter { eq("id", uid) } }
                    .decodeSingle<Map<String, JsonElement>>()

                val nombre = profile["nombre"]?.jsonPrimitive?.content ?: "Profesional"
                tvNombreBanner.text = nombre

                try {
                    val prof = SupabaseClient.client.postgrest
                        .from("profesionales")
                        .select { filter { eq("id", uid) } }
                        .decodeSingle<Map<String, JsonElement>>()

                    val calificacion = prof["calificacion"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    tvCalificacion.text = String.format("%.1f", calificacion)

                    val disponibilidad = prof["disponibilidad"]
                    tvDisponibilidad.text =
                        if (disponibilidad != null && disponibilidad is JsonArray && disponibilidad.size > 0)
                            "🟢" else "🔴"

                } catch (e: Exception) {
                    Log.e("INICIO_PROF", "Sin datos en profesionales: ${e.message}")
                    tvCalificacion.text   = "0.0"
                    tvDisponibilidad.text = "🔴"
                }

                cargarSolicitudesPendientes(uid)
                cargarPedidoEnCurso(uid)
                cargarKpisMes(uid)

            } catch (e: Exception) {
                Log.e("INICIO_PROF", "Error cargarDatos: ${e.message}", e)
            }
        }
    }

    // ── Solicitudes pendientes ────────────────────────────────────────────────
    private suspend fun cargarSolicitudesPendientes(uid: String) {
        try {
            val pedidos = SupabaseClient.client.postgrest
                .from("pedidos")
                .select { filter { eq("profesional_id", uid); eq("estado", "pendiente") } }
                .decodeList<Map<String, JsonElement>>()

            val cantidad = pedidos.size
            tvBadge.text       = if (cantidad > 0) cantidad.toString() else ""
            tvBadge.visibility = if (cantidad > 0) View.VISIBLE else View.GONE

            if (pedidos.isNotEmpty()) {
                val pedido        = pedidos.first()
                pedidoPendienteId = pedido["id"]?.jsonPrimitive?.content

                val clienteId     = pedido["cliente_id"]?.jsonPrimitive?.content ?: ""
                val clienteNombre = obtenerNombreCliente(clienteId)

                val servicioId    = pedido["servicio_id"]?.jsonPrimitive?.content ?: ""
                val servicioNombre = obtenerNombreServicio(servicioId)

                tvSolicitud1Titulo.text  = servicioNombre
                tvSolicitud1Cliente.text = "Cliente: $clienteNombre"
                tvSolicitud1Detalle.text = pedido["nota"]?.jsonPrimitive?.content ?: ""

                btnAceptar1.isEnabled  = true
                btnRechazar1.isEnabled = true
            } else {
                pedidoPendienteId        = null
                tvSolicitud1Titulo.text  = "Sin solicitudes nuevas"
                tvSolicitud1Cliente.text = ""
                tvSolicitud1Detalle.text = ""
                btnAceptar1.isEnabled    = false
                btnRechazar1.isEnabled   = false
            }
        } catch (e: Exception) {
            Log.e("INICIO_PROF", "Error solicitudes: ${e.message}", e)
        }
    }

    // ── Pedido en curso ───────────────────────────────────────────────────────
    private suspend fun cargarPedidoEnCurso(uid: String) {
        try {
            val enCurso = SupabaseClient.client.postgrest
                .from("pedidos")
                .select { filter { eq("profesional_id", uid); eq("estado", "aceptado") } }
                .decodeList<Map<String, JsonElement>>()

            if (enCurso.isNotEmpty()) {
                val pedido        = enCurso.first()
                val clienteId     = pedido["cliente_id"]?.jsonPrimitive?.content ?: ""
                val clienteNombre = obtenerNombreCliente(clienteId)
                val servicioId    = pedido["servicio_id"]?.jsonPrimitive?.content ?: ""
                val servicioNombre = obtenerNombreServicio(servicioId)

                clienteEnCursoId = clienteId  // ✅ guardar para el chat

                tvEnCursoTitulo.text  = servicioNombre
                tvEnCursoCliente.text = "Cliente: $clienteNombre"
                progressEnCurso.progress = 50
                btnChatEnCurso.isEnabled = true
            } else {
                clienteEnCursoId         = null
                tvEnCursoTitulo.text     = "Sin trabajos en curso"
                tvEnCursoCliente.text    = ""
                progressEnCurso.progress = 0
                btnChatEnCurso.isEnabled = false
            }
        } catch (e: Exception) {
            Log.e("INICIO_PROF", "Error en curso: ${e.message}", e)
        }
    }

    // ── KPIs del mes ─────────────────────────────────────────────────────────
    private suspend fun cargarKpisMes(uid: String) {
        try {
            val completados = SupabaseClient.client.postgrest
                .from("pedidos")
                .select { filter { eq("profesional_id", uid); eq("estado", "completado") } }
                .decodeList<Map<String, JsonElement>>()

            tvTrabajos.text = completados.size.toString()

            var totalIngresos = 0.0
            for (pedido in completados) {
                val servicioId = pedido["servicio_id"]?.jsonPrimitive?.content ?: continue
                try {
                    val servicio = SupabaseClient.client.postgrest
                        .from("servicios")
                        .select { filter { eq("id", servicioId) } }
                        .decodeSingle<Map<String, JsonElement>>()
                    totalIngresos += servicio["precio"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                } catch (_: Exception) {}
            }
            tvIngresos.text = String.format("$%,.0f", totalIngresos)

        } catch (e: Exception) {
            Log.e("INICIO_PROF", "Error KPIs: ${e.message}", e)
            tvTrabajos.text = "0"
            tvIngresos.text = "$0"
        }
    }

    // ✅ Fix: usar buildJsonObject en vez de mapOf (que no funciona con Supabase Kotlin)
    private fun responderPedido(pedidoId: String?, nuevoEstado: String) {
        if (pedidoId == null) return
        lifecycleScope.launch {
            try {
                SupabaseClient.client.postgrest
                    .from("pedidos")
                    .update(buildJsonObject { put("estado", nuevoEstado) }) {
                        filter { eq("id", pedidoId) }
                    }

                Toast.makeText(
                    requireContext(),
                    if (nuevoEstado == "aceptado") "Solicitud aceptada ✓" else "Solicitud rechazada",
                    Toast.LENGTH_SHORT
                ).show()

                cargarDatos()

            } catch (e: Exception) {
                Log.e("INICIO_PROF", "Error responder pedido: ${e.message}", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ Abrir chat con el cliente del trabajo en curso
    private fun abrirChatEnCurso() {
        val clienteId = clienteEnCursoId ?: return
        lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                    ?: return@launch

                val conv = SupabaseClient.client.postgrest
                    .from("conversaciones")
                    .select { filter {
                        eq("profesional_id", uid)
                        eq("cliente_id",     clienteId)
                    }}
                    .decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()

                val convId = if (conv.isNotEmpty()) {
                    conv.first()["id"]?.jsonPrimitive?.content ?: ""
                } else {
                    val nueva = SupabaseClient.client.postgrest
                        .from("conversaciones")
                        .insert(buildJsonObject {
                            put("profesional_id", uid)
                            put("cliente_id",     clienteId)
                            put("ultimo_mensaje", "")
                        })
                        .decodeSingle<Map<String, kotlinx.serialization.json.JsonElement>>()
                    nueva["id"]?.jsonPrimitive?.content ?: ""
                }

                val clienteNombre = obtenerNombreCliente(clienteId)

                startActivity(
                    android.content.Intent(requireContext(),
                        com.ucompensar.kstoreapp.UI.ChatActivity::class.java).apply {
                        putExtra("conversacion_id", convId)
                        putExtra("otro_nombre",     clienteNombre)
                        putExtra("otro_id",         clienteId)
                    }
                )
            } catch (e: Exception) {
                Log.e("INICIO_PROF", "Error abriendo chat en curso: ${e.message}", e)
                android.widget.Toast.makeText(requireContext(),
                    "Error al abrir chat", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private suspend fun obtenerNombreCliente(clienteId: String): String {
        return try {
            val cliente = SupabaseClient.client.postgrest
                .from("profiles")
                .select { filter { eq("id", clienteId) } }
                .decodeSingle<Map<String, JsonElement>>()
            cliente["nombre"]?.jsonPrimitive?.content ?: "Cliente"
        } catch (_: Exception) { "Cliente" }
    }

    private suspend fun obtenerNombreServicio(servicioId: String): String {
        return try {
            val servicio = SupabaseClient.client.postgrest
                .from("servicios")
                .select { filter { eq("id", servicioId) } }
                .decodeSingle<Map<String, JsonElement>>()
            servicio["titulo"]?.jsonPrimitive?.content ?: "Servicio"
        } catch (_: Exception) { "Servicio" }
    }
}