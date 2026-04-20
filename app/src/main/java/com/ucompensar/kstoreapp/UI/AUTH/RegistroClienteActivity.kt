package com.ucompensar.kstoreapp.UI.AUTH

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.CLIENTE.ClienteActivity

class RegistroClienteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_cliente)

        window.statusBarColor = android.graphics.Color.parseColor("#6200EE")

        val btnVolver    = findViewById<ImageButton>(R.id.btnVolver)
        val etNombre     = findViewById<EditText>(R.id.etNombre)
        val etEmail      = findViewById<EditText>(R.id.etEmail)
        val etTelefono   = findViewById<EditText>(R.id.etTelefono)
        val etCiudad     = findViewById<EditText>(R.id.etCiudad)
        val etPassword   = findViewById<EditText>(R.id.etPassword)
        val btnCrear     = findViewById<Button>(R.id.btnCrearCuenta)

        btnVolver.setOnClickListener { finish() }

        btnCrear.setOnClickListener {
            val nombre   = etNombre.text.toString().trim()
            val email    = etEmail.text.toString().trim()
            val telefono = etTelefono.text.toString().trim()
            val ciudad   = etCiudad.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (nombre.isEmpty()) {
                etNombre.error = "Ingresa tu nombre"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                etEmail.error = "Ingresa tu correo"
                return@setOnClickListener
            }
            if (telefono.isEmpty()) {
                etTelefono.error = "Ingresa tu teléfono"
                return@setOnClickListener
            }
            if (ciudad.isEmpty()) {
                etCiudad.error = "Ingresa tu ciudad"
                return@setOnClickListener
            }
            if (password.length < 8) {
                etPassword.error = "Mínimo 8 caracteres"
                return@setOnClickListener
            }

            // Por ahora navega directo al Cliente
            // cuando implementemos supabase modificar
            Toast.makeText(this, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, ClienteActivity::class.java))
            finishAffinity()
        }
    }
}