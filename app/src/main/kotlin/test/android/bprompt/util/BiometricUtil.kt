package test.android.bprompt.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import test.android.bprompt.BuildConfig
import test.android.bprompt.util.SecurityUtil.decryptMode
import test.android.bprompt.util.SecurityUtil.encryptMode
import javax.crypto.Cipher

internal object BiometricUtil {
    sealed interface Broadcast {
        data class OnError(val code: Int) : Broadcast
        data class OnSucceeded(val type: Type, val cipher: Cipher) : Broadcast {
            enum class Type {
                ENCRYPTION,
                DECRYPTION,
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val _broadcast = MutableSharedFlow<Broadcast>()
    val broadcast = _broadcast.asSharedFlow()
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    private fun getPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle("BiometricPrompt:${BuildConfig.APPLICATION_ID}:title")
            .setSubtitle("BiometricPrompt:${BuildConfig.APPLICATION_ID}:subtitle")
            .setAllowedAuthenticators(authenticators)
            .setConfirmationRequired(true)
            .setDeviceCredentialAllowed(true)
            .build()
    }

    fun authenticate(activity: FragmentActivity, type: Broadcast.OnSucceeded.Type) {
        val cipher = SecurityUtil.getCipher()
        when (type) {
            Broadcast.OnSucceeded.Type.ENCRYPTION -> {
                cipher.encryptMode()
            }
            Broadcast.OnSucceeded.Type.DECRYPTION -> {
                cipher.decryptMode()
            }
        }
        val callback = getAuthenticationCallback(type)
        BiometricPrompt(activity, callback).authenticate(getPromptInfo(), BiometricPrompt.CryptoObject(cipher))
    }

    private fun getAuthenticationCallback(type: Broadcast.OnSucceeded.Type) : BiometricPrompt.AuthenticationCallback {
        return object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                scope.launch {
                    _broadcast.emit(Broadcast.OnError(code = errorCode))
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                scope.launch {
                    val cryptoObject = result.cryptoObject ?: TODO("No crypto object!")
                    val cipher = cryptoObject.cipher ?: TODO("No cipher!")
                    _broadcast.emit(Broadcast.OnSucceeded(type = type, cipher = cipher))
                }
            }

            override fun onAuthenticationFailed() {
                // Called when a biometric (e.g. fingerprint, face, etc.) is presented but not recognized as belonging to the user.
                println("biometric (e.g. fingerprint, face, etc.) is presented but not recognized")
            }
        }
    }
}