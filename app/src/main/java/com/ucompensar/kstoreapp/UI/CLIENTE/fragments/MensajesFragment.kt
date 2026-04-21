package com.ucompensar.kstoreapp.UI.CLIENTE.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.CLIENTE.MensajesAdapter

class MensajesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mensajes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Datos de prueba
        val mensajes = listOf(
            MensajeItem("CL", "Carlos López",  "¿Estás disponible el viernes?",    "5 min", online = true,  noLeidos = 2),
            MensajeItem("MG", "Miguel Gómez",  "El trabajo quedó perfecto, gracias!", "1h",  online = false, noLeidos = 0),
            MensajeItem("AS", "Ana Silva",     "Te enviamos la cotización",          "3h",   online = true,  noLeidos = 1),
            MensajeItem("PV", "Pedro Vargas",  "Confirmó para el lunes a las 9am",  "Ayer",  online = false, noLeidos = 0)
        )

        val recycler = view.findViewById<RecyclerView>(R.id.rvMensajes)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
        recycler.adapter = MensajesAdapter(mensajes) { item ->
            Toast.makeText(requireContext(), "Chat con ${item.nombre}", Toast.LENGTH_SHORT).show()
        }
    }
}