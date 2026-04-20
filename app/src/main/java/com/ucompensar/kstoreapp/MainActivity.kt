package com.ucompensar.kstoreapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.ucompensar.kstoreapp.UI.AUTH.LoginActivity
import com.ucompensar.kstoreapp.UI.AUTH.TipoCuentaActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.statusBarColor = android.graphics.Color.parseColor("#6200EE")

        val btnIniciarSesion = findViewById<Button>(R.id.btnIniciarSesion)
        val btnRegistrarse   = findViewById<Button>(R.id.btnRegistrarse)

        btnIniciarSesion.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        btnRegistrarse.setOnClickListener {
            startActivity(Intent(this, TipoCuentaActivity::class.java))
        }
    }
}