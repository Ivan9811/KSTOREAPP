package com.ucompensar.kstoreapp.UI.ADMIN

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.kstoreapp.R

class ServicioAdapter(
    private var lista: MutableList<Servicio>,
    private val onVer: (Servicio) -> Unit,
    private val onAccion1: (Servicio) -> Unit,
    private val onAccion2: (Servicio) -> Unit
) : RecyclerView.Adapter<ServicioAdapter.ServicioViewHolder>() {

    inner class ServicioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvIcono       = itemView.findViewById<TextView>(R.id.tvIconoServicio)
        val tvNombre      = itemView.findViewById<TextView>(R.id.tvNombreServicio)
        val tvPrestador   = itemView.findViewById<TextView>(R.id.tvPrestadorServicio)
        val tvPrecio      = itemView.findViewById<TextView>(R.id.tvPrecio)
        val tvEstado      = itemView.findViewById<TextView>(R.id.tvEstadoServicio)
        val tvReporte     = itemView.findViewById<TextView>(R.id.tvReporte)
        val tvCalificacion= itemView.findViewById<TextView>(R.id.tvCalificacion)
        val tvResenas     = itemView.findViewById<TextView>(R.id.tvResenas)
        val tvReservas    = itemView.findViewById<TextView>(R.id.tvReservas)
        val btnVer        = itemView.findViewById<Button>(R.id.btnVerServicio)
        val btnAccion1    = itemView.findViewById<Button>(R.id.btnAccion1)
        val btnAccion2    = itemView.findViewById<Button>(R.id.btnAccion2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServicioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_servicio, parent, false)
        return ServicioViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServicioViewHolder, position: Int) {
        val servicio = lista[position]

        holder.tvNombre.text    = servicio.nombre
        holder.tvPrestador.text = "👤 ${servicio.prestador}"
        holder.tvPrecio.text    = servicio.precio
        holder.tvCalificacion.text = "⭐ ${servicio.calificacion}"
        holder.tvResenas.text   = "💬 ${servicio.resenas} reseñas"
        holder.tvReservas.text  = "📅 ${servicio.reservas}"

        // Estado
        when (servicio.estado) {
            "Activo" -> {
                holder.tvEstado.text = "Activo"
                holder.tvEstado.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
                holder.tvEstado.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                holder.tvIcono.setBackgroundColor(android.graphics.Color.parseColor("#FFF3E0"))
                holder.tvIcono.text = "⚡"
                holder.tvReporte.visibility = View.GONE
                holder.btnAccion1.text = "Pausar"
                holder.btnAccion1.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#FFF9C4"))
                holder.btnAccion1.setTextColor(android.graphics.Color.parseColor("#F57F17"))
                holder.btnAccion2.text = "Eliminar"
                holder.btnAccion2.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#FFE5E5"))
                holder.btnAccion2.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
            }
            "Revision" -> {
                holder.tvEstado.text = "Revisión"
                holder.tvEstado.setBackgroundColor(android.graphics.Color.parseColor("#FFF3CD"))
                holder.tvEstado.setTextColor(android.graphics.Color.parseColor("#856404"))
                holder.tvIcono.setBackgroundColor(android.graphics.Color.parseColor("#FFF9C4"))
                holder.tvIcono.text = "🧹"
                holder.tvReporte.visibility = View.GONE
                holder.btnAccion1.text = "Aprobar"
                holder.btnAccion1.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#E8F5E9"))
                holder.btnAccion1.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                holder.btnAccion2.text = "Rechazar"
                holder.btnAccion2.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#FFE5E5"))
                holder.btnAccion2.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
            }
            "Reportado" -> {
                holder.tvEstado.text = "Reportado"
                holder.tvEstado.setBackgroundColor(android.graphics.Color.parseColor("#FFE5E5"))
                holder.tvEstado.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
                holder.tvIcono.setBackgroundColor(android.graphics.Color.parseColor("#E3F2FD"))
                holder.tvIcono.text = "💻"
                holder.tvReporte.visibility = View.VISIBLE
                holder.tvReporte.text = "⚠️ Reporte: ${servicio.reporte}"
                holder.btnAccion1.text = "Absolver"
                holder.btnAccion1.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#E8F5E9"))
                holder.btnAccion1.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                holder.btnAccion2.text = "Eliminar"
                holder.btnAccion2.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#FFE5E5"))
                holder.btnAccion2.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
            }
        }

        holder.btnVer.setOnClickListener     { onVer(servicio) }
        holder.btnAccion1.setOnClickListener { onAccion1(servicio) }
        holder.btnAccion2.setOnClickListener { onAccion2(servicio) }
    }

    override fun getItemCount() = lista.size

    fun actualizarLista(nuevaLista: MutableList<Servicio>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}