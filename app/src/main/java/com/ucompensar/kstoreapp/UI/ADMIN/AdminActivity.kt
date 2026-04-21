package com.ucompensar.kstoreapp.UI.ADMIN

import androidx.fragment.app.Fragment
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.BaseActivity
import com.ucompensar.kstoreapp.UI.ADMIN.fragments.DashboardFragment
import com.ucompensar.kstoreapp.UI.ADMIN.fragments.ReportesFragment
import com.ucompensar.kstoreapp.UI.ADMIN.fragments.ServiciosFragment
import com.ucompensar.kstoreapp.UI.ADMIN.fragments.UsuariosFragment

class AdminActivity : BaseActivity() {

    override fun getStatusBarColor()  = "#4A0E8F"
    override fun getMenuRes()         = R.menu.menu_admin
    override fun getFragmentInicial() = DashboardFragment()

    override fun onNavItemSelected(itemId: Int): Fragment? = when (itemId) {
        R.id.nav_inicio    -> DashboardFragment()
        R.id.nav_usuarios  -> UsuariosFragment()
        R.id.nav_servicios -> ServiciosFragment()
        R.id.nav_reportes  -> ReportesFragment()
        else               -> null
    }
}