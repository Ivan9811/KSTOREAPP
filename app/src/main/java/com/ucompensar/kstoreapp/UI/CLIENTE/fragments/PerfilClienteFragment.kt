package com.ucompensar.kstoreapp.UI.CLIENTE.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ucompensar.kstoreapp.MainActivity
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.CLIENTE.EditarPerfilActivity

class PerfilClienteFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_perfil_cliente, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.itemFavoritos).setOnClickListener {
            (activity as? com.ucompensar.kstoreapp.UI.BaseActivity)
                ?.cargarFragment(FavoritosFragment())
        }
        view.findViewById<View>(R.id.itemMisPedidos).setOnClickListener {
            (activity as? com.ucompensar.kstoreapp.UI.BaseActivity)
                ?.cargarFragment(MisPedidosFragment())
        }

        view.findViewById<View>(R.id.itemCerrarSesion).setOnClickListener {
            startActivity(Intent(requireContext(), MainActivity::class.java))
            activity?.finish()
        }
        view.findViewById<View>(R.id.itemEditarPerfil).setOnClickListener {
            startActivity(Intent(requireContext(), EditarPerfilActivity::class.java))
        }
    }
}