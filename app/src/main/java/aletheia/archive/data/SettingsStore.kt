@file:Suppress("DEPRECATION")

package aletheia.archive.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.GeneralSecurityException

data class Gate(
    val apiKey: String,
    val model: String,
    val language: String,
)

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = open(context.applicationContext)

    fun read(): Gate {
        val model = prefs.getString(MODEL, DEFAULT_MODEL).orEmpty().trim()
        val language = prefs.getString(LANGUAGE, DEFAULT_LANGUAGE).orEmpty().trim()
        return Gate(
            apiKey = prefs.getString(KEY, "").orEmpty(),
            model = model,
            language = language.ifBlank { DEFAULT_LANGUAGE },
        )
    }

    fun write(gate: Gate) {
        prefs.edit()
            .putString(KEY, gate.apiKey.trim())
            .putString(MODEL, gate.model.trim())
            .putString(LANGUAGE, gate.language.ifBlank { DEFAULT_LANGUAGE })
            .apply()
    }

    private fun open(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (exception: GeneralSecurityException) {
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        } catch (exception: Exception) {
            // Keystore failures also surface as runtime exceptions on some devices.
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        }
    }

    companion object {
        const val DEFAULT_MODEL = "openrouter/auto"
        const val DEFAULT_LANGUAGE = "English"
        private const val FILE = "aletheia.gate"
        private const val KEY = "api_key"
        private const val MODEL = "model"
        private const val LANGUAGE = "language"
    }
}
