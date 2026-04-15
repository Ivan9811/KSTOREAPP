package com.ucompensar.kstoreapp.UI.ADMIN.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.ucompensar.kstoreapp.UI.ADMIN.Usuario
import com.ucompensar.kstoreapp.UI.ADMIN.UsuarioAdapter

class UsuariosFragment : Fragment(R.layout.fragment_usuarios) {

    private lateinit var adapter: UsuarioAdapter
    private val listaCompleta = mutableListOf(
        Usuario("María García", "maria@email.com", "Cliente", "Activo", "Ene 2025", 12, 4.8f, "MG"),
        Usuario("Juan Rodríguez","juan@email.com",   "Prestador",  "Pendiente","Feb 2025",  8, 4.9f, "JR"),
        Usuario("Ana Silva",     "ana@email.com",    "Prestador",  "Activo",   "Dic 2024", 24, 5.0f, "AS")
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerUsuarios = view.findViewById<RecyclerView>(R.id.recyclerUsuarios)
        val etBuscar         = view.findViewById<EditText>(R.id.etBuscar)
        val btnTodos         = view.findViewById<Button>(R.id.btnTodos)
        val btnClientes      = view.findViewById<Button>(R.id.btnClientes)
        val btnPrestadores   = view.findViewById<Button>(R.id.btnPrestadores)
        val btnPendientes    = view.findViewById<Button>(R.id.btnPendientes)

        // Configurar adapter
        adapter = UsuarioAdapter(
            listaCompleta,
            onVerPerfil = { usuario ->
                Toast.makeText(requireContext(), "Ver perfil: ${usuario.nombre}", Toast.LENGTH_SHORT).show()
            },
            onAccion = { usuario ->
                Toast.makeText(requireContext(), "Acción: ${usuario.nombre}", Toast.LENGTH_SHORT).show()
            }
        )

        recyclerUsuarios.layoutManager = LinearLayoutManager(requireContext())
        recyclerUsuarios.adapter = adapter

        // Filtros
        btnTodos.setOnClickListener       { adapter.actualizarLista(listaCompleta) }
        btnClientes.setOnClickListener    { filtrar("Cliente") }
        btnPrestadores.setOnClickListener { filtrar("Prestador") }
        btnPendientes.setOnClickListener  { filtrar(estado = "Pendiente") }

        // Búsqueda
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                val filtrada = listaCompleta.filter {
                    it.nombre.lowercase().contains(query) ||
                            it.email.lowercase().contains(query)
                }.toMutableList()
                adapter.actualizarLista(filtrada)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun filtrar(tipo: String? = null, estado: String? = null) {
        val filtrada = listaCompleta.filter {
            (tipo == null   || it.tipo   == tipo) &&
                    (estado == null || it.estado == estado)
        }.toMutableList()
        adapter.actualizarLista(filtrada)
    }
}