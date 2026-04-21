package com.ucompensar.kstoreapp.UI.CLIENTE

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.CLIENTE.fragments.MensajeItem

class MensajesAdapter(
    private val mensajes: List<MensajeItem>,
    private val onClick: (MensajeItem) -> Unit
) : RecyclerView.Adapter<MensajesAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIniciales: TextView     = view.findViewById(R.id.tvIniciales)
        val tvNombre: TextView        = view.findViewById(R.id.tvNombre)
        val tvUltimo: TextView        = view.findViewById(R.id.ultimoMensaje)
        val tvHora: TextView          = view.findViewById(R.id.hora)
        val dotOnline: View           = view.findViewById(R.id.dotonline)
        val tvBadge: TextView         = view.findViewById(R.id.tvBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mensaje, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mensajes[position]

        holder.tvIniciales.text = item.iniciales
        holder.tvNombre.text    = item.nombre
        holder.tvUltimo.text    = item.ultimoMensaje
        holder.tvHora.text      = item.hora

        // Punto online
        holder.dotOnline.visibility = if (item.online) View.VISIBLE else View.GONE

        // Badge no leídos
        if (item.noLeidos > 0) {
            holder.tvBadge.visibility = View.VISIBLE
            holder.tvBadge.text = item.noLeidos.toString()
        } else {
            holder.tvBadge.visibility = View.GONE
        }

        // Divider excepto último item
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = mensajes.size
}