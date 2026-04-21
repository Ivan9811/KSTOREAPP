package com.ucompensar.kstoreapp.UI

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.ucompensar.kstoreapp.MainActivity
import com.ucompensar.kstoreapp.R

abstract class BaseActivity : AppCompatActivity() {

    abstract fun getStatusBarColor(): String
    abstract fun getMenuRes(): Int
    abstract fun getFragmentInicial(): Fragment
    abstract fun onNavItemSelected(itemId: Int): Fragment?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        window.statusBarColor = getStatusBarColor().toColorInt()
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Fragment inicial según el rol
        cargarFragment(getFragmentInicial())

        // BottomNav
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.itemIconTintList = null
        bottomNav.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        bottomNav.menu.clear()
        bottomNav.inflateMenu(getMenuRes())

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.nav_salir) {
                cerrarSesion()
            } else {
                onNavItemSelected(item.itemId)?.let { cargarFragment(it) }
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