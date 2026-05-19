package com.ucompensar.kstoreapp.UI.CLIENTE.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.CLIENTE.PedidoClienteItem
import com.ucompensar.kstoreapp.UI.CLIENTE.PedidosClienteAdapter
import com.ucompensar.kstoreapp.UI.ChatActivity
import com.ucompensar.kstoreapp.process.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Locale

class MisPedidosFragment : Fragment() {

    private lateinit var recycler: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_mis_pedidos, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler = view.findViewById(R.id.rvPedidos)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        cargarPedidos()
    }

    override fun onResume() {
        super.onResume()
        cargarPedidos()
    }

    private fun cargarPedidos() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                    ?: return@launch

                val pedidosRaw = SupabaseClient.client.postgrest
                    .from("pedidos")
                    .select { filter { eq("cliente_id", uid) } }
                    .decodeList<Map<String, JsonElement>>()

                val lista = mutableListOf<PedidoClienteItem>()

                for (p in pedidosRaw) {
                    val pedidoId   = p["id"]?.jsonPrimitive?.content ?: continue
                    val estado     = p["estado"]?.jsonPrimitive?.content ?: "pendiente"
                    val servicioId = p["servicio_id"]?.jsonPrimitive?.content ?: ""
                    val profesionalId = p["profesional_id"]?.jsonPrimitive?.content ?: ""
                    val referencia = p["referencia"]?.jsonPrimitive?.content ?: ""
                    val createdAt  = p["created_at"]?.jsonPrimitive?.content ?: ""

                    val fechaServicio = p["fecha_servicio"]?.jsonPrimitive?.content
                        ?.let { formatearFecha(it) } ?: formatearFecha(createdAt)
                    val horaServicio = p["hora_servicio"]?.jsonPrimitive?.content
                        ?.let { formatearHora(it) } ?: "--:--"

                    // Nombre profesional
                    val nombreProfesional = obtenerNombre(profesionalId)

                    // Datos servicio
                    val (tituloServicio, precio) = obtenerDatosServicio(servicioId)

                    val estadoTexto = when (estado) {
                        "pendiente"  -> "Nueva solicitud"
                        "aceptado"   -> "Confirmado"
                        "completado" -> "Completado"
                        "rechazado"  -> "Rechazado"
                        else         -> estado
                    }

                    lista.add(PedidoClienteItem(
                        id           = pedidoId,
                        profesionalId = profesionalId,
                        estado       = estadoTexto,
                        fecha        = formatearFecha(createdAt),
                        servicio     = tituloServicio,
                        profesional  = nombreProfesional,
                        precio       = "$${String.format("%,.0f", precio)}",
                        fechaDetalle = fechaServicio,
                        hora         = horaServicio,
                        referencia   = if (referencia.isNotEmpty()) "#$referencia" else "#---",
                        cancelable   = estado == "pendiente"
                    ))
                }

                // Ordenar: pendientes primero
                val ordenada = lista.sortedWith(compareBy {
                    when (it.estado) {
                        "Nueva solicitud" -> 0
                        "Confirmado"      -> 1
                        "Completado"      -> 2
                        "Rechazado"       -> 3
                        else              -> 4
                    }
                })

                recycler.adapter = PedidosClienteAdapter(
                    ordenada,
                    onChat = { item ->
                        abrirChat(item.profesionalId, item.profesional)
                    },
                    onCancelar = { item ->
                        cancelarPedido(item.id)
                    }
                )

            } catch (e: Exception) {
                Log.e("MIS_PEDIDOS", "Error: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al cargar pedidos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirChat(profesionalId: String, nombreProfesional: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                    ?: return@launch

                // Buscar conversación existente o crear nueva
                val existentes = SupabaseClient.client.postgrest
                    .from("conversaciones")
                    .select { filter {
                        eq("cliente_id", uid)
                        eq("profesional_id", profesionalId)
                    }}
                    .decodeList<Map<String, JsonElement>>()

                val convId = if (existentes.isNotEmpty()) {
                    existentes.first()["id"]?.jsonPrimitive?.content ?: ""
                } else {
                    val nueva = SupabaseClient.client.postgrest
                        .from("conversaciones")
                        .insert(buildJsonObject {
                            put("cliente_id",     uid)
                            put("profesional_id", profesionalId)
                            put("ultimo_mensaje", "")
                        })
                        .decodeSingle<Map<String, JsonElement>>()
                    nueva["id"]?.jsonPrimitive?.content ?: ""
                }

                startActivity(
                    Intent(requireContext(), ChatActivity::class.java).apply {
                        putExtra("conversacion_id", convId)
                        putExtra("otro_nombre",     nombreProfesional)
                        putExtra("otro_id",         profesionalId)
                    }
                )
            } catch (e: Exception) {
                Log.e("MIS_PEDIDOS", "Error abriendo chat: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al abrir chat", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelarPedido(pedidoId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                SupabaseClient.client.postgrest
                    .from("pedidos")
                    .update(buildJsonObject { put("estado", "rechazado") }) {
                        filter { eq("id", pedidoId) }
                    }
                Toast.makeText(requireContext(), "Pedido cancelado", Toast.LENGTH_SHORT).show()
                cargarPedidos()
            } catch (e: Exception) {
                Log.e("MIS_PEDIDOS", "Error cancelando: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al cancelar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun obtenerNombre(uid: String): String {
        return try {
            val profile = SupabaseClient.client.postgrest
                .from("profiles")
                .select { filter { eq("id", uid) } }
                .decodeSingle<Map<String, JsonElement>>()
            profile["nombre"]?.jsonPrimitive?.content ?: "Profesional"
        } catch (_: Exception) { "Profesional" }
    }

    private suspend fun obtenerDatosServicio(servicioId: String): Pair<String, Double> {
        return try {
            val servicio = SupabaseClient.client.postgrest
                .from("servicios")
                .select { filter { eq("id", servicioId) } }
                .decodeSingle<Map<String, JsonElement>>()
            val titulo = servicio["titulo"]?.jsonPrimitive?.content ?: "Servicio"
            val precio = servicio["precio"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
            Pair(titulo, precio)
        } catch (_: Exception) { Pair("Servicio", 0.0) }
    }

    private fun formatearFecha(fecha: String): String {
        return try {
            val entrada = if (fecha.contains("T"))
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            else
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val salida = SimpleDateFormat("d MMMM yyyy", Locale("es", "CO"))
            salida.format(entrada.parse(fecha.substring(0, 19))!!)
        } catch (_: Exception) { fecha }
    }

    private fun formatearHora(hora: String): String {
        return try {
            val entrada = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val salida  = SimpleDateFormat("HH:mm", Locale.getDefault())
            salida.format(entrada.parse(hora)!!)
        } catch (_: Exception) { hora }
    }
}