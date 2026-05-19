package com.ucompensar.kstoreapp.UI.AUTH

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.PROFESIONAL.ProfesionalActivity
import com.ucompensar.kstoreapp.process.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RegistroProfesionalActivity : AppCompatActivity() {

    private val categoriasSeleccionadas = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_profesional)

        window.statusBarColor = android.graphics.Color.parseColor("#6200EE")

        val btnVolver     = findViewById<ImageButton>(R.id.btnVolver)
        val etNombre      = findViewById<EditText>(R.id.etNombre)
        val etEmail       = findViewById<EditText>(R.id.etEmail)
        val etTelefono    = findViewById<EditText>(R.id.etTelefono)
        val etCiudad      = findViewById<EditText>(R.id.etCiudad)
        val etPassword    = findViewById<EditText>(R.id.etPassword)
        val etDescripcion = findViewById<EditText>(R.id.etDescripcion)
        val btnCrear      = findViewById<Button>(R.id.btnCrearCuenta)

        // Categorías (igual que antes)
        val categorias = mapOf(
            R.id.catElectricista to "Electricista",
            R.id.catPlomero      to "Plomero",
            R.id.catCarpintero   to "Carpintero",
            R.id.catPintor       to "Pintor",
            R.id.catDev          to "Dev",
            R.id.catLimpieza     to "Limpieza",
            R.id.catJardin       to "Jardín",
            R.id.catMecanico     to "Mecánico",
            R.id.catArquitecto   to "Arquitecto"
        )

        categorias.forEach { (id, nombre) ->
            val layout = findViewById<LinearLayout>(id)
            layout.setOnClickListener {
                if (categoriasSeleccionadas.contains(nombre)) {
                    categoriasSeleccionadas.remove(nombre)
                    layout.setBackgroundResource(R.drawable.card_tipo)
                } else {
                    categoriasSeleccionadas.add(nombre)
                    layout.setBackgroundColor(
                        android.graphics.Color.parseColor("#EDE7F6"))
                }
            }
        }

        btnVolver.setOnClickListener { finish() }

        btnCrear.setOnClickListener {
            val nombre      = etNombre.text.toString().trim()
            val email       = etEmail.text.toString().trim()
            val telefono    = etTelefono.text.toString().trim()
            val ciudad      = etCiudad.text.toString().trim()
            val password    = etPassword.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()

            if (nombre.isEmpty())    { etNombre.error      = "Ingresa tu nombre";      return@setOnClickListener }
            if (email.isEmpty())     { etEmail.error       = "Ingresa tu correo";      return@setOnClickListener }
            if (telefono.isEmpty())  { etTelefono.error    = "Ingresa tu teléfono";    return@setOnClickListener }
            if (ciudad.isEmpty())    { etCiudad.error      = "Ingresa tu ciudad";      return@setOnClickListener }
            if (password.length < 8) { etPassword.error   = "Mínimo 8 caracteres";   return@setOnClickListener }
            if (categoriasSeleccionadas.isEmpty()) {
                Toast.makeText(this, "Selecciona al menos una categoría", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (descripcion.isEmpty()) { etDescripcion.error = "Ingresa tu descripción"; return@setOnClickListener }

            btnCrear.isEnabled = false
            btnCrear.text = "Creando cuenta..."

            lifecycleScope.launch {
                try {
                    // 1. Crear usuario en Supabase Auth
                    SupabaseClient.client.auth.signUpWith(Email) {
                        this.email    = email
                        this.password = password
                        data = buildJsonObject {
                            put("nombre", nombre)
                            put("rol", "profesional")
                        }
                    }

                    // 2. Esperar que el trigger cree el perfil
                    delay(500)

                    // 3. Obtener uid
                    val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id

                    if (uid != null) {
                        // 4. Actualizar profiles con todos los datos
                        SupabaseClient.client.postgrest.from("profiles").upsert(
                            mapOf(
                                "id"       to uid,
                                "email"    to email,
                                "nombre"   to nombre,
                                "telefono" to telefono,
                                "ciudad"   to ciudad,
                                "rol"      to "profesional"
                            )
                        )

                        // 5. Insertar en tabla profesionales
                        SupabaseClient.client.postgrest.from("profesionales").upsert(
                            mapOf(
                                "id"          to uid,
                                "descripcion" to descripcion,
                                "categorias"  to categoriasSeleccionadas
                            )
                        )
                    }

                    Toast.makeText(
                        this@RegistroProfesionalActivity,
                        "¡Cuenta creada exitosamente! 🎉",
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(this@RegistroProfesionalActivity, ProfesionalActivity::class.java))
                    finishAffinity()

                } catch (e: Exception) {
                    btnCrear.isEnabled = true
                    btnCrear.text = "Crear cuenta"
                    Toast.makeText(
                        this@RegistroProfesionalActivity,
                        e.message ?: "Error desconocido",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}