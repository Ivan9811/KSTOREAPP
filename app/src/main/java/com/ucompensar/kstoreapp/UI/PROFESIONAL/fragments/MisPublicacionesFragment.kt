package com.ucompensar.kstoreapp.UI.PROFESIONAL.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.process.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

// ── Modelo ────────────────────────────────────────────────────────────────────
data class PublicacionItem(
    val id          : String,
    val titulo      : String,
    val descripcion : String,
    val precio      : Double,
    val categoria   : String,
    val fotoUrl     : String,
    val activo      : Boolean
)

class MisPublicacionesFragment : Fragment() {

    private lateinit var rvPublicaciones  : RecyclerView
    private lateinit var layoutVacio      : LinearLayout
    private lateinit var tvContador       : TextView
    private lateinit var btnFiltroTodos   : TextView
    private lateinit var btnFiltroActivos : TextView
    private lateinit var btnFiltroPausados: TextView

    private val listaCompleta = mutableListOf<PublicacionItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_mis_publicaciones, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvPublicaciones   = view.findViewById(R.id.rvMisPublicaciones)
        layoutVacio       = view.findViewById(R.id.layoutVacio)
        tvContador        = view.findViewById(R.id.tvContadorPublicaciones)
        btnFiltroTodos    = view.findViewById(R.id.btnFiltroTodos)
        btnFiltroActivos  = view.findViewById(R.id.btnFiltroActivos)
        btnFiltroPausados = view.findViewById(R.id.btnFiltroPausados)

        rvPublicaciones.layoutManager = LinearLayoutManager(requireContext())

        // Volver
        view.findViewById<ImageView>(R.id.btnVolver).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Filtros
        btnFiltroTodos.setOnClickListener {
            seleccionarFiltro(btnFiltroTodos)
            mostrarLista(listaCompleta)
        }
        btnFiltroActivos.setOnClickListener {
            seleccionarFiltro(btnFiltroActivos)
            mostrarLista(listaCompleta.filter { it.activo }.toMutableList())
        }
        btnFiltroPausados.setOnClickListener {
            seleccionarFiltro(btnFiltroPausados)
            mostrarLista(listaCompleta.filter { !it.activo }.toMutableList())
        }

        // ✅ FAB — ID correcto: fragmentContainer
        view.findViewById<FloatingActionButton>(R.id.fabNuevaPublicacion).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, publicacionProfesional())
                .addToBackStack(null)
                .commit()
        }

        // ✅ Botón estado vacío — ID correcto: fragmentContainer
        view.findViewById<Button>(R.id.btnPublicarPrimero).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, publicacionProfesional())
                .addToBackStack(null)
                .commit()
        }

        cargarPublicaciones()
    }

    override fun onResume() {
        super.onResume()
        cargarPublicaciones()
    }

    // ── Cargar desde Supabase ─────────────────────────────────────────────────
    private fun cargarPublicaciones() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                    ?: return@launch

                val servicios = SupabaseClient.client.postgrest
                    .from("servicios")
                    .select { filter { eq("profesional_id", uid) } }
                    .decodeList<Map<String, JsonElement>>()

                listaCompleta.clear()
                for (s in servicios) {
                    val id = s["id"]?.jsonPrimitive?.content ?: continue
                    listaCompleta.add(
                        PublicacionItem(
                            id          = id,
                            titulo      = s["titulo"]?.jsonPrimitive?.content ?: "",
                            descripcion = s["descripcion"]?.jsonPrimitive?.content ?: "",
                            precio      = s["precio"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                            categoria   = s["categoria"]?.jsonPrimitive?.content ?: "",
                            fotoUrl     = s["foto_url"]?.jsonPrimitive?.content ?: "",
                            activo      = s["activo"]?.jsonPrimitive?.booleanOrNull ?: false
                        )
                    )
                }

                tvContador.text = listaCompleta.size.toString()
                mostrarLista(listaCompleta)

            } catch (e: Exception) {
                Log.e("MIS_PUBL", "Error: ${e.message}", e)
            }
        }
    }

    // ── Mostrar lista ─────────────────────────────────────────────────────────
    private fun mostrarLista(lista: List<PublicacionItem>) {
        if (lista.isEmpty()) {
            rvPublicaciones.visibility = View.GONE
            layoutVacio.visibility     = View.VISIBLE
        } else {
            rvPublicaciones.visibility = View.VISIBLE
            layoutVacio.visibility     = View.GONE
            rvPublicaciones.adapter = PublicacionesAdapter(
                lista.toMutableList(),
                onPausar   = { item -> toggleActivo(item) },
                onEliminar = { item -> confirmarEliminar(item) }
            )
        }
    }

    // ── Toggle activo/pausado ─────────────────────────────────────────────────
    private fun toggleActivo(item: PublicacionItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val nuevoEstado = !item.activo
                SupabaseClient.client.postgrest
                    .from("servicios")
                    .update(buildJsonObject { put("activo", nuevoEstado) }) {
                        filter { eq("id", item.id) }
                    }
                Toast.makeText(requireContext(),
                    if (nuevoEstado) "Servicio activado ✓" else "Servicio pausado",
                    Toast.LENGTH_SHORT).show()
                cargarPublicaciones()
            } catch (e: Exception) {
                Log.e("MIS_PUBL", "Error toggle: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al cambiar estado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Eliminar con confirmación ─────────────────────────────────────────────
    private fun confirmarEliminar(item: PublicacionItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar publicación")
            .setMessage("¿Estás seguro de que quieres eliminar '${item.titulo}'?\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ -> eliminarServicio(item) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarServicio(item: PublicacionItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                SupabaseClient.client.postgrest
                    .from("servicios")
                    .delete { filter { eq("id", item.id) } }
                Toast.makeText(requireContext(),
                    "Publicación eliminada", Toast.LENGTH_SHORT).show()
                cargarPublicaciones()
            } catch (e: Exception) {
                Log.e("MIS_PUBL", "Error eliminar: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al eliminar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Filtros UI ────────────────────────────────────────────────────────────
    private fun seleccionarFiltro(seleccionado: TextView) {
        listOf(btnFiltroTodos, btnFiltroActivos, btnFiltroPausados).forEach {
            it.setBackgroundResource(R.drawable.bg_chip_unselected)
            it.setTextColor(resources.getColor(R.color.morado_tarjetas, null))
        }
        seleccionado.setBackgroundResource(R.drawable.bg_chip_selected)
        seleccionado.setTextColor(resources.getColor(R.color.white, null))
    }
}

// ── Adapter ───────────────────────────────────────────────────────────────────
class PublicacionesAdapter(
    private val items     : MutableList<PublicacionItem>,
    private val onPausar  : (PublicacionItem) -> Unit,
    private val onEliminar: (PublicacionItem) -> Unit
) : RecyclerView.Adapter<PublicacionesAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivFoto        : android.widget.ImageView = view.findViewById(R.id.ivFotoPublicacion)
        val tvEstado      : TextView = view.findViewById(R.id.tvEstadoPublicacion)
        val tvTitulo      : TextView = view.findViewById(R.id.tvTituloPublicacion)
        val tvCategoria   : TextView = view.findViewById(R.id.tvCategoriaPublicacion)
        val tvDescripcion : TextView = view.findViewById(R.id.tvDescripcionPublicacion)
        val tvPrecio      : TextView = view.findViewById(R.id.tvPrecioPublicacion)
        val btnPausar     : Button   = view.findViewById(R.id.btnPausarPublicacion)
        val btnEliminar   : android.widget.ImageView = view.findViewById(R.id.btnEliminarPublicacion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_publicacion_profesional, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvTitulo.text      = item.titulo
        holder.tvCategoria.text   = item.categoria
        holder.tvDescripcion.text = item.descripcion
        holder.tvPrecio.text      = "$${ String.format("%,.0f", item.precio)}/hr"

        holder.tvEstado.text = if (item.activo) "Activo" else "Pausado"
        holder.tvEstado.setBackgroundResource(
            if (item.activo) R.drawable.bg_chip_selected
            else             R.drawable.bg_chip_unselected
        )
        if (!item.activo) {
            holder.tvEstado.setTextColor(
                holder.itemView.context.getColor(R.color.morado_tarjetas)
            )
        }

        holder.btnPausar.text = if (item.activo) "Pausar" else "Activar"

        if (item.fotoUrl.isNotEmpty()) {
            holder.ivFoto.load(item.fotoUrl) { crossfade(true) }
        }

        holder.btnPausar.setOnClickListener   { onPausar(item) }
        holder.btnEliminar.setOnClickListener { onEliminar(item) }
    }

    override fun getItemCount() = items.size
}