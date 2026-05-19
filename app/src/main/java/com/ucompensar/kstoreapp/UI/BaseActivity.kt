package com.ucompensar.kstoreapp.UI

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.ucompensar.kstoreapp.MainActivity
import com.ucompensar.kstoreapp.R
import com.ucompensar.kstoreapp.process.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

abstract class BaseActivity : AppCompatActivity() {

    abstract fun getStatusBarColor(): String
    abstract fun getMenuRes(): Int
    abstract fun getFragmentInicial(): Fragment
    abstract fun onNavItemSelected(itemId: Int): Fragment?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars())

        window.statusBarColor = getStatusBarColor().toColorInt()
        WindowCompat.setDecorFitsSystemWindows(window, true)

        cargarFragment(getFragmentInicial())

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
            .commitAllowingStateLoss()
    }

    private fun cerrarSesion() {
        lifecycleScope.launch {
            try {
                SupabaseClient.client.auth.signOut()
            } catch (e: Exception) {
                // Si falla igual navegamos
            } finally {
                val intent = Intent(this@BaseActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }
}