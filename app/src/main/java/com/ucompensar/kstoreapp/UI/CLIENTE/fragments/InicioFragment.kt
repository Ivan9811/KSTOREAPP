package com.ucompensar.kstoreapp.UI.CLIENTE.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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

class InicioFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_inicio, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cargarNombreUsuario(view)
        cargarServiciosDestacados(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            cargarNombreUsuario(it)
            cargarServiciosDestacados(it)
        }
    }

    // ── Nombre del usuario en el header ──────────────────────────────────────
    private fun cargarNombreUsuario(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                    ?: return@launch
                val profile = SupabaseClient.client.postgrest
                    .from("profiles")
                    .select { filter { eq("id", uid) } }
                    .decodeSingle<Map<String, JsonElement>>()
                val nombre = profile["nombre"]?.jsonPrimitive?.content ?: ""
                view.findViewById<TextView>(R.id.tvNombreUsuario)?.text = "Hola, $nombre 👋"
            } catch (e: Exception) {
                Log.e("INICIO", "Error nombre: ${e.message}")
            }
        }
    }

    // ── Servicios destacados — muestra hasta 3 ────────────────────────────────
    private fun cargarServiciosDestacados(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val servicios = SupabaseClient.client.postgrest
                    .from("servicios")
                    .select { filter { eq("activo", true) } }
                    .decodeList<Map<String, JsonElement>>()

                if (servicios.isEmpty()) return@launch

                // ── Primer servicio — tarjeta grande ─────────────────────────
                val s1            = servicios[0]
                val servicioId1   = s1["id"]?.jsonPrimitive?.content ?: ""
                val titulo1       = s1["titulo"]?.jsonPrimitive?.content ?: ""
                val descripcion1  = s1["descripcion"]?.jsonPrimitive?.content ?: ""
                val precio1       = s1["precio"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                val categoria1    = s1["categoria"]?.jsonPrimitive?.content ?: ""
                val fotoUrl1      = s1["foto_url"]?.jsonPrimitive?.content ?: ""
                val profId1       = s1["profesional_id"]?.jsonPrimitive?.content ?: ""

                val nombreProf1 = try {
                    SupabaseClient.client.postgrest
                        .from("profiles")
                        .select { filter { eq("id", profId1) } }
                        .decodeSingle<Map<String, JsonElement>>()
                        .let { it["nombre"]?.jsonPrimitive?.content ?: "Profesional" }
                } catch (_: Exception) { "Profesional" }

                val cal1 = try {
                    SupabaseClient.client.postgrest
                        .from("profesionales")
                        .select { filter { eq("id", profId1) } }
                        .decodeSingle<Map<String, JsonElement>>()
                        .let { it["calificacion"]?.jsonPrimitive?.doubleOrNull ?: 0.0 }
                } catch (_: Exception) { 0.0 }

                val iniciales1 = nombreProf1.split(" ")
                    .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")

                view.findViewById<TextView>(R.id.tvTituloServicio)?.text       = titulo1
                view.findViewById<TextView>(R.id.tvDescripcionServicio)?.text  = descripcion1
                view.findViewById<TextView>(R.id.tvPrecioServicio)?.text =
                    "$${ String.format("%,.0f", precio1)}/hr"
                view.findViewById<TextView>(R.id.tvCategoriaServicio)?.text    = categoria1
                view.findViewById<TextView>(R.id.tvNombreProfesional)?.text    = nombreProf1
                view.findViewById<TextView>(R.id.tvCalificacionServicio)?.text =
                    "⭐ ${String.format("%.1f", cal1)}"
                view.findViewById<TextView>(R.id.tvInicialesProfesional)?.text = iniciales1

                if (fotoUrl1.isNotEmpty()) {
                    view.findViewById<ImageView>(R.id.ivFotoServicio)
                        ?.load(fotoUrl1) { crossfade(true) }
                }

                view.findViewById<LinearLayout>(R.id.cardServicio)?.setOnClickListener {
                    startActivity(
                        Intent(requireContext(), DetalleServicioActivity::class.java).apply {
                            putExtra("servicio_id",    servicioId1)
                            putExtra("profesional_id", profId1)
                        }
                    )
                }

                // ── Segundo servicio — tarjeta pequeña ───────────────────────
                if (servicios.size > 1) {
                    mostrarServicioAdicional(
                        view, servicios[1],
                        R.id.cardServicio2, R.id.ivFotoServicio2,
                        R.id.tvTituloServicio2, R.id.tvPrecioServicio2
                    )
                }

                // ── Tercer servicio — tarjeta pequeña ────────────────────────
                if (servicios.size > 2) {
                    mostrarServicioAdicional(
                        view, servicios[2],
                        R.id.cardServicio3, R.id.ivFotoServicio3,
                        R.id.tvTituloServicio3, R.id.tvPrecioServicio3
                    )
                }

            } catch (e: Exception) {
                Log.e("INICIO", "Error servicios: ${e.message}", e)
            }
        }
    }

    private fun mostrarServicioAdicional(
        view: View,
        servicio: Map<String, JsonElement>,
        cardId: Int,
        ivId: Int,
        tvTituloId: Int,
        tvPrecioId: Int
    ) {
        val card = view.findViewById<LinearLayout>(cardId) ?: return
        card.visibility = View.VISIBLE

        val servicioId    = servicio["id"]?.jsonPrimitive?.content ?: ""
        val titulo        = servicio["titulo"]?.jsonPrimitive?.content ?: ""
        val precio        = servicio["precio"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val fotoUrl       = servicio["foto_url"]?.jsonPrimitive?.content ?: ""
        val profesionalId = servicio["profesional_id"]?.jsonPrimitive?.content ?: ""

        view.findViewById<TextView>(tvTituloId)?.text = titulo
        view.findViewById<TextView>(tvPrecioId)?.text =
            "$${ String.format("%,.0f", precio)}/hr"

        if (fotoUrl.isNotEmpty()) {
            view.findViewById<ImageView>(ivId)?.load(fotoUrl) { crossfade(true) }
        }

        card.setOnClickListener {
            startActivity(
                Intent(requireContext(), DetalleServicioActivity::class.java).apply {
                    putExtra("servicio_id",    servicioId)
                    putExtra("profesional_id", profesionalId)
                }
            )
        }
    }
}