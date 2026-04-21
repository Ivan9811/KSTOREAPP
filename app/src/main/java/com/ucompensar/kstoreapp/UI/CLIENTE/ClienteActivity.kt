package com.ucompensar.kstoreapp.UI.CLIENTE

import androidx.fragment.app.Fragment
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.BaseActivity
import com.ucompensar.kstoreapp.UI.CLIENTE.fragments.InicioFragment
import com.ucompensar.kstoreapp.UI.CLIENTE.fragments.BuscarFragment
import com.ucompensar.kstoreapp.UI.CLIENTE.fragments.FavoritosFragment
import com.ucompensar.kstoreapp.UI.CLIENTE.fragments.MensajesFragment
import com.ucompensar.kstoreapp.UI.CLIENTE.fragments.PerfilClienteFragment

class ClienteActivity : BaseActivity() {

    override fun getStatusBarColor()  = "#4A0E8F" // 👈 morado como el diseño
    override fun getMenuRes()         = R.menu.menu_cliente
    override fun getFragmentInicial() = InicioFragment()


    override fun onNavItemSelected(itemId: Int): Fragment? = when (itemId) {
        R.id.nav_inicio     -> InicioFragment()
        R.id.nav_buscar     -> BuscarFragment()
        R.id.nav_favoritos  -> FavoritosFragment()
        R.id.nav_mensajes   -> MensajesFragment()
        R.id.nav_perfil     -> PerfilClienteFragment()
        else                -> null
    }
}