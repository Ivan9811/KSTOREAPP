package com.ucompensar.kstoreapp.UI.ADMIN.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.ADMIN.AdminActivity
import com.ucompensar.kstoreapp.UI.ADMIN.UsuarioAdapter
import com.ucompensar.kstoreapp.UI.ADMIN.Usuario

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tarjetas de estadísticas
        val tvUsuariosTotales = view.findViewById<TextView>(R.id.tvUsuariosTotales)
        val tvPrestadores     = view.findViewById<TextView>(R.id.tvPrestadores)
        val tvServicios       = view.findViewById<TextView>(R.id.tvServicios)
        val tvIngresos        = view.findViewById<TextView>(R.id.tvIngresos)
        val tvNombreAdmin     = view.findViewById<TextView>(R.id.tvNombreAdmin)

        // Acciones rápidas
        val btnUsuarios      = view.findViewById<Button>(R.id.btnUsuarios)
        val btnServicios     = view.findViewById<Button>(R.id.btnServicios)
        val btnReportes      = view.findViewById<Button>(R.id.btnReportes)
        val btnConfiguracion = view.findViewById<Button>(R.id.btnConfiguracion)
        val btnVerTodos      = view.findViewById<Button>(R.id.btnVerTodosUsuarios)

        // RecyclerView usuarios recientes
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerUsuariosRecientes)

        // Datos
        tvUsuariosTotales.text = "5,248"
        tvPrestadores.text     = "1,893"
        tvServicios.text       = "342"
        tvIngresos.text        = "$48.2K"
        tvNombreAdmin.text     = "Administrador_prueba"

        // Lista reciente
        val listaReciente = mutableListOf(
            Usuario("María García",   "maria@email.com", "Cliente",   "Activo",    "Ene 2025", 12, 4.8f, "MG"),
            Usuario("Juan Rodríguez", "juan@email.com",  "Prestador", "Pendiente", "Feb 2025",  8, 4.9f, "JR"),
            Usuario("Ana Silva",      "ana@email.com",   "Prestador", "Activo",    "Dic 2024", 24, 5.0f, "AS")
        )

        val adapter = UsuarioAdapter(
            listaReciente,
            onVerPerfil = { usuario ->
                Toast.makeText(requireContext(), "Ver: ${usuario.nombre}", Toast.LENGTH_SHORT).show()
            },
            onAccion = { usuario ->
                Toast.makeText(requireContext(), "Acción: ${usuario.nombre}", Toast.LENGTH_SHORT).show()
            }
        )

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // Botones
        btnUsuarios.setOnClickListener {
            (requireActivity() as AdminActivity).cargarFragment(UsuariosFragment())
        }
        btnServicios.setOnClickListener {
            (requireActivity() as AdminActivity).cargarFragment(ServiciosFragment())
        }
        btnReportes.setOnClickListener {
            (requireActivity() as AdminActivity).cargarFragment(ReportesFragment())
        }
        btnConfiguracion.setOnClickListener { }
        btnVerTodos.setOnClickListener {
            (requireActivity() as AdminActivity).cargarFragment(UsuariosFragment())
        }
    }
}