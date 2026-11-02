package com.example.githubprofileexplorer.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores the OAuth token encrypted with an AES key that lives in the Android Keystore. */
class SecureTokenStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val key: SecretKey

    init {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        key = (store.getKey(KEY_ALIAS, null) as? SecretKey) ?: run {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            generator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            generator.generateKey()
        }
    }

    fun save(token: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        preferences.edit {
            putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            putString("token", Base64.encodeToString(cipher.doFinal(token.toByteArray()), Base64.NO_WRAP))
        }
    }

    fun read(): String? = runCatching {
        val iv = Base64.decode(preferences.getString("iv", null), Base64.DEFAULT)
        val value = Base64.decode(preferences.getString("token", null), Base64.DEFAULT)
        Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        }.doFinal(value).toString(StandardCharsets.UTF_8)
    }.getOrNull()

    fun isLoggedIn(): Boolean = read() != null

    fun clear() = preferences.edit { clear() }

    private companion object {
        const val PREFS_NAME = "secure_auth"
        const val KEY_ALIAS = "github_profile_explorer_oauth"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
