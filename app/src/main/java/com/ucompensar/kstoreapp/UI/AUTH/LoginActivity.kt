package com.ucompensar.kstoreapp.UI.AUTH

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.ADMIN.AdminActivity
import com.ucompensar.kstoreapp.UI.CLIENTE.ClienteActivity
import com.ucompensar.kstoreapp.UI.PROFESIONAL.ProfesionalActivity
import com.ucompensar.kstoreapp.process.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import com.ucompensar.kstoreapp.BuildConfig

class LoginActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        window.statusBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val etEmail          = findViewById<EditText>(R.id.etEmail)
        val etPassword       = findViewById<EditText>(R.id.etPassword)
        val btnIniciarSesion = findViewById<Button>(R.id.btnIniciarSesion)
        val btnGoogle        = findViewById<Button>(R.id.btnGoogle)
        val btnBiometria     = findViewById<Button>(R.id.btnBiometria)
        val btnVolver        = findViewById<Button>(R.id.btnVolver)
        val tvOlvidaste      = findViewById<TextView>(R.id.tvOlvidaste)
        val tvRegistrate     = findViewById<TextView>(R.id.tvRegistrate)

        configurarVisibilidadHuella(btnBiometria)

        // ── Login con email y contraseña ──────────────────────
        btnIniciarSesion.setOnClickListener {
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty())    { etEmail.error    = "Ingresa tu correo";     return@setOnClickListener }
            if (password.isEmpty()) { etPassword.error = "Ingresa tu contraseña"; return@setOnClickListener }

            btnIniciarSesion.isEnabled = false
            btnIniciarSesion.text = "Entrando..."

            lifecycleScope.launch {
                try {
                    SupabaseClient.client.auth.signInWith(Email) {
                        this.email    = email
                        this.password = password
                    }

                    val session = SupabaseClient.client.auth.currentSessionOrNull()
                    Log.d("LOGIN", "UID tras login: ${session?.user?.id}")

                    if (session != null) {
                        guardarSesion(session.accessToken, session.refreshToken)
                        guardarCredenciales(email, password)
                    }

                    val uid = session?.user?.id ?: ""
                    val rol = obtenerRol(uid)
                    irAPantallaSegunRol(rol)

                } catch (e: Exception) {
                    btnIniciarSesion.isEnabled = true
                    btnIniciarSesion.text = "Iniciar sesión"
                    val mensaje = when {
                        e.message?.contains("Invalid login") == true ->
                            "Correo o contraseña incorrectos"
                        e.message?.contains("Email not confirmed") == true ->
                            "Debes confirmar tu correo primero"
                        else -> "Error: ${e.message}"
                    }
                    Toast.makeText(this@LoginActivity, mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }

        // ── Login con Google ──────────────────────────────────
        btnGoogle.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                        .setAutoSelectEnabled(false)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val credentialManager = CredentialManager.create(this@LoginActivity)
                    val result = credentialManager.getCredential(this@LoginActivity, request)
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(result.credential.data)

                    SupabaseClient.client.auth.signInWith(IDToken) {
                        idToken  = googleIdTokenCredential.idToken
                        provider = Google
                    }

                    val session = SupabaseClient.client.auth.currentSessionOrNull()
                    if (session != null) {
                        guardarSesion(session.accessToken, session.refreshToken)
                    }

                    val uid = session?.user?.id ?: ""
                    val rol = obtenerRol(uid)
                    irAPantallaSegunRol(rol)

                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity,
                        "Error con Google: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // ── Login con huella ──────────────────────────────────
        btnBiometria.setOnClickListener {
            mostrarBiometria()
        }

        btnVolver.setOnClickListener { finish() }

        tvOlvidaste.setOnClickListener {
            Toast.makeText(this, "Recuperar contraseña próximamente", Toast.LENGTH_SHORT).show()
        }

        tvRegistrate.setOnClickListener {
            startActivity(Intent(this, TipoCuentaActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<Button>(R.id.btnBiometria)?.let { configurarVisibilidadHuella(it) }
    }

    // ── Huella visible solo si hay credenciales guardadas ─────
    private fun configurarVisibilidadHuella(btn: Button) {
        val prefs = getSharedPreferences("kstore_prefs", Context.MODE_PRIVATE)
        val huellaActiva = prefs.getBoolean("bio_habilitada", false)
        val biometricManager = BiometricManager.from(this)
        val disponible = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
        btn.visibility = if (huellaActiva && disponible) View.VISIBLE else View.GONE
    }

    // ── Diálogo de huella ─────────────────────────────────────
    private fun mostrarBiometria() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {

            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = ContextCompat.getMainExecutor(this)
                val biometricPrompt = BiometricPrompt(this, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(
                            result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            val prefs = getSharedPreferences("kstore_prefs", Context.MODE_PRIVATE)
                            val email    = prefs.getString("bio_email", null)
                            val password = prefs.getString("bio_password", null)

                            if (email != null && password != null) {
                                lifecycleScope.launch {
                                    try {
                                        SupabaseClient.client.auth.signInWith(Email) {
                                            this.email    = email
                                            this.password = password
                                        }
                                        val session = SupabaseClient.client.auth.currentSessionOrNull()
                                        if (session != null) {
                                            guardarSesion(session.accessToken, session.refreshToken)
                                        }
                                        val uid = session?.user?.id ?: ""
                                        val rol = obtenerRol(uid)
                                        irAPantallaSegunRol(rol)
                                    } catch (e: Exception) {
                                        runOnUiThread {
                                            Toast.makeText(this@LoginActivity,
                                                "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(this@LoginActivity,
                                    "Sesión expirada. Inicia sesión con tu correo.",
                                    Toast.LENGTH_LONG).show()
                                limpiarCredenciales()
                            }
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                                errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                Toast.makeText(this@LoginActivity,
                                    "Error biométrico: $errString", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onAuthenticationFailed() {
                            Toast.makeText(this@LoginActivity,
                                "Huella no reconocida", Toast.LENGTH_SHORT).show()
                        }
                    })

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Acceso con huella")
                    .setSubtitle("Usa tu huella dactilar para ingresar")
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()

                biometricPrompt.authenticate(promptInfo)
            }
            else -> Toast.makeText(this,
                "Biometría no disponible en este dispositivo", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Obtener rol desde profiles ────────────────────────────
    private suspend fun obtenerRol(uid: String): String {
        if (uid.isEmpty()) return "cliente"
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
        startActivity(intent)
        finishAffinity()
    }

    // ── Guardar sesión y credenciales ─────────────────────────
    private fun guardarSesion(accessToken: String, refreshToken: String) {
        getSharedPreferences("kstore_prefs", Context.MODE_PRIVATE).edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .apply()
    }

    private fun guardarCredenciales(email: String, password: String) {
        getSharedPreferences("kstore_prefs", Context.MODE_PRIVATE).edit()
            .putString("bio_email", email)
            .putString("bio_password", password)
            .putBoolean("bio_habilitada", true)
            .apply()
    }

    private fun limpiarCredenciales() {
        getSharedPreferences("kstore_prefs", Context.MODE_PRIVATE).edit()
            .remove("bio_email")
            .remove("bio_password")
            .putBoolean("bio_habilitada", false)
            .apply()
    }
}