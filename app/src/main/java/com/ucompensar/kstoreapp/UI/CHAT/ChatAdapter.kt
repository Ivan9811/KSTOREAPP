package com.ucompensar.kstoreapp.UI.CHAT

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.kstoreapp.R

class ChatAdapter(
    private val mensajes: MutableList<ChatMensajeItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TIPO_ENVIADO   = 1
        private const val TIPO_RECIBIDO  = 0
    }

    inner class ViewHolderEnviado(view: View) : RecyclerView.ViewHolder(view) {
        val tvContenido: TextView = view.findViewById(R.id.tvContenido)
        val tvHora     : TextView = view.findViewById(R.id.tvHora)
    }

    inner class ViewHolderRecibido(view: View) : RecyclerView.ViewHolder(view) {
        val tvContenido: TextView = view.findViewById(R.id.tvContenido)
        val tvHora     : TextView = view.findViewById(R.id.tvHora)
    }

    override fun getItemViewType(position: Int) =
        if (mensajes[position].esPropio) TIPO_ENVIADO else TIPO_RECIBIDO

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TIPO_ENVIADO) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_mensaje_enviado, parent, false)
            ViewHolderEnviado(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_mensaje_recibido, parent, false)
            ViewHolderRecibido(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mensaje = mensajes[position]
        when (holder) {
            is ViewHolderEnviado  -> {
                holder.tvContenido.text = mensaje.contenido
                holder.tvHora.text      = mensaje.hora
            }
            is ViewHolderRecibido -> {
                holder.tvContenido.text = mensaje.contenido
                holder.tvHora.text      = mensaje.hora
            }
        }
    }

    override fun getItemCount() = mensajes.size

    fun agregarMensaje(mensaje: ChatMensajeItem) {
        mensajes.add(mensaje)
        notifyItemInserted(mensajes.size - 1)
    }

    fun actualizar(nuevos: List<ChatMensajeItem>) {
        mensajes.clear()
        mensajes.addAll(nuevos)
        notifyDataSetChanged()
    }
}