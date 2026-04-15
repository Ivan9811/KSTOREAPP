package com.ucompensar.kstoreapp.UI.ADMIN.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.ADMIN.Servicio
import com.ucompensar.kstoreapp.UI.ADMIN.ServicioAdapter

class ServiciosFragment : Fragment(R.layout.fragment_servicios) {

    private lateinit var adapter: ServicioAdapter
    private val listaCompleta = mutableListOf(
        Servicio(
            "Instalación Eléctrica",
            "Juan Rodríguez",
            "$50/h",
            "Activo",
            4.9f,
            127,
            "342 reservas"
        ),
        Servicio("Limpieza Profunda del Hogar","Rosa Mendoza", "$60",   "Revision",  4.8f, 201, "Nuevo"),
        Servicio("Desarrollo Web & Apps",    "Ana Silva",      "$80/h", "Reportado", 0f,   0,   "", "Descripción no corresponde al servicio ofrecido"),
        Servicio("Pintura Exterior e Interior","Pedro Vargas",  "$35/h", "Activo",   4.7f, 62,  "98 reservas")
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler            = view.findViewById<RecyclerView>(R.id.recyclerServicios)
        val etBuscar            = view.findViewById<EditText>(R.id.etBuscarServicio)
        val btnTodos            = view.findViewById<Button>(R.id.btnTodos)
        val btnActivos          = view.findViewById<Button>(R.id.btnActivos)
        val btnRevision         = view.findViewById<Button>(R.id.btnRevision)
        val btnAprobarPendientes= view.findViewById<Button>(R.id.btnAprobarPendientes)
        val btnVerReportados    = view.findViewById<Button>(R.id.btnVerReportados)

        adapter = ServicioAdapter(
            listaCompleta,
            onVer = { servicio ->
                Toast.makeText(requireContext(), "Ver: ${servicio.nombre}", Toast.LENGTH_SHORT).show()
            },
            onAccion1 = { servicio ->
                Toast.makeText(requireContext(), "Acción 1: ${servicio.nombre}", Toast.LENGTH_SHORT).show()
            },
            onAccion2 = { servicio ->
                Toast.makeText(requireContext(), "Acción 2: ${servicio.nombre}", Toast.LENGTH_SHORT).show()
            }
        )

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        btnTodos.setOnClickListener    { adapter.actualizarLista(listaCompleta) }
        btnActivos.setOnClickListener  { filtrar("Activo") }
        btnRevision.setOnClickListener { filtrar("Revision") }
        btnAprobarPendientes.setOnClickListener { filtrar("Revision") }
        btnVerReportados.setOnClickListener     { filtrar("Reportado") }

        etBuscar.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString().lowercase()
                val filtrada = listaCompleta.filter {
                    it.nombre.lowercase().contains(query) ||
                            it.prestador.lowercase().contains(query)
                }.toMutableList()
                adapter.actualizarLista(filtrada)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun filtrar(estado: String) {
        val filtrada = listaCompleta.filter { it.estado == estado }.toMutableList()
        adapter.actualizarLista(filtrada)
    }
}