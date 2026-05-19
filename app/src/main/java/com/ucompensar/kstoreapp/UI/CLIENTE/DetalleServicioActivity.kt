package com.ucompensar.kstoreapp.UI.CLIENTE

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.ChatActivity
import com.ucompensar.kstoreapp.process.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.util.Calendar

class DetalleServicioActivity : AppCompatActivity() {

    // ── MapView (reemplaza el <fragment> estático que bloqueaba el scroll) ──
    private var mapView: MapView? = null
    private var googleMap: GoogleMap? = null

    // ── Data ─────────────────────────────────────────────────────────────────
    private var servicioId        = ""
    private var profesionalId     = ""
    private var tituloServicio    = ""
    private var precioServicio    = 0.0
    private var nombreProfesional = ""
    private var latitudServicio   = 0.0
    private var longitudServicio  = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_servicio)

        window.statusBarColor = Color.parseColor("#6B2FCC")
        WindowCompat.setDecorFitsSystemWindows(window, true)

        servicioId    = intent.getStringExtra("servicio_id")    ?: ""
        profesionalId = intent.getStringExtra("profesional_id") ?: ""

        if (servicioId.isEmpty()) {
            Toast.makeText(this, "ID inválido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // ── MapView — ciclo de vida manual ───────────────────────────────────
        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync { gMap ->
            googleMap = gMap
            gMap.uiSettings.isScrollGesturesEnabled = false
            gMap.uiSettings.isZoomGesturesEnabled   = true
            gMap.uiSettings.isZoomControlsEnabled   = true
        }

        // ── Botones ──────────────────────────────────────────────────────────
        findViewById<ImageView>(R.id.btnVolver).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnVerUbicacion).setOnClickListener { mostrarUbicacionEnMapa() }
        findViewById<Button>(R.id.btnReservar).setOnClickListener { mostrarDialogoReserva() }
        findViewById<Button>(R.id.btnChat).setOnClickListener { abrirChat() }
        findViewById<ImageView>(R.id.btnFavorito).setOnClickListener { toggleFavorito() }

        cargarDetalle()
    }

    // ── Mostrar ubicación del servicio en el mapa ────────────────────────────
    private fun mostrarUbicacionEnMapa() {
        val gMap = googleMap ?: run {
            Toast.makeText(this, "El mapa aún se está cargando", Toast.LENGTH_SHORT).show()
            return
        }
        if (latitudServicio == 0.0 || longitudServicio == 0.0) {
            Toast.makeText(this,
                "Este servicio no tiene ubicación registrada", Toast.LENGTH_LONG).show()
            return
        }
        val ubicacion = LatLng(latitudServicio, longitudServicio)
        gMap.clear()
        gMap.addMarker(MarkerOptions().position(ubicacion).title("Ubicación: $tituloServicio"))
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, 16f))
    }

    // ── Cargar datos desde Supabase ──────────────────────────────────────────
    private fun cargarDetalle() {
        lifecycleScope.launch {
            try {
                // Servicio
                val servicios = SupabaseClient.client.postgrest
                    .from("servicios")
                    .select { filter { eq("id", servicioId) } }
                    .decodeList<Map<String, JsonElement>>()

                if (servicios.isEmpty()) {
                    Toast.makeText(this@DetalleServicioActivity,
                        "Servicio no encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                val servicio      = servicios.first()
                tituloServicio    = servicio["titulo"]?.jsonPrimitive?.content ?: ""
                precioServicio    = servicio["precio"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                val descripcion   = servicio["descripcion"]?.jsonPrimitive?.content ?: ""
                val categoria     = servicio["categoria"]?.jsonPrimitive?.content ?: ""
                val fotoUrl       = servicio["foto_url"]?.jsonPrimitive?.content ?: ""
                // Coordenadas — agrega columnas latitud/longitud en Supabase para activarlas
                latitudServicio   = servicio["latitud"]?.jsonPrimitive?.doubleOrNull  ?: 0.0
                longitudServicio  = servicio["longitud"]?.jsonPrimitive?.doubleOrNull ?: 0.0

                // Profile del profesional
                val profiles = SupabaseClient.client.postgrest
                    .from("profiles")
                    .select { filter { eq("id", profesionalId) } }
                    .decodeList<Map<String, JsonElement>>()

                if (profiles.isEmpty()) {
                    Toast.makeText(this@DetalleServicioActivity,
                        "Profesional no encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                val profile       = profiles.first()
                nombreProfesional = profile["nombre"]?.jsonPrimitive?.content ?: "Profesional"
                val ciudad        = profile["ciudad"]?.jsonPrimitive?.content ?: ""
                val fotoProfUrl   = profile["foto_url"]?.jsonPrimitive?.content ?: ""

                // Datos del profesional (calificación)
                val profData = try {
                    SupabaseClient.client.postgrest
                        .from("profesionales")
                        .select { filter { eq("id", profesionalId) } }
                        .decodeList<Map<String, JsonElement>>()
                        .firstOrNull()
                } catch (_: Exception) { null }

                val calificacion = profData?.get("calificacion")?.jsonPrimitive?.doubleOrNull ?: 0.0
                val totalResenas = profData?.get("total_resenas")?.jsonPrimitive?.content ?: "0"

                val iniciales = nombreProfesional.split(" ")
                    .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("")

                // ── Actualizar UI ────────────────────────────────────────────
                findViewById<TextView>(R.id.tvTituloServicio).text    = tituloServicio
                findViewById<TextView>(R.id.tvDescripcionServicio).text = descripcion
                findViewById<TextView>(R.id.tvCategoria).text         = categoria
                findViewById<TextView>(R.id.tvPrecio).text =
                    "$${ String.format("%,.0f", precioServicio)}/hr"
                findViewById<TextView>(R.id.tvNombreProfesional).text = nombreProfesional
                findViewById<TextView>(R.id.tvCiudad).text            = ciudad
                findViewById<TextView>(R.id.tvCalificacion).text =
                    "⭐ ${String.format("%.1f", calificacion)} ($totalResenas reseñas)"

                // Avatar profesional: foto o iniciales
                val tvIniciales = findViewById<TextView>(R.id.tvIniciales)
                val ivFotoProf  = findViewById<ImageView>(R.id.ivFotoProfesional)
                if (fotoProfUrl.isNotEmpty()) {
                    tvIniciales?.visibility = View.GONE
                    ivFotoProf?.visibility  = View.VISIBLE
                    ivFotoProf?.load(fotoProfUrl) {
                        transformations(CircleCropTransformation())
                        placeholder(R.drawable.bg_avatar_purple)
                    }
                } else {
                    tvIniciales?.text       = iniciales
                    ivFotoProf?.visibility  = View.GONE
                }

                // Foto del servicio
                if (fotoUrl.isNotEmpty()) {
                    findViewById<ImageView>(R.id.ivFotoServicio)
                        ?.load(fotoUrl) { crossfade(true) }
                }

                // ✅ Verificar favorito siempre al terminar de cargar
                verificarFavorito()

            } catch (e: Exception) {
                Log.e("DETALLE", "Error: ${e.message}", e)
                Toast.makeText(this@DetalleServicioActivity,
                    "Error al cargar servicio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Reserva ──────────────────────────────────────────────────────────────
    private fun mostrarDialogoReserva() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            TimePickerDialog(this, { _, h, min ->
                confirmarReserva(
                    String.format("%04d-%02d-%02d", y, m + 1, d),
                    String.format("%02d:%02d:00", h, min)
                )
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun confirmarReserva(fecha: String, hora: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar reserva")
            .setMessage("¿Reservar $tituloServicio con $nombreProfesional el $fecha a las $hora?")
            .setPositiveButton("Confirmar") { _, _ -> crearPedido(fecha, hora) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun crearPedido(fecha: String, hora: String) {
        lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                    ?: return@launch
                SupabaseClient.client.postgrest
                    .from("pedidos")
                    .insert(buildJsonObject {
                        put("cliente_id",    uid)
                        put("profesional_id", profesionalId)
                        put("servicio_id",    servicioId)
                        put("estado",         "pendiente")
                        put("fecha_servicio", fecha)
                        put("hora_servicio",  hora)
                    })
                Toast.makeText(this@DetalleServicioActivity,
                    "¡Reserva enviada correctamente!", Toast.LENGTH_LONG).show()
                finish()
            } catch (e: Exception) {
                Log.e("DETALLE", "Error reservando: ${e.message}", e)
                Toast.makeText(this@DetalleServicioActivity,
                    "Error al reservar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Chat ─────────────────────────────────────────────────────────────────
    private fun abrirChat() {
        lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                    ?: return@launch

                val conv = SupabaseClient.client.postgrest
                    .from("conversaciones")
                    .select { filter {
                        eq("cliente_id",    uid)
                        eq("profesional_id", profesionalId)
                    }}
                    .decodeList<Map<String, JsonElement>>()

                val convId = if (conv.isNotEmpty()) {
                    conv.first()["id"]?.jsonPrimitive?.content ?: ""
                } else {
                    val nueva = SupabaseClient.client.postgrest
                        .from("conversaciones")
                        .insert(buildJsonObject {
                            put("cliente_id",    uid)
                            put("profesional_id", profesionalId)
                            put("ultimo_mensaje", "")
                        })
                        .decodeSingle<Map<String, JsonElement>>()
                    nueva["id"]?.jsonPrimitive?.content ?: ""
                }

                startActivity(
                    Intent(this@DetalleServicioActivity, ChatActivity::class.java).apply {
                        putExtra("conversacion_id", convId)
                        putExtra("otro_nombre",     nombreProfesional)
                        putExtra("otro_id",         profesionalId)
                    }
                )
            } catch (e: Exception) {
                Log.e("DETALLE", "Error chat: ${e.message}", e)
                Toast.makeText(this@DetalleServicioActivity,
                    "Error al abrir chat", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Favoritos ────────────────────────────────────────────────────────────
    private fun verificarFavorito() {
        lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                    ?: return@launch
                val favs = SupabaseClient.client.postgrest
                    .from("favoritos")
                    .select { filter {
                        eq("cliente_id",  uid)
                        eq("servicio_id", servicioId)
                    }}
                    .decodeList<Map<String, JsonElement>>()

                runOnUiThread {
                    findViewById<ImageView>(R.id.btnFavorito)?.setColorFilter(
                        if (favs.isNotEmpty()) getColor(R.color.primary)
                        else                   getColor(android.R.color.white)
                    )
                }
            } catch (e: Exception) {
                Log.e("DETALLE", "Error verificar fav: ${e.message}", e)
            }
        }
    }

    private fun toggleFavorito() {
        lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                    ?: return@launch

                val favs = SupabaseClient.client.postgrest
                    .from("favoritos")
                    .select { filter {
                        eq("cliente_id",  uid)
                        eq("servicio_id", servicioId)
                    }}
                    .decodeList<Map<String, JsonElement>>()

                if (favs.isNotEmpty()) {
                    val favId = favs.first()["id"]?.jsonPrimitive?.content ?: return@launch
                    SupabaseClient.client.postgrest
                        .from("favoritos")
                        .delete { filter { eq("id", favId) } }
                    Toast.makeText(this@DetalleServicioActivity,
                        "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
                } else {
                    SupabaseClient.client.postgrest
                        .from("favoritos")
                        .insert(buildJsonObject {
                            put("cliente_id",  uid)
                            put("servicio_id", servicioId)
                        })
                    Toast.makeText(this@DetalleServicioActivity,
                        "Agregado a favoritos ❤️", Toast.LENGTH_SHORT).show()
                }
                verificarFavorito()

            } catch (e: Exception) {
                Log.e("DETALLE", "Error toggle fav: ${e.message}", e)
            }
        }
    }

    // ── Ciclo de vida del MapView — obligatorio ──────────────────────────────
    override fun onResume()  { super.onResume();  mapView?.onResume() }
    override fun onPause()   { super.onPause();   mapView?.onPause() }
    override fun onStart()   { super.onStart();   mapView?.onStart() }
    override fun onStop()    { super.onStop();    mapView?.onStop() }
    override fun onDestroy() { super.onDestroy(); mapView?.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView?.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }
}