package com.ucompensar.kstoreapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ucompensar.kstoreapp.UI.ADMIN.AdminActivity
import com.ucompensar.kstoreapp.UI.AUTH.LoginActivity
import com.ucompensar.kstoreapp.UI.AUTH.TipoCuentaActivity
import com.ucompensar.kstoreapp.UI.CLIENTE.ClienteActivity
import com.ucompensar.kstoreapp.UI.PROFESIONAL.ProfesionalActivity
import com.ucompensar.kstoreapp.process.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.statusBarColor = android.graphics.Color.parseColor("#6200EE")

        val btnIniciarSesion = findViewById<Button>(R.id.btnIniciarSesion)
        val btnRegistrarse   = findViewById<Button>(R.id.btnRegistrarse)

        // ── Verificar si hay sesión activa al abrir la app ────
        lifecycleScope.launch {
            val sesionActiva = SupabaseClient.client.auth.currentSessionOrNull()
            if (sesionActiva != null) {
                // Hay sesión — obtener rol y navegar directo
                val uid = sesionActiva.user?.id ?: return@launch
                val rol = obtenerRol(uid)
                irAPantallaSegunRol(rol)
                return@launch
            }
            // No hay sesión — mostrar botones normalmente
        }

        btnIniciarSesion.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        btnRegistrarse.setOnClickListener {
            startActivity(Intent(this, TipoCuentaActivity::class.java))
        }
    }

    // ── Obtener rol desde tabla profiles ─────────────────────
    private suspend fun obtenerRol(uid: String): String {
        return try {
            val resultado = SupabaseClient.client.postgrest
                .from("profiles")
                .select { filter { eq("id", uid) } }
                .decodeSingle<Map<String, kotlinx.serialization.json.JsonElement>>()
            resultado["rol"]?.jsonPrimitive?.content ?: "cliente"
        } catch (e: Exception) {
            "cliente"
        }
    }

    // ── Navegar según rol ─────────────────────────────────────
    private fun irAPantallaSegunRol(rol: String) {
        val intent = when (rol) {
            "admin"       -> Intent(this, AdminActivity::class.java)
            "profesional" -> Intent(this, ProfesionalActivity::class.java)
            else          -> Intent(this, ClienteActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}