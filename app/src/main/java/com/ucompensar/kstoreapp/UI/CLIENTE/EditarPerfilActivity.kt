package com.ucompensar.kstoreapp.UI.CLIENTE

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import com.ucompensar.kstoreapp.R

class EditarPerfilActivity : AppCompatActivity() {

    private val diasSeleccionados = mutableSetOf("Lun", "Mar", "Mié", "Jue", "Vie")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_perfil)

        window.statusBarColor = "#FFFFFF".toColorInt()
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Volver
        findViewById<ImageView>(R.id.btnVolver).setOnClickListener {
            finish()
        }

        // Días disponibilidad toggle
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
            findViewById<TextView>(id).setOnClickListener { tv ->
                tv as TextView
                if (diasSeleccionados.contains(nombre)) {
                    diasSeleccionados.remove(nombre)
                    tv.background = getDrawable(R.drawable.bg_chip_unselected)
                    tv.setTextColor("#4A0E8F".toColorInt())
                } else {
                    diasSeleccionados.add(nombre)
                    tv.background = getDrawable(R.drawable.bg_chip_selected)
                    tv.setTextColor("#FFFFFF".toColorInt())
                }
            }
        }

        // Guardar
        findViewById<Button>(R.id.btnGuardar).setOnClickListener {
            Toast.makeText(this, "Perfil actualizado ✓", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}