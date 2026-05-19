package com.ucompensar.kstoreapp.UI.PROFESIONAL.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.gms.location.LocationServices
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.process.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

class publicacionProfesional : Fragment() {

    private lateinit var etTitulo             : EditText
    private lateinit var tvCategoria          : TextView
    private lateinit var etDescripcion        : EditText
    private lateinit var etPrecio             : EditText
    private lateinit var etDireccion          : EditText
    private lateinit var tvUbicacionDetectada : TextView
    private lateinit var btnDetectarUbicacion : Button
    private lateinit var layoutFotos          : FrameLayout
    private lateinit var ivFotoPreview        : ImageView
    private lateinit var layoutPlaceholderFoto: LinearLayout
    private lateinit var btnPublicar          : Button

    private lateinit var diaLun: TextView
    private lateinit var diaMar: TextView
    private lateinit var diaMie: TextView
    private lateinit var diaJue: TextView
    private lateinit var diaVie: TextView
    private lateinit var diaSab: TextView
    private lateinit var diaDom: TextView

    private val diasSeleccionados     = mutableSetOf<String>()
    private var categoriaSeleccionada : String? = null
    private var uriFotoTemporal       : Uri?    = null
    private var fotoUrlSubida         : String? = null
    private var fotoSubiendose        = false

    // ✅ Coordenadas del servicio
    private var latitudServicio  = 0.0
    private var longitudServicio = 0.0

    private val categorias = listOf(
        "Electricidad", "Plomería", "Carpintería", "Pintura",
        "Jardinería", "Limpieza", "Cerrajería", "Aire acondicionado", "Otro"
    )

    companion object {
        private const val NOMBRE_ARCHIVO_CAMARA = "foto_servicio_temp.jpg"
        private const val BUCKET = "avatars"
    }

    // ── Galería ───────────────────────────────────────────────────────────────
    private val galeria = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { procesarFotoSeleccionada(it) }
            ?: Toast.makeText(requireContext(), "No se seleccionó imagen", Toast.LENGTH_SHORT).show()
    }

    // ── Cámara ────────────────────────────────────────────────────────────────
    private val camara = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exitoso ->
        if (exitoso) uriFotoTemporal?.let { procesarFotoSeleccionada(it) }
        else Toast.makeText(requireContext(), "Foto cancelada", Toast.LENGTH_SHORT).show()
    }

    // ── Permiso cámara ────────────────────────────────────────────────────────
    private val permisoCamara = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) abrirCamara()
        else Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
    }

    // ── Permiso ubicación ─────────────────────────────────────────────────────
    private val permisoUbicacion = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val ok = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) obtenerUbicacionGPS()
        else Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_publicacion_profesional, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etTitulo              = view.findViewById(R.id.etTitulo)
        tvCategoria           = view.findViewById(R.id.tvCategoria)
        etDescripcion         = view.findViewById(R.id.etDescripcion)
        etPrecio              = view.findViewById(R.id.etPrecio)
        etDireccion           = view.findViewById(R.id.etDireccion)
        tvUbicacionDetectada  = view.findViewById(R.id.tvUbicacionDetectada)
        btnDetectarUbicacion  = view.findViewById(R.id.btnDetectarUbicacion)
        layoutFotos           = view.findViewById(R.id.layoutFotos)
        ivFotoPreview         = view.findViewById(R.id.ivFotoPreview)
        layoutPlaceholderFoto = view.findViewById(R.id.layoutPlaceholderFoto)
        btnPublicar           = view.findViewById(R.id.btnPublicar)

        diaLun = view.findViewById(R.id.diaLun)
        diaMar = view.findViewById(R.id.diaMar)
        diaMie = view.findViewById(R.id.diaMie)
        diaJue = view.findViewById(R.id.diaJue)
        diaVie = view.findViewById(R.id.diaVie)
        diaSab = view.findViewById(R.id.diaSab)
        diaDom = view.findViewById(R.id.diaDom)

        configurarDias()
        configurarCategoria()

        layoutFotos.setOnClickListener { mostrarOpcionesFoto() }
        ivFotoPreview.setOnClickListener { mostrarOpcionesFoto() }
        btnDetectarUbicacion.setOnClickListener { verificarPermisoUbicacion() }
        btnPublicar.setOnClickListener { publicarServicio() }

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    // ── GPS ───────────────────────────────────────────────────────────────────
    private fun verificarPermisoUbicacion() {
        val fineOk   = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseOk = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fineOk == PackageManager.PERMISSION_GRANTED || coarseOk == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionGPS()
        } else {
            permisoUbicacion.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun obtenerUbicacionGPS() {
        val fineOk = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineOk != PackageManager.PERMISSION_GRANTED) return

        btnDetectarUbicacion.isEnabled = false
        btnDetectarUbicacion.text      = "Detectando..."

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    latitudServicio  = location.latitude
                    longitudServicio = location.longitude
                    try {
                        val geocoder  = Geocoder(requireContext(), Locale.getDefault())
                        val addresses = geocoder.getFromLocation(latitudServicio, longitudServicio, 1)
                        val direccion = addresses?.firstOrNull()?.getAddressLine(0) ?: ""
                        tvUbicacionDetectada.text       = "📍 $direccion"
                        tvUbicacionDetectada.visibility = View.VISIBLE
                        etDireccion.setText(direccion)
                    } catch (_: Exception) {
                        tvUbicacionDetectada.text       = "📍 Lat: $latitudServicio, Lng: $longitudServicio"
                        tvUbicacionDetectada.visibility = View.VISIBLE
                    }
                    Toast.makeText(requireContext(), "Ubicación detectada ✓", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(),
                        "No se pudo obtener ubicación. Ingresa la dirección manualmente.", Toast.LENGTH_LONG).show()
                }
                btnDetectarUbicacion.isEnabled = true
                btnDetectarUbicacion.text      = "📍 Detectar mi ubicación"
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Error al obtener ubicación", Toast.LENGTH_SHORT).show()
                btnDetectarUbicacion.isEnabled = true
                btnDetectarUbicacion.text      = "📍 Detectar mi ubicación"
            }
        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), "Error de permisos", Toast.LENGTH_SHORT).show()
            btnDetectarUbicacion.isEnabled = true
            btnDetectarUbicacion.text      = "📍 Detectar mi ubicación"
        }
    }

    // ── Días ──────────────────────────────────────────────────────────────────
    private fun configurarDias() {
        mapOf(
            diaLun to "Lun", diaMar to "Mar", diaMie to "Mié",
            diaJue to "Jue", diaVie to "Vie", diaSab to "Sáb", diaDom to "Dom"
        ).forEach { (vista, nombre) -> vista.setOnClickListener { toggleDia(vista, nombre) } }
    }

    private fun toggleDia(vista: TextView, nombre: String) {
        if (diasSeleccionados.contains(nombre)) {
            diasSeleccionados.remove(nombre)
            vista.setBackgroundResource(R.drawable.bg_chip_unselected)
            vista.setTextColor(resources.getColor(R.color.morado_tarjetas, null))
        } else {
            diasSeleccionados.add(nombre)
            vista.setBackgroundResource(R.drawable.bg_chip_selected)
            vista.setTextColor(resources.getColor(R.color.white, null))
        }
    }

    // ── Categoría ─────────────────────────────────────────────────────────────
    private fun configurarCategoria() {
        tvCategoria.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Selecciona una categoría")
                .setItems(categorias.toTypedArray()) { _, which ->
                    categoriaSeleccionada = categorias[which]
                    tvCategoria.text = categoriaSeleccionada
                    tvCategoria.setTextColor(resources.getColor(R.color.Texto1, null))
                }.show()
        }
    }

    // ── Foto ──────────────────────────────────────────────────────────────────
    private fun mostrarOpcionesFoto() {
        AlertDialog.Builder(requireContext())
            .setTitle("Foto del servicio")
            .setItems(arrayOf("Tomar foto", "Elegir de galería")) { _, which ->
                when (which) { 0 -> verificarPermisoCamara(); 1 -> galeria.launch("image/*") }
            }.show()
    }

    private fun verificarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) abrirCamara()
        else permisoCamara.launch(Manifest.permission.CAMERA)
    }

    private fun abrirCamara() {
        try {
            val archivo = File(requireContext().getExternalFilesDir(null), NOMBRE_ARCHIVO_CAMARA)
            archivo.parentFile?.mkdirs()
            uriFotoTemporal = FileProvider.getUriForFile(
                requireContext(), "${requireContext().packageName}.fileprovider", archivo)
            camara.launch(uriFotoTemporal!!)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al abrir cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private fun procesarFotoSeleccionada(uri: Uri) {
        ivFotoPreview.load(uri) { crossfade(true) }
        ivFotoPreview.visibility         = View.VISIBLE
        layoutPlaceholderFoto.visibility = View.GONE
        fotoSubiendose        = true
        btnPublicar.isEnabled = false
        btnPublicar.text      = "Subiendo foto..."

        lifecycleScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { comprimirImagen(uri) }
                if (bytes == null) {
                    withContext(Dispatchers.Main) {
                        fotoSubiendose = false; btnPublicar.isEnabled = true
                        btnPublicar.text = "Publicar Servicio"
                        Toast.makeText(requireContext(), "Error al procesar imagen", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: run {
                    fotoSubiendose = false; return@launch
                }
                val path = "servicios/$uid/${System.currentTimeMillis()}.jpg"
                withContext(Dispatchers.IO) {
                    SupabaseClient.client.storage.from(BUCKET).upload(path, bytes) { upsert = true }
                }
                fotoUrlSubida = SupabaseClient.client.storage.from(BUCKET).publicUrl(path)
                withContext(Dispatchers.Main) {
                    fotoSubiendose = false; btnPublicar.isEnabled = true
                    btnPublicar.text = "Publicar Servicio"
                    Toast.makeText(requireContext(), "Foto lista ✓", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    fotoSubiendose = false; btnPublicar.isEnabled = true
                    btnPublicar.text = "Publicar Servicio"
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun comprimirImagen(uri: Uri): ByteArray? {
        return try {
            val input  = requireContext().contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(input); input.close()
            val redim  = if (bitmap.width > 800 || bitmap.height > 800) {
                val ratio = minOf(800f / bitmap.width, 800f / bitmap.height)
                Bitmap.createScaledBitmap(bitmap,
                    (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
            } else bitmap
            val out = ByteArrayOutputStream()
            redim.compress(Bitmap.CompressFormat.JPEG, 80, out)
            out.toByteArray()
        } catch (e: Exception) { null }
    }

    // ── Publicar ──────────────────────────────────────────────────────────────
    private fun publicarServicio() {
        val titulo      = etTitulo.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val precioStr   = etPrecio.text.toString().trim()

        if (titulo.isEmpty())            { etTitulo.error = "El título es obligatorio"; return }
        if (categoriaSeleccionada == null) {
            Toast.makeText(requireContext(), "Selecciona una categoría", Toast.LENGTH_SHORT).show(); return }
        if (descripcion.isEmpty())       { etDescripcion.error = "La descripción es obligatoria"; return }
        if (precioStr.isEmpty())         { etPrecio.error = "El precio es obligatorio"; return }
        if (diasSeleccionados.isEmpty()) {
            Toast.makeText(requireContext(), "Selecciona al menos un día", Toast.LENGTH_SHORT).show(); return }
        if (fotoSubiendose) {
            Toast.makeText(requireContext(), "Espera a que termine de subir la foto", Toast.LENGTH_SHORT).show(); return }

        val precio = precioStr.toDoubleOrNull() ?: run { etPrecio.error = "Precio inválido"; return }

        btnPublicar.isEnabled = false
        btnPublicar.text      = "Publicando..."

        lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: return@launch

                SupabaseClient.client.postgrest
                    .from("servicios")
                    .insert(buildJsonObject {
                        put("profesional_id", uid)
                        put("titulo",         titulo)
                        put("descripcion",    descripcion)
                        put("precio",         precio)
                        put("categoria",      categoriaSeleccionada!!)
                        put("foto_url",       fotoUrlSubida ?: "")
                        put("activo",         true)
                        // ✅ Guardar coordenadas GPS en Supabase
                        put("latitud",        latitudServicio)
                        put("longitud",       longitudServicio)
                    })

                SupabaseClient.client.postgrest
                    .from("profesionales")
                    .update(buildJsonObject {
                        put("disponibilidad", buildJsonArray {
                            diasSeleccionados.forEach { add(JsonPrimitive(it)) }
                        })
                    }) { filter { eq("id", uid) } }

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Servicio publicado ✓", Toast.LENGTH_SHORT).show()
                    limpiarFormulario()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    btnPublicar.isEnabled = true
                    btnPublicar.text      = "Publicar Servicio"
                }
            }
        }
    }

    // ── Limpiar ───────────────────────────────────────────────────────────────
    private fun limpiarFormulario() {
        etTitulo.setText(""); etDescripcion.setText(""); etPrecio.setText("")
        etDireccion.setText(""); tvUbicacionDetectada.visibility = View.GONE
        tvCategoria.text = "Selecciona una categoría"
        tvCategoria.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        categoriaSeleccionada = null; fotoUrlSubida = null
        fotoSubiendose = false; latitudServicio = 0.0; longitudServicio = 0.0
        ivFotoPreview.visibility = View.GONE; layoutPlaceholderFoto.visibility = View.VISIBLE
        diasSeleccionados.clear()
        listOf(diaLun, diaMar, diaMie, diaJue, diaVie, diaSab, diaDom).forEach {
            it.setBackgroundResource(R.drawable.bg_chip_unselected)
            it.setTextColor(resources.getColor(R.color.morado_tarjetas, null))
        }
    }
}