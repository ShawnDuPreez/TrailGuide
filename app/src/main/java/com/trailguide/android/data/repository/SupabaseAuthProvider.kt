package com.trailguide.android.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides access to the currently authenticated Supabase user.
 * Extracted so repositories can be tested without wiring Supabase plugins.
 */
@Singleton
class SupabaseAuthProvider @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    fun currentUserId(): String? {
        return supabaseClient.auth.currentUserOrNull()?.id
    }
}

