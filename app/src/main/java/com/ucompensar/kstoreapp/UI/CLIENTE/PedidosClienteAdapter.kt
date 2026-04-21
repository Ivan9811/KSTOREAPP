package com.ucompensar.kstoreapp.UI.CLIENTE

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.kstoreapp.R

class PedidosClienteAdapter(
    private val pedidos: List<PedidoClienteItem>,
    private val onChat: (PedidoClienteItem) -> Unit,
    private val onCancelar: (PedidoClienteItem) -> Unit
) : RecyclerView.Adapter<PedidosClienteAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEstado: TextView       = view.findViewById(R.id.tvEstado)
        val tvFechaBadge: TextView   = view.findViewById(R.id.tvFechaBadge)
        val tvServicio: TextView     = view.findViewById(R.id.tvServicio)
        val tvProfesional: TextView  = view.findViewById(R.id.tvProfesional)
        val tvPrecio: TextView       = view.findViewById(R.id.tvPrecio)
        val tvFecha: TextView        = view.findViewById(R.id.tvFecha)
        val tvHora: TextView         = view.findViewById(R.id.tvHora)
        val tvReferencia: TextView   = view.findViewById(R.id.tvReferencia)
        val layoutBadge: LinearLayout = view.findViewById(R.id.layoutBadge)
        val iconBadge: ImageView     = view.findViewById(R.id.iconBadge)
        val btnChat: Button          = view.findViewById(R.id.btnChat)
        val btnCancelar: Button      = view.findViewById(R.id.btnCancelar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pedido_cliente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = pedidos[position]
        val ctx  = holder.itemView.context

        holder.tvEstado.text      = item.estado
        holder.tvFechaBadge.text  = item.fecha
        holder.tvServicio.text    = item.servicio
        holder.tvProfesional.text = item.profesional
        holder.tvPrecio.text      = item.precio
        holder.tvFecha.text       = item.fechaDetalle
        holder.tvHora.text        = item.hora
        holder.tvReferencia.text  = item.referencia

        // Estilo badge según estado
        when (item.estado) {
            "Nueva solicitud" -> {
                holder.layoutBadge.background =
                    ContextCompat.getDrawable(ctx, R.drawable.bg_chip_white)
                holder.tvEstado.setTextColor(
                    ContextCompat.getColor(ctx, R.color.dorado))
                holder.iconBadge.setColorFilter(
                    ContextCompat.getColor(ctx, R.color.dorado))
                holder.iconBadge.setImageResource(R.drawable.star)
            }
            "Confirmado" -> {
                holder.layoutBadge.background =
                    ContextCompat.getDrawable(ctx, R.drawable.bg_chip_white)
                holder.tvEstado.setTextColor(
                    ContextCompat.getColor(ctx, R.color.verde_oscuro))
                holder.iconBadge.setColorFilter(
                    ContextCompat.getColor(ctx, R.color.verde_oscuro))
                holder.iconBadge.setImageResource(R.drawable.check_circle)
            }
        }

        // Botón cancelar deshabilitado si no es cancelable
        if (!item.cancelable) {
            holder.btnCancelar.isEnabled = false
            holder.btnCancelar.alpha = 0.5f
        }

        holder.btnChat.setOnClickListener    { onChat(item) }
        holder.btnCancelar.setOnClickListener { onCancelar(item) }
    }

    override fun getItemCount() = pedidos.size
}