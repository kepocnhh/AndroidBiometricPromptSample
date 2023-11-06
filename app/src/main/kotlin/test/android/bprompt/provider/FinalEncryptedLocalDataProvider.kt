package test.android.bprompt.provider

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import test.android.bprompt.BuildConfig

internal class FinalEncryptedLocalDataProvider(
    context: Context,
) : EncryptedLocalDataProvider {
    private val prefs: SharedPreferences

    init {
        val purposes = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        val keySize = 256
        val spec = KeyGenParameterSpec
            .Builder(BuildConfig.APPLICATION_ID + ":master", purposes)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(keySize)
            .build()
        prefs = EncryptedSharedPreferences.create(
            "PreferencesFilename",
            MasterKeys.getOrCreate(spec),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override var secret: String?
        get() {
            return prefs.getString("secret", null)
        }
        set(value) {
            if (value == null) {
                prefs.edit().remove("secret").commit()
            } else {
                prefs.edit().putString("secret", value).commit()
            }
        }
}
