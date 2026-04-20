package com.ucompensar.kstoreapp.UI.AUTH

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.ucompensar.kstoreapp.R

class TipoCuentaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tipo_cuenta)

        window.statusBarColor = android.graphics.Color.parseColor("#6200EE")

        val btnVolver       = findViewById<ImageButton>(R.id.btnVolver)
        val cardProfesional = findViewById<LinearLayout>(R.id.cardProfesional)
        val cardCliente     = findViewById<LinearLayout>(R.id.cardCliente)

        btnVolver.setOnClickListener { finish() }

        cardProfesional.setOnClickListener {
            startActivity(android.content.Intent(this, RegistroProfesionalActivity::class.java))
        }

        cardCliente.setOnClickListener {
            startActivity(android.content.Intent(this, RegistroClienteActivity::class.java))
        }
    }
}