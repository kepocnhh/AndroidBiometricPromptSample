package test.android.bprompt.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.MasterKeys
import test.android.bprompt.BuildConfig
import java.security.AlgorithmParameters
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

internal object SecurityUtil {
    val algorithm = KeyProperties.KEY_ALGORITHM_AES
        val blocks = KeyProperties.BLOCK_MODE_GCM
//    val blocks = KeyProperties.BLOCK_MODE_CBC
        val paddings = KeyProperties.ENCRYPTION_PADDING_NONE
//    val paddings = KeyProperties.ENCRYPTION_PADDING_PKCS7
    val keyAlias = BuildConfig.APPLICATION_ID + ":foo:3"
    private var parameters: AlgorithmParameters? = null

    fun getKeyOrCreate(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        if (!keyStore.containsAlias(keyAlias)) {
            val purposes = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            val keySize = 256
            val spec = KeyGenParameterSpec
                .Builder(keyAlias, purposes)
                .setBlockModes(blocks)
                .setEncryptionPaddings(paddings)
                .setKeySize(keySize)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .setUserAuthenticationValidityDurationSeconds(0)
                .build()
            val keyGenerator = KeyGenerator.getInstance(algorithm, keyStore.provider)
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }
        return keyStore.getKey(keyAlias, null) as SecretKey
    }

    fun deleteEntry() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        keyStore.deleteEntry(keyAlias)
    }

    fun getCipher(): Cipher {
        return Cipher.getInstance( "$algorithm/$blocks/$paddings")
    }

    fun encrypt(decrypted: String): ByteArray {
        val cipher = getCipher()
        val key = getKeyOrCreate()
        cipher.init(Cipher.ENCRYPT_MODE, key)
        parameters = cipher.parameters
        return cipher.doFinal(decrypted.toByteArray())
    }

    fun decrypt(encrypted: ByteArray): String {
        val cipher = getCipher()
        val key = getKeyOrCreate()
        cipher.init(Cipher.DECRYPT_MODE, key, parameters)
        return String(cipher.doFinal(encrypted))
    }
}
