package com.ucompensar.kstoreapp.UI.PROFESIONAL.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.CLIENTE.MensajesAdapter
import com.ucompensar.kstoreapp.UI.CLIENTE.fragments.MensajeItem
import com.ucompensar.kstoreapp.UI.ChatActivity
import com.ucompensar.kstoreapp.process.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

class MensajesProfesionalFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_mensajes, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cargarConversaciones(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { cargarConversaciones(it) }
    }

    private fun cargarConversaciones(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                    ?: return@launch

                val conversaciones = SupabaseClient.client.postgrest
                    .from("conversaciones")
                    .select { filter { eq("profesional_id", uid) } }
                    .decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()

                val items = conversaciones.mapNotNull { conv ->
                    val otroId = conv["cliente_id"]?.jsonPrimitive?.content

                    otroId ?: return@mapNotNull null

                    val otroProfile = try {
                        SupabaseClient.client.postgrest
                            .from("profiles")
                            .select { filter { eq("id", otroId) } }
                            .decodeSingle<Map<String, kotlinx.serialization.json.JsonElement>>()
                    } catch (e: Exception) { null } ?: return@mapNotNull null

                    val nombre    = otroProfile["nombre"]?.jsonPrimitive?.content ?: "Usuario"
                    val iniciales = nombre.split(" ")
                        .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .joinToString("")

                    MensajeItem(
                        iniciales      = iniciales,
                        nombre         = nombre,
                        ultimoMensaje  = conv["ultimo_mensaje"]?.jsonPrimitive?.content ?: "",
                        hora           = formatearTiempo(conv["ultimo_mensaje_at"]?.jsonPrimitive?.content ?: ""),
                        online         = false,
                        noLeidos       = 0,
                        conversacionId = conv["id"]?.jsonPrimitive?.content ?: "",
                        otroId         = otroId
                    )
                }

                val recycler = view.findViewById<RecyclerView>(R.id.rvMensajes)
                recycler.layoutManager = LinearLayoutManager(requireContext())
                recycler.addItemDecoration(
                    DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
                )
                recycler.adapter = MensajesAdapter(items) { item ->
                    val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                        putExtra("conversacion_id", item.conversacionId)
                        putExtra("otro_nombre",     item.nombre)
                        putExtra("otro_id",         item.otroId)
                    }
                    startActivity(intent)
                }

            } catch (e: Exception) {
                android.util.Log.e("MENSAJES_PROF", "Error: ${e.message}", e)
            }
        }
    }

    private fun formatearTiempo(fecha: String): String {
        if (fecha.isEmpty()) return ""
        return try {
            val instant = java.time.Instant.parse(fecha)
            val now     = java.time.Instant.now()
            val diff    = java.time.Duration.between(instant, now)
            when {
                diff.toMinutes() < 60 -> "${diff.toMinutes()} min"
                diff.toHours()   < 24 -> "${diff.toHours()}h"
                else                  -> "Ayer"
            }
        } catch (e: Exception) { "" }
    }
}