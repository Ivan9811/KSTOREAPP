package com.ucompensar.kstoreapp.UI.CLIENTE.fragments

import android.content.Intent
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
import coil.load
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.CLIENTE.DetalleServicioActivity
import com.ucompensar.kstoreapp.process.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

data class FavoritoItem(
    val favoritoId   : String,
    val servicioId   : String,
    val profesionalId: String,
    val titulo       : String,
    val categoria    : String,
    val precio       : Double,
    val profesional  : String,
    val calificacion : Double,
    val fotoUrl      : String   // ✅ foto del servicio
)

class FavoritosFragment : Fragment() {

    private lateinit var rvFavoritos : RecyclerView
    private lateinit var layoutVacio : LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_favoritos, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvFavoritos = view.findViewById(R.id.rvFavoritos)
        layoutVacio = view.findViewById(R.id.layoutVacio)
        rvFavoritos.layoutManager = LinearLayoutManager(requireContext())

        // ✅ Botón explorar servicios conectado
        view.findViewById<Button>(R.id.btnExplorar)?.setOnClickListener {
            // Navegar al tab de Buscar (index 1 en el BottomNav del cliente)
            val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
            bottomNav?.selectedItemId = R.id.nav_buscar
        }

        cargarFavoritos()
    }

    override fun onResume() {
        super.onResume()
        cargarFavoritos()
    }

    private fun cargarFavoritos() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                    ?: return@launch

                // ── 1. Traer todos los favoritos del usuario ──────────────────
                val favs = SupabaseClient.client.postgrest
                    .from("favoritos")
                    .select { filter { eq("cliente_id", uid) } }
                    .decodeList<Map<String, JsonElement>>()

                if (favs.isEmpty()) {
                    mostrarEstadoVacio()
                    return@launch
                }

                val lista = mutableListOf<FavoritoItem>()

                // ── 2. Por cada favorito, cargar datos del servicio ───────────
                for (fav in favs) {
                    val favId      = fav["id"]?.jsonPrimitive?.content ?: continue
                    val servicioId = fav["servicio_id"]?.jsonPrimitive?.content ?: continue

                    val servicio = try {
                        SupabaseClient.client.postgrest
                            .from("servicios")
                            .select { filter { eq("id", servicioId) } }
                            .decodeSingle<Map<String, JsonElement>>()
                    } catch (_: Exception) { continue }

                    val titulo        = servicio["titulo"]?.jsonPrimitive?.content ?: ""
                    val categoria     = servicio["categoria"]?.jsonPrimitive?.content ?: ""
                    val precio        = servicio["precio"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val fotoUrl       = servicio["foto_url"]?.jsonPrimitive?.content ?: ""
                    val profesionalId = servicio["profesional_id"]?.jsonPrimitive?.content ?: ""

                    // ── Nombre del profesional ────────────────────────────────
                    val nombreProfesional = try {
                        SupabaseClient.client.postgrest
                            .from("profiles")
                            .select { filter { eq("id", profesionalId) } }
                            .decodeSingle<Map<String, JsonElement>>()
                            .let { it["nombre"]?.jsonPrimitive?.content ?: "Profesional" }
                    } catch (_: Exception) { "Profesional" }

                    // ── Calificación ──────────────────────────────────────────
                    val calificacion = try {
                        SupabaseClient.client.postgrest
                            .from("profesionales")
                            .select { filter { eq("id", profesionalId) } }
                            .decodeSingle<Map<String, JsonElement>>()
                            .let { it["calificacion"]?.jsonPrimitive?.doubleOrNull ?: 0.0 }
                    } catch (_: Exception) { 0.0 }

                    lista.add(FavoritoItem(
                        favoritoId    = favId,
                        servicioId    = servicioId,
                        profesionalId = profesionalId,
                        titulo        = titulo,
                        categoria     = categoria,
                        precio        = precio,
                        profesional   = nombreProfesional,
                        calificacion  = calificacion,
                        fotoUrl       = fotoUrl
                    ))
                }

                if (lista.isEmpty()) {
                    mostrarEstadoVacio()
                } else {
                    rvFavoritos.visibility = View.VISIBLE
                    layoutVacio.visibility = View.GONE
                    rvFavoritos.adapter = FavoritosAdapter(
                        items = lista,
                        onClick = { item ->
                            startActivity(
                                Intent(requireContext(), DetalleServicioActivity::class.java).apply {
                                    putExtra("servicio_id",    item.servicioId)
                                    putExtra("profesional_id", item.profesionalId)
                                }
                            )
                        },
                        onEliminar = { item -> eliminarFavorito(item.favoritoId) }
                    )
                }

            } catch (e: Exception) {
                Log.e("FAVORITOS", "Error: ${e.message}", e)
            }
        }
    }

    private fun mostrarEstadoVacio() {
        rvFavoritos.visibility = View.GONE
        layoutVacio.visibility = View.VISIBLE
    }

    private fun eliminarFavorito(favoritoId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                SupabaseClient.client.postgrest
                    .from("favoritos")
                    .delete { filter { eq("id", favoritoId) } }
                Toast.makeText(requireContext(), "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
                cargarFavoritos()
            } catch (e: Exception) {
                Log.e("FAVORITOS", "Error eliminando: ${e.message}", e)
            }
        }
    }
}

// ── Adapter ───────────────────────────────────────────────────────────────────
class FavoritosAdapter(
    private val items     : List<FavoritoItem>,
    private val onClick   : (FavoritoItem) -> Unit,
    private val onEliminar: (FavoritoItem) -> Unit
) : RecyclerView.Adapter<FavoritosAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivFoto       : android.widget.ImageView = view.findViewById(R.id.ivFotoServicio)
        val layoutFallback: LinearLayout            = view.findViewById(R.id.layoutIconoFallback)
        val tvTitulo     : TextView                 = view.findViewById(R.id.tvTituloServicio)
        val tvCategoria  : TextView                 = view.findViewById(R.id.tvCategoriaServicio)
        val tvPrecio     : TextView                 = view.findViewById(R.id.tvPrecioServicio)
        val tvProfesional: TextView                 = view.findViewById(R.id.tvNombreProfesional)
        val tvCal        : TextView                 = view.findViewById(R.id.tvCalificacionServicio)
        val btnEliminar  : android.widget.ImageView = view.findViewById(R.id.btnEliminarFavorito)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorito, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvTitulo.text       = item.titulo
        holder.tvCategoria.text    = item.categoria
        holder.tvPrecio.text       = "$${ String.format("%,.0f", item.precio)}/hr"
        holder.tvProfesional.text  = item.profesional
        holder.tvCal.text          = "⭐ ${String.format("%.1f", item.calificacion)}"

        // ✅ Cargar foto del servicio si existe
        if (item.fotoUrl.isNotEmpty()) {
            holder.ivFoto.visibility        = View.VISIBLE
            holder.layoutFallback.visibility = View.GONE
            holder.ivFoto.load(item.fotoUrl) { crossfade(true) }
        } else {
            holder.ivFoto.visibility        = View.GONE
            holder.layoutFallback.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener { onClick(item) }
        holder.btnEliminar.setOnClickListener { onEliminar(item) }
    }

    override fun getItemCount() = items.size
}