package com.ucompensar.kstoreapp.UI.CLIENTE.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.ucompensar.kstoreapp.UI.CLIENTE.DetalleServicioActivity
import com.ucompensar.kstoreapp.process.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

// ── Modelo resultado búsqueda ─────────────────────────────────────────────────
data class ResultadoBusqueda(
    val servicioId    : String,
    val profesionalId : String,
    val titulo        : String,
    val categoria     : String,
    val precio        : Double,
    val nombreProfesional: String,
    val calificacion  : Double,
    val fotoUrl       : String
)

class BuscarFragment : Fragment() {

    private lateinit var etBuscar   : EditText
    private lateinit var btnBuscar  : Button
    private lateinit var rvResultados: RecyclerView
    private lateinit var tvSinResultados: TextView

    private var categoriaSeleccionada = "Todos"
    private var ordenSeleccionado     = "calificacion"
    private var calMinima             = 0.0
    private var busquedaJob           : Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_buscar, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etBuscar         = view.findViewById(R.id.etBuscar)
        btnBuscar        = view.findViewById(R.id.btnBuscar)
        rvResultados     = view.findViewById(R.id.rvResultados)
        tvSinResultados  = view.findViewById(R.id.tvSinResultados)

        rvResultados.layoutManager = LinearLayoutManager(requireContext())

        // ── Chips categorías ──────────────────────────────────
        val chipMap = mapOf(
            R.id.chipTodos         to "Todos",
            R.id.chipElectricista  to "Electricidad",
            R.id.chipPlomero       to "Plomería",
            R.id.chipCarpintero    to "Carpintería",
            R.id.chipPintor        to "Pintura"
        )
        chipMap.forEach { (id, cat) ->
            view.findViewById<View>(id)?.setOnClickListener {
                categoriaSeleccionada = cat
                actualizarChips(view, chipMap, id)
                buscar()
            }
        }

        // ── Chips calificación ────────────────────────────────
        mapOf(
            R.id.chipCalTodas to 0.0,
            R.id.chipCal4     to 4.0,
            R.id.chipCal45    to 4.5,
            R.id.chipCal5     to 5.0
        ).forEach { (id, min) ->
            view.findViewById<View>(id)?.setOnClickListener {
                calMinima = min
                buscar()
            }
        }

        // ── Buscar mientras escribe ───────────────────────────
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                busquedaJob?.cancel()
                busquedaJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(500) // debounce
                    buscar()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnBuscar.setOnClickListener { buscar() }

        // Cargar todos al inicio
        buscar()
    }

    private fun actualizarChips(view: View, chipMap: Map<Int, String>, seleccionado: Int) {
        chipMap.keys.forEach { id ->
            val chip = view.findViewById<TextView>(id) ?: return@forEach
            if (id == seleccionado) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(resources.getColor(R.color.white, null))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(resources.getColor(R.color.morado_tarjetas, null))
            }
        }
    }

    private fun buscar() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val query = etBuscar.text.toString().trim().lowercase()

                val servicios = SupabaseClient.client.postgrest
                    .from("servicios")
                    .select { filter { eq("activo", true) } }
                    .decodeList<Map<String, JsonElement>>()

                val resultados = mutableListOf<ResultadoBusqueda>()

                for (s in servicios) {
                    val titulo    = s["titulo"]?.jsonPrimitive?.content ?: ""
                    val categoria = s["categoria"]?.jsonPrimitive?.content ?: ""
                    val precio    = s["precio"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val fotoUrl   = s["foto_url"]?.jsonPrimitive?.content ?: ""
                    val servicioId    = s["id"]?.jsonPrimitive?.content ?: ""
                    val profesionalId = s["profesional_id"]?.jsonPrimitive?.content ?: ""

                    // Filtro categoría
                    if (categoriaSeleccionada != "Todos" && categoria != categoriaSeleccionada) continue

                    // Filtro texto
                    if (query.isNotEmpty() && !titulo.lowercase().contains(query) &&
                        !categoria.lowercase().contains(query)) continue

                    // Datos profesional
                    val nombreProfesional = try {
                        val prof = SupabaseClient.client.postgrest
                            .from("profiles")
                            .select { filter { eq("id", profesionalId) } }
                            .decodeSingle<Map<String, JsonElement>>()
                        prof["nombre"]?.jsonPrimitive?.content ?: "Profesional"
                    } catch (_: Exception) { "Profesional" }

                    val calificacion = try {
                        val prof = SupabaseClient.client.postgrest
                            .from("profesionales")
                            .select { filter { eq("id", profesionalId) } }
                            .decodeSingle<Map<String, JsonElement>>()
                        prof["calificacion"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    } catch (_: Exception) { 0.0 }

                    // Filtro calificación mínima
                    if (calificacion < calMinima) continue

                    resultados.add(ResultadoBusqueda(
                        servicioId     = servicioId,
                        profesionalId  = profesionalId,
                        titulo         = titulo,
                        categoria      = categoria,
                        precio         = precio,
                        nombreProfesional = nombreProfesional,
                        calificacion   = calificacion,
                        fotoUrl        = fotoUrl
                    ))
                }

                // Ordenar
                val ordenados = resultados.sortedByDescending { it.calificacion }

                if (ordenados.isEmpty()) {
                    rvResultados.visibility    = View.GONE
                    tvSinResultados.visibility = View.VISIBLE
                } else {
                    rvResultados.visibility    = View.VISIBLE
                    tvSinResultados.visibility = View.GONE
                    rvResultados.adapter = ResultadosAdapter(ordenados) { resultado ->
                        startActivity(
                            Intent(requireContext(), DetalleServicioActivity::class.java).apply {
                                putExtra("servicio_id",    resultado.servicioId)
                                putExtra("profesional_id", resultado.profesionalId)
                            }
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("BUSCAR", "Error: ${e.message}", e)
            }
        }
    }
}

// ── Adapter resultados ────────────────────────────────────────────────────────
class ResultadosAdapter(
    private val items  : List<ResultadoBusqueda>,
    private val onClick: (ResultadoBusqueda) -> Unit
) : RecyclerView.Adapter<ResultadosAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo    : TextView = view.findViewById(R.id.tvTituloServicio)
        val tvCategoria : TextView = view.findViewById(R.id.tvCategoriaServicio)
        val tvPrecio    : TextView = view.findViewById(R.id.tvPrecioServicio)
        val tvProfesional: TextView = view.findViewById(R.id.tvNombreProfesional)
        val tvCal       : TextView = view.findViewById(R.id.tvCalificacionServicio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_servicio_buscar, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitulo.text     = item.titulo
        holder.tvCategoria.text  = item.categoria
        holder.tvPrecio.text     = "$${String.format("%,.0f", item.precio)}/hr"
        holder.tvProfesional.text = item.nombreProfesional
        holder.tvCal.text        = "⭐ ${String.format("%.1f", item.calificacion)}"
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}