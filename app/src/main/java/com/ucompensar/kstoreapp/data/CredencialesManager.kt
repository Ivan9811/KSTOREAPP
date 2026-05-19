package com.ucompensar.kstoreapp

import android.app.Application
import com.ucompensar.kstoreapp.process.SupabaseClient
import io.github.jan.supabase.auth.auth

class KStoreApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializar el storage de sesión con el contexto de Android
        SupabaseClient.client.auth.apply {
            // Esto activa la persistencia de sesión en SharedPreferences
        }
    }
}