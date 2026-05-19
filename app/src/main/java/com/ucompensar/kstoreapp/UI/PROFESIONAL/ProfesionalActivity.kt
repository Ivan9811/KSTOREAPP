package com.ucompensar.kstoreapp.UI.PROFESIONAL

import androidx.fragment.app.Fragment
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.BaseActivity
import com.ucompensar.kstoreapp.UI.PROFESIONAL.fragments.InicioProfesionalFragments
import com.ucompensar.kstoreapp.UI.PROFESIONAL.fragments.MensajesProfesionalFragment
import com.ucompensar.kstoreapp.UI.PROFESIONAL.fragments.PerfilProfesionalFragment
import com.ucompensar.kstoreapp.UI.PROFESIONAL.fragments.PedidosProfesionalFragment
import com.ucompensar.kstoreapp.UI.PROFESIONAL.fragments.publicacionProfesional

class ProfesionalActivity : BaseActivity() {

    override fun getStatusBarColor()  = "#4A0E8F"
    override fun getMenuRes()         = R.menu.menu_profesional
    override fun getFragmentInicial() = InicioProfesionalFragments()

    override fun onNavItemSelected(itemId: Int): Fragment? = when (itemId) {
        R.id.nav_inicio   -> InicioProfesionalFragments()
        R.id.nav_pedidos  -> PedidosProfesionalFragment()  // ✅ corregido
        R.id.nav_publicar -> publicacionProfesional()
        R.id.nav_mensajes -> MensajesProfesionalFragment()
        R.id.nav_perfil   -> PerfilProfesionalFragment()
        else              -> null
    }
}

