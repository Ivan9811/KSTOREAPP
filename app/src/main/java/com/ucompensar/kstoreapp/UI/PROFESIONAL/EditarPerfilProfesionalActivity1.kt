package com.ucompensar.kstoreapp.UI.PROFESIONAL

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.process.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.io.File

class EditarPerfilProfesionalActivity : AppCompatActivity() {

    private lateinit var ivAvatar     : ImageView
    private lateinit var tvIniciales  : TextView
    private lateinit var etNombre     : EditText
    private lateinit var etCorreo     : EditText
    private lateinit var etTelefono   : EditText
    private lateinit var etCiudad     : EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etPrecio     : EditText

    private var uriFotoTemporal: Uri? = null
    private var fotoUrlActual: String? = null
    private val diasSeleccionados = mutableSetOf<String>()

    companion object {
        private const val NOMBRE_ARCHIVO_CAMARA = "foto_perfil_prof_temp.jpg"
    }

    // ── Galería ───────────────────────────────────────────────
    private val galeria = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { subirFotoComprimida(it) } }

    // ── Cámara ────────────────────────────────────────────────
    private val camara = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exitoso -> if (exitoso) uriFotoTemporal?.let { subirFotoComprimida(it) } }

    // ── Permiso cámara ────────────────────────────────────────
    private val permisoCamara = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) abrirCamara()
        else Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_perfil_profesional1)

        window.statusBarColor = "#FFFFFF".toColorInt()
        WindowCompat.setDecorFitsSystemWindows(window, true)

        ivAvatar      = findViewById(R.id.ivAvatar)
        tvIniciales   = findViewById(R.id.tvIniciales)
        etNombre      = findViewById(R.id.etNombre)
        etCorreo      = findViewById(R.id.etCorreo)
        etTelefono    = findViewById(R.id.etTelefono)
        etCiudad      = findViewById(R.id.etCiudad)
        etDescripcion = findViewById(R.id.etDescripcion)
        etPrecio      = findViewById(R.id.etPrecio)

        etCorreo.isEnabled = false
        etCorreo.alpha = 0.6f

        findViewById<ImageView>(R.id.btnVolver).setOnClickListener { finish() }
        findViewById<FrameLayout>(R.id.btnCamara).setOnClickListener { mostrarOpcionesFoto() }
        findViewById<Button>(R.id.btnGuardar).setOnClickListener { guardarCambios() }

        configurarDias()
        cargarDatos()
    }

    // ── Toggle días ───────────────────────────────────────────
    private fun configurarDias() {
        val dias = mapOf(
            R.id.diaLun to "Lun",
            R.id.diaMar to "Mar",
            R.id.diaMie to "Mié",
            R.id.diaJue to "Jue",
            R.id.diaVie to "Vie",
            R.id.diaSab to "Sáb",
            R.id.diaDom to "Dom"
        )
        dias.forEach { (id, nombre) ->
            val tv = findViewById<TextView>(id)
            tv.setOnClickListener {
                if (diasSeleccionados.contains(nombre)) {
                    diasSeleccionados.remove(nombre)
                    tv.setBackgroundResource(R.drawable.bg_chip_unselected)
                    tv.setTextColor(getColor(R.color.morado_tarjetas))
                } else {
                    diasSeleccionados.add(nombre)
                    tv.setBackgroundResource(R.drawable.bg_chip_selected)
                    tv.setTextColor(getColor(android.R.color.white))
                }
            }
        }
    }

    // ── Marcar días guardados visualmente ─────────────────────
    private fun marcarDias(dias: List<String>) {
        val mapaDias = mapOf(
            "Lun" to R.id.diaLun, "Mar" to R.id.diaMar, "Mié" to R.id.diaMie,
            "Jue" to R.id.diaJue, "Vie" to R.id.diaVie, "Sáb" to R.id.diaSab,
            "Dom" to R.id.diaDom
        )
        dias.forEach { dia ->
            diasSeleccionados.add(dia)
            mapaDias[dia]?.let { id ->
                val tv = findViewById<TextView>(id)
                tv.setBackgroundResource(R.drawable.bg_chip_selected)
                tv.setTextColor(getColor(android.R.color.white))
            }
        }
    }

    // ── Opciones foto ─────────────────────────────────────────
    private fun mostrarOpcionesFoto() {
        AlertDialog.Builder(this)
            .setTitle("Foto de perfil")
            .setItems(arrayOf("Tomar foto", "Elegir de galería")) { _, which ->
                when (which) {
                    0 -> verificarPermisoCamara()
                    1 -> galeria.launch("image/*")
                }
            }.show()
    }

    private fun verificarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) abrirCamara()
        else permisoCamara.launch(Manifest.permission.CAMERA)
    }

    private fun abrirCamara() {
        try {
            val archivo = File(getExternalFilesDir(null), NOMBRE_ARCHIVO_CAMARA)
            archivo.parentFile?.mkdirs()
            uriFotoTemporal = FileProvider.getUriForFile(
                this, "${packageName}.fileprovider", archivo  // ✅ como estaba
            )
            camara.launch(uriFotoTemporal!!)
        } catch (e: Exception) {
            Log.e("FOTO_PROF", "Error abriendo cámara: ${e.message}")
            Toast.makeText(this, "Error al abrir cámara", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Comprimir y subir foto ────────────────────────────────
    private fun subirFotoComprimida(uri: Uri) {
        ivAvatar.load(uri) {
            transformations(CircleCropTransformation())
            placeholder(R.drawable.bg_avatar_purple)
        }
        tvIniciales.text = ""

        lifecycleScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { comprimirImagen(uri) } ?: run {
                    Toast.makeText(this@EditarPerfilProfesionalActivity,
                        "Error al procesar imagen", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val url = withContext(Dispatchers.IO) { subirBytesAStorage(bytes) }

                if (url.isNotEmpty()) {
                    val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                        ?: return@launch
                    SupabaseClient.client.postgrest
                        .from("profiles")
                        .update(buildJsonObject { put("foto_url", url) }) {
                            filter { eq("id", uid) }
                        }
                    fotoUrlActual = url
                    Toast.makeText(this@EditarPerfilProfesionalActivity,
                        "Foto actualizada ✓", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("FOTO_PROF", "Error subiendo foto: ${e.message}", e)
                Toast.makeText(this@EditarPerfilProfesionalActivity,
                    "Error al subir foto: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun comprimirImagen(uri: Uri): ByteArray? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            val bitmapRedim = if (bitmap.width > 800 || bitmap.height > 800) {
                val ratio = minOf(800f / bitmap.width, 800f / bitmap.height)
                Bitmap.createScaledBitmap(bitmap,
                    (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
            } else bitmap
            val output = ByteArrayOutputStream()
            bitmapRedim.compress(Bitmap.CompressFormat.JPEG, 80, output)
            output.toByteArray()
        } catch (e: Exception) {
            Log.e("FOTO_PROF", "Error comprimiendo: ${e.message}")
            null
        }
    }

    private suspend fun subirBytesAStorage(bytes: ByteArray): String {
        val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: return ""
        val path = "perfil/$uid.jpg"
        SupabaseClient.client.storage.from("avatars").upload(path, bytes) { upsert = true }
        return SupabaseClient.client.storage.from("avatars").publicUrl(path)
    }

    // ── Cargar datos ──────────────────────────────────────────
    private fun cargarDatos() {
        lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                    ?: return@launch

                val profile = SupabaseClient.client.postgrest
                    .from("profiles")
                    .select { filter { eq("id", uid) } }
                    .decodeSingle<Map<String, JsonElement>>()

                val nombre = profile["nombre"]?.jsonPrimitive?.content ?: ""
                etNombre.setText(nombre)
                etCorreo.setText(profile["email"]?.jsonPrimitive?.content ?: "")
                etTelefono.setText(profile["telefono"]?.jsonPrimitive?.content ?: "")
                etCiudad.setText(profile["ciudad"]?.jsonPrimitive?.content ?: "")

                fotoUrlActual = profile["foto_url"]?.jsonPrimitive?.content
                if (!fotoUrlActual.isNullOrEmpty()) {
                    tvIniciales.text = ""
                    ivAvatar.load(fotoUrlActual) {
                        transformations(CircleCropTransformation())
                        placeholder(R.drawable.bg_avatar_purple)
                    }
                } else {
                    tvIniciales.text = nombre.split(" ")
                        .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .joinToString("")
                }

                // Cargar datos profesionales
                try {
                    val prof = SupabaseClient.client.postgrest
                        .from("profesionales")
                        .select { filter { eq("id", uid) } }
                        .decodeSingle<Map<String, JsonElement>>()

                    etDescripcion.setText(prof["descripcion"]?.jsonPrimitive?.content ?: "")
                    etPrecio.setText(prof["precio"]?.jsonPrimitive?.content ?: "")

                    val diasGuardados = prof["disponibilidad"]?.jsonArray
                        ?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
                    marcarDias(diasGuardados)

                } catch (e: Exception) {
                    Log.e("EDITAR_PROF", "Sin datos en profesionales: ${e.message}")
                }

            } catch (e: Exception) {
                Log.e("EDITAR_PROF", "Error cargarDatos: ${e.message}", e)
                Toast.makeText(this@EditarPerfilProfesionalActivity,
                    "Error al cargar datos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Guardar cambios ───────────────────────────────────────
    private fun guardarCambios() {
        val nombre      = etNombre.text.toString().trim()
        val telefono    = etTelefono.text.toString().trim()
        val ciudad      = etCiudad.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val precio      = etPrecio.text.toString().trim()

        if (nombre.isEmpty()) { etNombre.error = "El nombre es obligatorio"; return }

        lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                    ?: return@launch

                SupabaseClient.client.postgrest
                    .from("profiles")
                    .update(buildJsonObject {
                        put("nombre",   nombre)
                        put("telefono", telefono)
                        put("ciudad",   ciudad)
                    }) { filter { eq("id", uid) } }

                SupabaseClient.client.postgrest
                    .from("profesionales")
                    .upsert(buildJsonObject {
                        put("id",          uid)
                        put("descripcion", descripcion)
                        put("precio",      precio.toDoubleOrNull() ?: 0.0)
                        put("disponibilidad", buildJsonArray {
                            diasSeleccionados.forEach { add(it) }
                        })
                    })

                Toast.makeText(this@EditarPerfilProfesionalActivity,
                    "Perfil actualizado ✓", Toast.LENGTH_SHORT).show()
                finish()

            } catch (e: Exception) {
                Log.e("EDITAR_PROF", "Error guardarCambios: ${e.message}", e)
                Toast.makeText(this@EditarPerfilProfesionalActivity,
                    "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}