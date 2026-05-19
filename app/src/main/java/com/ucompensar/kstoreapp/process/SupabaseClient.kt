package com.ucompensar.kstoreapp.process

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://yxzalatyaeyvvctxuquo.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inl4emFsYXR5YWV5dnZjdHh1cXVvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzY2MjY3NjIsImV4cCI6MjA5MjIwMjc2Mn0.aN3Z5IDiQBKlXyJgfT6R9Mzj2KWx5hu5rVr9PYnFYOA"
    ){
        install(Auth) {
            flowType = FlowType.PKCE
        }
        install(Postgrest)
        install(Storage)
    }
}