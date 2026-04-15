package com.ucompensar.kstoreapp.UI.ADMIN

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.kstoreapp.R

class UsuarioAdapter(
    private var lista: MutableList<Usuario>,
    private val onVerPerfil: (Usuario) -> Unit,
    private val onAccion: (Usuario) -> Unit
) : RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder>() {

    inner class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvIniciales    = itemView.findViewById<TextView>(R.id.tvIniciales)
        val tvNombre       = itemView.findViewById<TextView>(R.id.tvNombre)
        val tvEmail        = itemView.findViewById<TextView>(R.id.tvEmail)
        val tvTipo         = itemView.findViewById<TextView>(R.id.tvTipo)
        val tvEstado       = itemView.findViewById<TextView>(R.id.tvEstado)
        val tvTiempo       = itemView.findViewById<TextView>(R.id.tvTiempo)
        val tvRegistro     = itemView.findViewById<TextView>(R.id.tvRegistro)
        val tvPedidos      = itemView.findViewById<TextView>(R.id.tvPedidos)
        val tvCalificacion = itemView.findViewById<TextView>(R.id.tvCalificacion)
        val btnVerPerfil   = itemView.findViewById<Button>(R.id.btnVerPerfil)
        val btnAccion      = itemView.findViewById<Button>(R.id.btnAccion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = lista[position]

        holder.tvIniciales.text    = usuario.iniciales
        holder.tvNombre.text       = usuario.nombre
        holder.tvEmail.text        = usuario.email
        holder.tvTipo.text         = usuario.tipo
        holder.tvRegistro.text     = "📅 ${usuario.fechaRegistro}"
        holder.tvCalificacion.text = "⭐ ${usuario.calificacion}"

        // Pedidos o servicios según tipo
        if (usuario.tipo == "Prestador") {
            holder.tvPedidos.text  = "🔧 ${usuario.pedidos} servicios"
            holder.btnAccion.text  = "Verificar"
            holder.btnAccion.backgroundTintList =
                ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9"))
            holder.btnAccion.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
        } else {
            holder.tvPedidos.text  = "🛒 ${usuario.pedidos} pedidos"
            holder.btnAccion.text  = "Suspender"
            holder.btnAccion.backgroundTintList =
                ColorStateList.valueOf(android.graphics.Color.parseColor("#FFE5E5"))
            holder.btnAccion.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
        }

        // Estado
        if (usuario.estado == "Activo") {
            holder.tvEstado.text      = "● Activo"
            holder.tvEstado.setTextColor(android.graphics.Color.parseColor("#00C853"))
        } else {
            holder.tvEstado.text      = "● Pendiente"
            holder.tvEstado.setTextColor(android.graphics.Color.parseColor("#FF6B00"))
        }

        // Botones
        holder.btnVerPerfil.setOnClickListener { onVerPerfil(usuario) }
        holder.btnAccion.setOnClickListener    { onAccion(usuario) }
    }

    override fun getItemCount() = lista.size

    // Actualizar lista (para filtros y búsqueda)
    fun actualizarLista(nuevaLista: MutableList<Usuario>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}