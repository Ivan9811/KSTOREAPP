package com.ucompensar.kstoreapp.UI.PROFESIONAL.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.ucompensar.kstoreapp.MainActivity
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.PROFESIONAL.EditarPerfilProfesionalActivity
import com.ucompensar.kstoreapp.process.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

class PerfilProfesionalFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_perfil_profesional, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cargarDatosPerfil(view)

        view.findViewById<View>(R.id.itemEditarPerfil).setOnClickListener {
            startActivity(Intent(requireContext(), EditarPerfilProfesionalActivity::class.java))
        }

        // ✅ Mis Publicaciones — ID correcto: fragmentContainer
        view.findViewById<View>(R.id.itemMisPublicaciones).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, MisPublicacionesFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.itemCerrarSesion).setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    SupabaseClient.client.auth.signOut()
                } catch (e: Exception) {
                    Log.e("PERFIL_PROF", "Error signOut: ${e.message}")
                } finally {
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { cargarDatosPerfil(it) }
    }

    private fun cargarDatosPerfil(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                    ?: return@launch

                // ── Profile ───────────────────────────────────────────────────
                val profile = SupabaseClient.client.postgrest
                    .from("profiles")
                    .select { filter { eq("id", uid) } }
                    .decodeSingle<Map<String, JsonElement>>()

                val nombre    = profile["nombre"]?.jsonPrimitive?.content ?: "Profesional"
                val fotoUrl   = profile["foto_url"]?.jsonPrimitive?.content ?: ""
                val iniciales = nombre.split(" ")
                    .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("")

                // ── Profesional (calificación y reseñas desde Supabase) ───────
                val profData = try {
                    SupabaseClient.client.postgrest
                        .from("profesionales")
                        .select { filter { eq("id", uid) } }
                        .decodeSingle<Map<String, JsonElement>>()
                } catch (_: Exception) { null }

                val calificacion = profData?.get("calificacion")?.jsonPrimitive?.doubleOrNull ?: 0.0
                val totalResenas = profData?.get("total_resenas")?.jsonPrimitive?.intOrNull ?: 0

                // ── Contrataciones completadas desde pedidos ──────────────────
                val contrataciones = try {
                    SupabaseClient.client.postgrest
                        .from("pedidos")
                        .select { filter {
                            eq("profesional_id", uid)
                            eq("estado", "completado")
                        }}
                        .decodeList<Map<String, JsonElement>>()
                        .size
                } catch (_: Exception) { 0 }

                // ── UI ────────────────────────────────────────────────────────
                requireActivity().runOnUiThread {
                    view.findViewById<TextView>(R.id.tvNombrePerfil)?.text = nombre
                    view.findViewById<TextView>(R.id.tvCalificacionPerfil)?.text =
                        "⭐ ${String.format("%.1f", calificacion)}"

                    //  Stats dinámicos (antes eran hardcodeados)
                    view.findViewById<TextView>(R.id.tvTotalResenas)?.text = "$totalResenas"
                    view.findViewById<TextView>(R.id.tvContrataciones)?.text = "$contrataciones"
                    view.findViewById<TextView>(R.id.tvRatingNumero)?.text = String.format("%.1f", calificacion)

                    // Avatar: foto o iniciales
                    val tvIniciales = view.findViewById<TextView>(R.id.tvIniciales)
                    val ivAvatar    = view.findViewById<ImageView>(R.id.ivAvatar)

                    if (fotoUrl.isNotEmpty()) {
                        tvIniciales?.visibility = View.GONE
                        ivAvatar?.visibility    = View.VISIBLE
                        ivAvatar?.load(fotoUrl) {
                            transformations(CircleCropTransformation())
                            placeholder(R.drawable.bg_avatar_purple)
                        }
                    } else {
                        tvIniciales?.visibility = View.VISIBLE
                        tvIniciales?.text       = iniciales
                        ivAvatar?.visibility    = View.GONE
                    }
                }

            } catch (e: Exception) {
                Log.e("PERFIL_PROF", "Error: ${e.message}", e)
            }
        }
    }
}