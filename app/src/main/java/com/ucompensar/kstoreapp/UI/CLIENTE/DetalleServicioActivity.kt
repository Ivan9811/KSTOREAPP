package com.ucompensar.kstoreapp.UI.CLIENTE

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.ucompensar.kstoreapp.R

class DetalleServicioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_servicio)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars())

        window.statusBarColor = "#6B2FCC".toColorInt()
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Volver atrás
        findViewById<ImageView>(R.id.btnVolver).setOnClickListener {
            finish()
        }

        // Ir a Reservar

    }
}