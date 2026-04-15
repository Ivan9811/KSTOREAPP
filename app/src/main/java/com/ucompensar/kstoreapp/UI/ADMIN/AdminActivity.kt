package com.ucompensar.kstoreapp.UI.ADMIN

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.ucompensar.kstoreapp.MainActivity
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.UI.ADMIN.fragments.DashboardFragment
import com.ucompensar.kstoreapp.UI.ADMIN.fragments.ReportesFragment
import com.ucompensar.kstoreapp.UI.ADMIN.fragments.ServiciosFragment
import com.ucompensar.kstoreapp.UI.ADMIN.fragments.UsuariosFragment
import androidx.core.graphics.toColorInt

class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // Fijar color barra de estado
        window.statusBarColor = "#4A0E8F".toColorInt()

        // Evitar que el contenido suba debajo de la cámara
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Fragment inicial
        cargarFragment(DashboardFragment())

        // Navegación BottomNav
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.itemIconTintList = null
        bottomNav.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio    -> cargarFragment(DashboardFragment())
                R.id.nav_usuarios  -> cargarFragment(UsuariosFragment())
                R.id.nav_servicios -> cargarFragment(ServiciosFragment())
                R.id.nav_reportes  -> cargarFragment(ReportesFragment())
                R.id.nav_salir     -> cerrarSesion()
            }
            true
        }
    }

    fun cargarFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun cerrarSesion() {
        // FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}