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
import javax.crypto.Cipher

internal object BiometricUtil {
    sealed interface Broadcast {
        data class OnError(val code: Int) : Broadcast
        data class OnSucceeded(val result: BiometricPrompt.AuthenticationResult) : Broadcast
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val _broadcast = MutableSharedFlow<Broadcast>()
    val broadcast = _broadcast.asSharedFlow()
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun authenticate(activity: FragmentActivity) {
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("BiometricPrompt:${BuildConfig.APPLICATION_ID}:title")
            .setSubtitle("BiometricPrompt:${BuildConfig.APPLICATION_ID}:subtitle")
            .setAllowedAuthenticators(authenticators)
            .setConfirmationRequired(true)
            .setDeviceCredentialAllowed(true)
            .build()
//        BiometricPrompt(activity, callback).authenticate(info)
        val cipher = SecurityUtil.getCipher()
        cipher.init(Cipher.ENCRYPT_MODE, SecurityUtil.getKeyOrCreate())
        BiometricPrompt(activity, callback).authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    private val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            scope.launch {
                _broadcast.emit(Broadcast.OnError(code = errorCode))
            }
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            scope.launch {
                _broadcast.emit(Broadcast.OnSucceeded(result = result))
            }
        }

        override fun onAuthenticationFailed() {
            println("[BiometricPrompt] failed!")
            TODO("onAuthenticationFailed")
        }
    }
}