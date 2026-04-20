package com.ucompensar.kstoreapp.UI.AUTH

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.ADMIN.AdminActivity


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

        btnIniciarSesion.setOnClickListener {
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = "Ingresa tu correo"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Ingresa tu contraseña"
                return@setOnClickListener
            }
            startActivity(Intent(this, AdminActivity::class.java))
            finish()
        }

        btnGoogle.setOnClickListener {
            Toast.makeText(this, "Google Sign In próximamente", Toast.LENGTH_SHORT).show()
        }

        btnBiometria.setOnClickListener {
            mostrarBiometria()
        }

        btnVolver.setOnClickListener {
            finish()
        }

        tvOlvidaste.setOnClickListener {
            Toast.makeText(this, "Recuperar contraseña próximamente", Toast.LENGTH_SHORT).show()
        }

        tvRegistrate.setOnClickListener {
            startActivity(Intent(this, TipoCuentaActivity::class.java))
        }
    }

    private fun mostrarBiometria() {
        val biometricManager = BiometricManager.from(this)

        when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {

            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = ContextCompat.getMainExecutor(this)
                val biometricPrompt = BiometricPrompt(
                    this, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(
                            result: BiometricPrompt.AuthenticationResult
                        ) {
                            super.onAuthenticationSucceeded(result)
                            startActivity(Intent(this@LoginActivity, AdminActivity::class.java))
                            finish()
                        }

                        override fun onAuthenticationError(
                            errorCode: Int, errString: CharSequence
                        ) {
                            super.onAuthenticationError(errorCode, errString)
                            Toast.makeText(
                                this@LoginActivity,
                                "Error: $errString", Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            Toast.makeText(
                                this@LoginActivity,
                                "Autenticación fallida", Toast.LENGTH_SHORT
                            ).show()
                        }
                    })

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Autenticación biométrica")
                    .setSubtitle("Usa tu huella o Face ID para continuar")
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()

                biometricPrompt.authenticate(promptInfo)
            }
            else -> {
                Toast.makeText(this,
                    "Biometría no disponible en este dispositivo",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}