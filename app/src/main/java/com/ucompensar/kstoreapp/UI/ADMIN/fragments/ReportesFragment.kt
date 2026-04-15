package com.ucompensar.kstoreapp.UI.ADMIN.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.ucompensar.kstoreapp.R
import androidx.core.graphics.toColorInt

class ReportesFragment : Fragment(R.layout.fragment_reportes) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btn7dias    = view.findViewById<Button>(R.id.btn7dias)
        val btnEsteMes  = view.findViewById<Button>(R.id.btnEsteMes)
        val btnTrimestre= view.findViewById<Button>(R.id.btnTrimestre)
        val btnAnio     = view.findViewById<Button>(R.id.btnAnio)
        val btnExportar = view.findViewById<Button>(R.id.btnExportarPDF)

        // Filtros de tiempo
        val botonesFiltro = listOf(btn7dias, btnEsteMes, btnTrimestre, btnAnio)

        btn7dias.setOnClickListener     { seleccionarFiltro(btn7dias, botonesFiltro) }
        btnEsteMes.setOnClickListener   { seleccionarFiltro(btnEsteMes, botonesFiltro) }
        btnTrimestre.setOnClickListener { seleccionarFiltro(btnTrimestre, botonesFiltro) }
        btnAnio.setOnClickListener      { seleccionarFiltro(btnAnio, botonesFiltro) }

        btnExportar.setOnClickListener {
            Toast.makeText(requireContext(), "Exportando PDF...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun seleccionarFiltro(botonSeleccionado: Button, todos: List<Button>) {
        todos.forEach { btn ->
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.WHITE)
            btn.setTextColor("#888888".toColorInt())
        }
        botonSeleccionado.backgroundTintList = android.content.res.ColorStateList.valueOf(
            "#6200EE".toColorInt())
        botonSeleccionado.setTextColor(android.graphics.Color.WHITE)
    }
}
