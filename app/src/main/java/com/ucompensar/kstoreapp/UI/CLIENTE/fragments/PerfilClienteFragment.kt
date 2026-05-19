package com.ucompensar.kstoreapp.UI.CLIENTE.fragments

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
import com.ucompensar.kstoreapp.UI.CLIENTE.EditarPerfilActivity
import com.ucompensar.kstoreapp.process.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

class PerfilClienteFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_perfil_cliente, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cargarDatosPerfil(view)

        view.findViewById<View>(R.id.itemFavoritos).setOnClickListener {
            (activity as? com.ucompensar.kstoreapp.UI.BaseActivity)
                ?.cargarFragment(FavoritosFragment())
        }
        view.findViewById<View>(R.id.itemMisPedidos).setOnClickListener {
            (activity as? com.ucompensar.kstoreapp.UI.BaseActivity)
                ?.cargarFragment(MisPedidosFragment())
        }
        view.findViewById<View>(R.id.itemEditarPerfil).setOnClickListener {
            startActivity(Intent(requireContext(), EditarPerfilActivity::class.java))
        }
        view.findViewById<View>(R.id.itemCerrarSesion).setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    SupabaseClient.client.auth.signOut()
                } catch (e: Exception) {
                    Log.e("PERFIL", "Error signOut: ${e.message}")
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

                val resultado = SupabaseClient.client.postgrest
                    .from("profiles")
                    .select { filter { eq("id", uid) } }
                    .decodeSingle<Map<String, kotlinx.serialization.json.JsonElement>>()

                val nombre   = resultado["nombre"]?.jsonPrimitive?.content ?: "Usuario"
                val fotoUrl  = resultado["foto_url"]?.jsonPrimitive?.content ?: ""
                val iniciales = nombre.split(" ")
                    .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("")

                requireActivity().runOnUiThread {
                    view.findViewById<TextView>(R.id.tvNombrePerfil)?.text = nombre

                    val tvIniciales = view.findViewById<TextView>(R.id.tvIniciales)
                    val ivAvatar   = view.findViewById<ImageView>(R.id.ivAvatar)

                    if (!fotoUrl.isNullOrEmpty()) {
                        // ✅ Mostrar foto si existe
                        tvIniciales?.visibility = View.GONE
                        ivAvatar?.visibility    = View.VISIBLE
                        ivAvatar?.load(fotoUrl) {
                            transformations(CircleCropTransformation())
                            placeholder(R.drawable.bg_avatar_purple)
                        }
                    } else {
                        // Mostrar iniciales si no hay foto
                        tvIniciales?.visibility = View.VISIBLE
                        tvIniciales?.text       = iniciales
                        ivAvatar?.visibility    = View.GONE
                    }
                }

            } catch (e: Exception) {
                Log.e("PERFIL", "Error: ${e.message}", e)
            }
        }
    }
}