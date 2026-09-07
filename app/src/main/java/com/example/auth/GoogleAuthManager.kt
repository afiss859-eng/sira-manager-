package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SiraRole(val label: String) {
    SUPER_ADMIN("Super Administrateur"),
    MERCHANT("Commerçant Titulaire"),
    STORE_MANAGER("Gérant de Boutique"),
    CASHIER("Caissier")
}

data class SiraAuthUser(
    val id: String,
    val email: String,
    val displayName: String,
    val role: SiraRole,
    val shopName: String,
    val city: String,
    val photoUrl: String? = null
)

class GoogleAuthManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sira_auth_session", Context.MODE_PRIVATE)

    private val adminEmail = "sawadogoafis125@gmail.com"

    private val _currentUser = MutableStateFlow(loadInitialUser())
    val currentUser: StateFlow<SiraAuthUser> = _currentUser.asStateFlow()

    private fun loadInitialUser(): SiraAuthUser {
        val savedEmail = prefs.getString("user_email", adminEmail) ?: adminEmail
        val savedId = prefs.getString("user_id", sanitizeId(savedEmail)) ?: sanitizeId(savedEmail)
        val savedName = prefs.getString("user_name", "Afis Sawadogo") ?: "Afis Sawadogo"
        val savedShop = prefs.getString("user_shop", "SIRA Direction Centrale") ?: "SIRA Direction Centrale"
        val savedCity = prefs.getString("user_city", "Ouagadougou") ?: "Ouagadougou"
        val role = if (savedEmail.equals(adminEmail, ignoreCase = true) || savedEmail.contains("admin")) {
            SiraRole.SUPER_ADMIN
        } else {
            SiraRole.MERCHANT
        }

        return SiraAuthUser(
            id = savedId,
            email = savedEmail,
            displayName = savedName,
            role = role,
            shopName = savedShop,
            city = savedCity
        )
    }

    fun signInWithGoogle(email: String, displayName: String, shopName: String, city: String): SiraAuthUser {
        val cleanEmail = email.trim().lowercase()
        val role = if (cleanEmail.equals(adminEmail, ignoreCase = true) || cleanEmail.contains("admin")) {
            SiraRole.SUPER_ADMIN
        } else {
            SiraRole.MERCHANT
        }
        val id = sanitizeId(cleanEmail)

        val user = SiraAuthUser(
            id = id,
            email = cleanEmail,
            displayName = displayName.ifBlank { cleanEmail.substringBefore("@").replace(".", " ").capitalize() },
            role = role,
            shopName = shopName.ifBlank { "Commerce $city" },
            city = city.ifBlank { "Ouagadougou" }
        )

        prefs.edit()
            .putString("user_id", user.id)
            .putString("user_email", user.email)
            .putString("user_name", user.displayName)
            .putString("user_shop", user.shopName)
            .putString("user_city", user.city)
            .apply()

        _currentUser.value = user
        return user
    }

    fun signOut(context: Context) {
        // Switch back to admin or prompt login
        signInWithGoogle(adminEmail, "Afis Sawadogo", "SIRA Direction Centrale", "Ouagadougou")
    }

    private fun sanitizeId(email: String): String {
        return email.replace("@", "_").replace(".", "_").replace("-", "_")
    }

    companion object {
        @Volatile
        private var INSTANCE: GoogleAuthManager? = null

        fun getInstance(context: Context): GoogleAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GoogleAuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
