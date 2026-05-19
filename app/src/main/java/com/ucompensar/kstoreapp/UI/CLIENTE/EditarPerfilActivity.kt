package com.ucompensar.kstoreapp.UI.CLIENTE

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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.io.File

class EditarPerfilActivity : AppCompatActivity() {

    private lateinit var ivAvatar   : ImageView
    private lateinit var tvIniciales: TextView
    private lateinit var etNombre   : EditText
    private lateinit var etCorreo   : EditText
    private lateinit var etTelefono : EditText
    private lateinit var etCiudad   : EditText

    private var uriFotoTemporal: Uri? = null
    private var fotoUrlActual: String? = null

    companion object {
        private const val NOMBRE_ARCHIVO_CAMARA = "foto_perfil_temp.jpg"
    }

    // ── Galería ───────────────────────────────────────────────
    private val galeria = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        Log.d("FOTO", "Galería resultado: $uri")
        uri?.let { subirFotoComprimida(it) } ?: Log.e("FOTO", "URI de galería es null")
    }

    // ── Cámara ────────────────────────────────────────────────
    private val camara = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exitoso ->
        Log.d("FOTO", "Cámara resultado: $exitoso — uri: $uriFotoTemporal")
        if (exitoso) uriFotoTemporal?.let { subirFotoComprimida(it) }
        else Log.e("FOTO", "Foto no tomada o cancelada")
    }

    // ── Permiso cámara ────────────────────────────────────────
    private val permisoCamara = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) abrirCamara()
        else Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_perfil)

        window.statusBarColor = "#FFFFFF".toColorInt()
        WindowCompat.setDecorFitsSystemWindows(window, true)

        ivAvatar    = findViewById(R.id.ivAvatar)
        tvIniciales = findViewById(R.id.tvIniciales)
        etNombre    = findViewById(R.id.etNombre)
        etCorreo    = findViewById(R.id.etCorreo)
        etTelefono  = findViewById(R.id.etTelefono)
        etCiudad    = findViewById(R.id.etCiudad)

        etCorreo.isEnabled = false
        etCorreo.alpha = 0.6f

        findViewById<ImageView>(R.id.btnVolver).setOnClickListener { finish() }
        findViewById<FrameLayout>(R.id.btnCamara).setOnClickListener { mostrarOpcionesFoto() }
        findViewById<Button>(R.id.btnGuardar).setOnClickListener { guardarCambios() }

        cargarDatos()
    }

    // ── Opciones: cámara o galería ────────────────────────────
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
            == PackageManager.PERMISSION_GRANTED
        ) abrirCamara()
        else permisoCamara.launch(Manifest.permission.CAMERA)
    }

    private fun abrirCamara() {
        try {
            // ✅ CORRECCIÓN 1: usar getExternalFilesDir en vez de cacheDir
            val archivo = File(getExternalFilesDir(null), NOMBRE_ARCHIVO_CAMARA)
            archivo.parentFile?.mkdirs()

            // ✅ CORRECCIÓN 2: usar "fileprovider" (debe coincidir con AndroidManifest)
            uriFotoTemporal = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                archivo
            )
            camara.launch(uriFotoTemporal!!)
        } catch (e: Exception) {
            Log.e("FOTO", "Error abriendo cámara: ${e.message}")
            Toast.makeText(this, "Error al abrir cámara", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Comprimir y subir foto ────────────────────────────────
    private fun subirFotoComprimida(uri: Uri) {
        // ✅ CORRECCIÓN 3: mostrar imagen localmente de inmediato sin esperar subida
        ivAvatar.load(uri) {
            transformations(CircleCropTransformation())
            placeholder(R.drawable.bg_avatar_purple)
        }
        tvIniciales.text = ""

        lifecycleScope.launch {
            try {
                // Comprimir en background
                val bytesComprimidos = withContext(Dispatchers.IO) {
                    comprimirImagen(uri)
                }

                if (bytesComprimidos == null) {
                    Toast.makeText(this@EditarPerfilActivity,
                        "Error al procesar imagen", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Subir bytes comprimidos
                val url = withContext(Dispatchers.IO) {
                    subirBytesAStorage(bytesComprimidos)
                }

                if (url.isNotEmpty()) {
                    // Actualizar foto_url en profiles
                    SupabaseClient.client.postgrest
                        .from("profiles")
                        .update(buildJsonObject { put("foto_url", url) }) {
                            filter {
                                eq("id", SupabaseClient.client.auth
                                    .currentSessionOrNull()?.user?.id ?: "")
                            }
                        }
                    fotoUrlActual = url
                    Toast.makeText(this@EditarPerfilActivity,
                        "Foto actualizada ✓", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("FOTO", "Error subiendo foto: ${e.message}", e)
                Toast.makeText(this@EditarPerfilActivity,
                    "Error al subir foto: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ✅ Comprimir imagen a máximo ~500KB
    private fun comprimirImagen(uri: Uri): ByteArray? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val bitmapRedim = if (bitmap.width > 800 || bitmap.height > 800) {
                val ratio = minOf(800f / bitmap.width, 800f / bitmap.height)
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt(),
                    (bitmap.height * ratio).toInt(),
                    true
                )
            } else bitmap

            val output = ByteArrayOutputStream()
            bitmapRedim.compress(Bitmap.CompressFormat.JPEG, 80, output)
            output.toByteArray()
        } catch (e: Exception) {
            Log.e("FOTO", "Error comprimiendo: ${e.message}")
            null
        }
    }

    // ✅ Subir bytes al bucket avatars de Supabase
    private suspend fun subirBytesAStorage(bytes: ByteArray): String {
        val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: return ""
        val path = "perfil/$uid.jpg"

        SupabaseClient.client.storage
            .from("avatars")
            .upload(path, bytes) { upsert = true }

        return SupabaseClient.client.storage
            .from("avatars")
            .publicUrl(path)
    }

    // ── Mostrar foto o iniciales ──────────────────────────────
    private fun mostrarFoto(url: String?) {
        if (!url.isNullOrEmpty()) {
            tvIniciales.text = ""
            ivAvatar.load(url) {
                transformations(CircleCropTransformation())
                placeholder(R.drawable.bg_avatar_purple)
            }
        }
    }

    // ── Cargar datos desde Supabase ───────────────────────────
    private fun cargarDatos() {
        lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                    ?: return@launch

                val resultado = SupabaseClient.client.postgrest
                    .from("profiles")
                    .select { filter { eq("id", uid) } }
                    .decodeSingle<Map<String, kotlinx.serialization.json.JsonElement>>()

                val nombre = resultado["nombre"]?.jsonPrimitive?.content ?: ""
                etNombre.setText(nombre)
                etCorreo.setText(resultado["email"]?.jsonPrimitive?.content ?: "")
                etTelefono.setText(resultado["telefono"]?.jsonPrimitive?.content ?: "")
                etCiudad.setText(resultado["ciudad"]?.jsonPrimitive?.content ?: "")

                fotoUrlActual = resultado["foto_url"]?.jsonPrimitive?.content
                if (!fotoUrlActual.isNullOrEmpty()) {
                    mostrarFoto(fotoUrlActual)
                } else {
                    val iniciales = nombre.split(" ")
                        .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .joinToString("")
                    tvIniciales.text = iniciales
                }

            } catch (e: Exception) {
                Log.e("FOTO", "Error cargarDatos: ${e.message}", e)
                Toast.makeText(this@EditarPerfilActivity,
                    "Error al cargar datos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Guardar cambios ───────────────────────────────────────
    private fun guardarCambios() {
        val nombre   = etNombre.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val ciudad   = etCiudad.text.toString().trim()

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

                Toast.makeText(this@EditarPerfilActivity,
                    "Perfil actualizado ✓", Toast.LENGTH_SHORT).show()
                finish()

            } catch (e: Exception) {
                Log.e("FOTO", "Error guardarCambios: ${e.message}", e)
                Toast.makeText(this@EditarPerfilActivity,
                    "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}