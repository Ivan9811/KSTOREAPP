package com.ucompensar.kstoreapp.UI.PROFESIONAL.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.process.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Locale

// ── Modelo ────────────────────────────────────────────────────────────────────
data class PedidoProfesional(
    val id            : String,
    val estado        : String,
    val tituloServicio: String,
    val nombreCliente : String,
    val clienteId     : String,
    val precio        : Double,
    val fechaCreacion : String,
    val fechaServicio : String,
    val horaServicio  : String,
    val referencia    : String
)

// ── Fragment ──────────────────────────────────────────────────────────────────
// ✅ Clase renombrada a PedidosProfesionalFragment para coincidir con ProfesionalActivity
class PedidosProfesionalFragment : Fragment() {

    private lateinit var rvPedidos   : RecyclerView
    private lateinit var layoutVacio : LinearLayout

    private lateinit var filtroTodos      : TextView
    private lateinit var filtroPendiente  : TextView
    private lateinit var filtroAceptado   : TextView
    private lateinit var filtroCompletado : TextView

    private var todosPedidos = listOf<PedidoProfesional>()
    private var filtroActual = "todos"
    private lateinit var adapter: PedidosProfesionalAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_pedidos_profesional, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvPedidos   = view.findViewById(R.id.rvPedidos)
        layoutVacio = view.findViewById(R.id.layoutVacio)

        filtroTodos      = view.findViewById(R.id.filtroTodos)
        filtroPendiente  = view.findViewById(R.id.filtroPendiente)
        filtroAceptado   = view.findViewById(R.id.filtroAceptado)
        filtroCompletado = view.findViewById(R.id.filtroCompletado)

        adapter = PedidosProfesionalAdapter(
            pedidos     = mutableListOf(),
            onAceptar   = { pedido -> cambiarEstado(pedido, "aceptado") },
            onCompletar = { pedido -> cambiarEstado(pedido, "completado") },
            onRechazar  = { pedido -> cambiarEstado(pedido, "rechazado") },
            onChat      = { pedido -> abrirChat(pedido) }
        )

        rvPedidos.layoutManager = LinearLayoutManager(requireContext())
        rvPedidos.adapter = adapter

        configurarFiltros()
        cargarPedidos()
    }

    override fun onResume() {
        super.onResume()
        cargarPedidos()
    }

    // ── Filtros ───────────────────────────────────────────────
    private fun configurarFiltros() {
        filtroTodos.setOnClickListener      { aplicarFiltro("todos") }
        filtroPendiente.setOnClickListener  { aplicarFiltro("pendiente") }
        filtroAceptado.setOnClickListener   { aplicarFiltro("aceptado") }
        filtroCompletado.setOnClickListener { aplicarFiltro("completado") }
    }

    private fun aplicarFiltro(filtro: String) {
        filtroActual = filtro
        val chips  = listOf(filtroTodos, filtroPendiente, filtroAceptado, filtroCompletado)
        val estados = listOf("todos", "pendiente", "aceptado", "completado")
        chips.forEachIndexed { i, chip ->
            if (estados[i] == filtro) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(resources.getColor(R.color.white, null))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(resources.getColor(R.color.morado_tarjetas, null))
            }
        }
        val filtrados = if (filtro == "todos") todosPedidos
        else todosPedidos.filter { it.estado == filtro }
        mostrarLista(filtrados)
    }

    // ── Cargar pedidos ────────────────────────────────────────
    private fun cargarPedidos() {
        lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                    ?: return@launch

                val pedidosRaw = SupabaseClient.client.postgrest
                    .from("pedidos")
                    .select { filter { eq("profesional_id", uid) } }
                    .decodeList<Map<String, JsonElement>>()

                val lista = mutableListOf<PedidoProfesional>()

                for (p in pedidosRaw) {
                    val pedidoId   = p["id"]?.jsonPrimitive?.content ?: continue
                    val estado     = p["estado"]?.jsonPrimitive?.content ?: "pendiente"
                    val clienteId  = p["cliente_id"]?.jsonPrimitive?.content ?: ""
                    val servicioId = p["servicio_id"]?.jsonPrimitive?.content ?: ""
                    val referencia = p["referencia"]?.jsonPrimitive?.content ?: ""
                    val createdAt  = p["created_at"]?.jsonPrimitive?.content ?: ""

                    val fechaServicio = p["fecha_servicio"]?.jsonPrimitive?.content
                        ?.let { formatearFecha(it) } ?: formatearFecha(createdAt)
                    val horaServicio  = p["hora_servicio"]?.jsonPrimitive?.content
                        ?.let { formatearHora(it) } ?: "--:--"

                    val nombreCliente          = obtenerNombreCliente(clienteId)
                    val (tituloServicio, precio) = obtenerDatosServicio(servicioId)

                    lista.add(PedidoProfesional(
                        id             = pedidoId,
                        estado         = estado,
                        tituloServicio = tituloServicio,
                        nombreCliente  = nombreCliente,
                        clienteId      = clienteId,
                        precio         = precio,
                        fechaCreacion  = formatearFecha(createdAt),
                        fechaServicio  = fechaServicio,
                        horaServicio   = horaServicio,
                        referencia     = if (referencia.isNotEmpty()) "#$referencia" else "#---"
                    ))
                }

                todosPedidos = lista.sortedWith(compareBy {
                    when (it.estado) {
                        "pendiente"  -> 0
                        "aceptado"   -> 1
                        "completado" -> 2
                        else         -> 3
                    }
                })

                aplicarFiltro(filtroActual)

            } catch (e: Exception) {
                Log.e("PEDIDOS_PROF", "Error: ${e.message}", e)
            }
        }
    }

    private fun mostrarLista(lista: List<PedidoProfesional>) {
        if (lista.isEmpty()) {
            rvPedidos.visibility   = View.GONE
            layoutVacio.visibility = View.VISIBLE
        } else {
            rvPedidos.visibility   = View.VISIBLE
            layoutVacio.visibility = View.GONE
            adapter.actualizar(lista)
        }
    }

    // ── Cambiar estado ────────────────────────────────────────
    private fun cambiarEstado(pedido: PedidoProfesional, nuevoEstado: String) {
        lifecycleScope.launch {
            try {
                SupabaseClient.client.postgrest
                    .from("pedidos")
                    .update(mapOf("estado" to nuevoEstado)) {
                        filter { eq("id", pedido.id) }
                    }
                val msg = when (nuevoEstado) {
                    "aceptado"   -> "Pedido aceptado ✓"
                    "completado" -> "Pedido completado ✓"
                    "rechazado"  -> "Pedido rechazado"
                    else         -> "Estado actualizado"
                }
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                cargarPedidos()
            } catch (e: Exception) {
                Log.e("PEDIDOS_PROF", "Error: ${e.message}", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Abrir chat con el cliente ─────────────────────────────
    private fun abrirChat(pedido: PedidoProfesional) {
        lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                    ?: return@launch

                // Buscar conversación existente o crear una nueva
                val existentes = SupabaseClient.client.postgrest
                    .from("conversaciones")
                    .select {
                        filter {
                            eq("cliente_id",     pedido.clienteId)
                            eq("profesional_id", uid)
                        }
                    }
                    .decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()

                val convId = if (existentes.isNotEmpty()) {
                    existentes.first()["id"]?.jsonPrimitive?.content ?: ""
                } else {
                    val nueva = SupabaseClient.client.postgrest
                        .from("conversaciones")
                        .insert(
                            kotlinx.serialization.json.buildJsonObject {
                                put("cliente_id",     pedido.clienteId)
                                put("profesional_id", uid)
                                put("ultimo_mensaje", "")
                            }
                        )
                        .decodeSingle<Map<String, kotlinx.serialization.json.JsonElement>>()
                    nueva["id"]?.jsonPrimitive?.content ?: ""
                }

                startActivity(
                    android.content.Intent(
                        requireContext(),
                        com.ucompensar.kstoreapp.UI.ChatActivity::class.java
                    ).apply {
                        putExtra("conversacion_id", convId)
                        putExtra("otro_nombre",     pedido.nombreCliente)
                        putExtra("otro_id",         pedido.clienteId)
                    }
                )
            } catch (e: Exception) {
                Log.e("PEDIDOS_PROF", "Error abriendo chat: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al abrir chat", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────
    private suspend fun obtenerNombreCliente(clienteId: String): String {
        return try {
            val cliente = SupabaseClient.client.postgrest
                .from("profiles")
                .select { filter { eq("id", clienteId) } }
                .decodeSingle<Map<String, JsonElement>>()
            cliente["nombre"]?.jsonPrimitive?.content ?: "Cliente"
        } catch (_: Exception) { "Cliente" }
    }

    private suspend fun obtenerDatosServicio(servicioId: String): Pair<String, Double> {
        return try {
            val servicio = SupabaseClient.client.postgrest
                .from("servicios")
                .select { filter { eq("id", servicioId) } }
                .decodeSingle<Map<String, JsonElement>>()
            val titulo = servicio["titulo"]?.jsonPrimitive?.content ?: "Servicio"
            val precio = servicio["precio"]?.jsonPrimitive?.doubleOrNull ?: 0.0
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

// ── Adapter ───────────────────────────────────────────────────────────────────
class PedidosProfesionalAdapter(
    private val pedidos    : MutableList<PedidoProfesional>,
    private val onAceptar  : (PedidoProfesional) -> Unit,
    private val onCompletar: (PedidoProfesional) -> Unit,
    private val onRechazar : (PedidoProfesional) -> Unit,
    private val onChat     : (PedidoProfesional) -> Unit
) : RecyclerView.Adapter<PedidosProfesionalAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEstadoBadge   : TextView     = view.findViewById(R.id.tvEstadoBadge)
        val tvFecha         : TextView     = view.findViewById(R.id.tvFecha)
        val tvTituloServicio: TextView     = view.findViewById(R.id.tvTituloServicio)
        val tvNombreCliente : TextView     = view.findViewById(R.id.tvNombreCliente)
        val tvPrecio        : TextView     = view.findViewById(R.id.tvPrecio)
        val tvFechaServicio : TextView     = view.findViewById(R.id.tvFechaServicio)
        val tvHoraServicio  : TextView     = view.findViewById(R.id.tvHoraServicio)
        val tvReferencia    : TextView     = view.findViewById(R.id.tvReferencia)
        val btnChat         : Button       = view.findViewById(R.id.btnChat)
        val btnAccion       : Button       = view.findViewById(R.id.btnAccion)
        val layoutBotones   : LinearLayout = view.findViewById(R.id.layoutBotones)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pedido_profesional, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pedido = pedidos[position]
        val ctx    = holder.itemView.context

        holder.tvTituloServicio.text = pedido.tituloServicio
        holder.tvNombreCliente.text  = "Cliente: ${pedido.nombreCliente}"
        holder.tvPrecio.text         = "$${String.format("%,.0f", pedido.precio)}"
        holder.tvFecha.text          = pedido.fechaCreacion
        holder.tvFechaServicio.text  = pedido.fechaServicio
        holder.tvHoraServicio.text   = pedido.horaServicio
        holder.tvReferencia.text     = pedido.referencia

        when (pedido.estado) {
            "pendiente" -> {
                holder.tvEstadoBadge.text = "🔔 Nueva solicitud"
                holder.tvEstadoBadge.setBackgroundResource(R.drawable.bg_chip_unselected)
                holder.btnAccion.text = "Aceptar"
                holder.btnAccion.backgroundTintList = ctx.getColorStateList(R.color.primary)
                holder.btnAccion.visibility = View.VISIBLE
                holder.btnAccion.setOnClickListener { onAceptar(pedido) }
            }
            "aceptado" -> {
                holder.tvEstadoBadge.text = "✅ Confirmado"
                holder.tvEstadoBadge.setBackgroundResource(R.drawable.bg_chip_selected)
                holder.btnAccion.text = "Completar"
                holder.btnAccion.backgroundTintList =
                    ctx.getColorStateList(android.R.color.holo_green_dark)
                holder.btnAccion.visibility = View.VISIBLE
                holder.btnAccion.setOnClickListener { onCompletar(pedido) }
            }
            "completado" -> {
                holder.tvEstadoBadge.text = "✔ Completado"
                holder.tvEstadoBadge.setBackgroundResource(R.drawable.bg_chip_selected)
                holder.btnAccion.visibility = View.GONE
            }
            "rechazado" -> {
                holder.tvEstadoBadge.text = "✖ Rechazado"
                holder.tvEstadoBadge.setBackgroundResource(R.drawable.bg_chip_unselected)
                holder.btnAccion.visibility = View.GONE
            }
        }

        holder.btnChat.setOnClickListener { onChat(pedido) }
    }

    override fun getItemCount() = pedidos.size

    fun actualizar(nueva: List<PedidoProfesional>) {
        pedidos.clear()
        pedidos.addAll(nueva)
        notifyDataSetChanged()
    }
}