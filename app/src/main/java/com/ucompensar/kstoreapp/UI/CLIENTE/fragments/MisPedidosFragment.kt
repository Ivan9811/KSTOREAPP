package com.ucompensar.kstoreapp.UI.CLIENTE.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.CLIENTE.PedidoClienteItem
import com.ucompensar.kstoreapp.UI.CLIENTE.PedidosClienteAdapter

class MisPedidosFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mis_pedidos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pedidos = listOf(
            PedidoClienteItem(
                estado      = "Nueva solicitud",
                fecha       = "12 Marzo 2026",
                servicio    = "Instalación Eléctrica Residencial",
                profesional = "Carlos Martínez",
                precio      = "$150",
                fechaDetalle = "12 Marzo 2026",
                hora        = "10:00",
                referencia  = "#SRV-4821",
                cancelable  = true
            ),
            PedidoClienteItem(
                estado      = "Confirmado",
                fecha       = "14 Marzo 2026",
                servicio    = "Mantenimiento Preventivo",
                profesional = "Laura Gómez",
                precio      = "$80",
                fechaDetalle = "14 Marzo 2026",
                hora        = "14:00",
                referencia  = "#SRV-1934",
                cancelable  = false  // 👈 deshabilitado como en el diseño
            )
        )

        val recycler = view.findViewById<RecyclerView>(R.id.rvPedidos)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = PedidosClienteAdapter(
            pedidos,
            onChat = { item ->
                Toast.makeText(requireContext(),
                    "Chat con ${item.profesional}", Toast.LENGTH_SHORT).show()
            },
            onCancelar = { item ->
                Toast.makeText(requireContext(),
                    "Cancelar ${item.servicio}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}