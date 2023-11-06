package test.android.bprompt

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import test.android.bprompt.provider.EncryptedLocalDataProvider
import test.android.bprompt.provider.FinalEncryptedLocalDataProvider
import test.android.bprompt.util.findActivity
import test.android.bprompt.util.showToast
import test.android.bprompt.util.toPaddings
import java.security.AlgorithmParameters
import java.security.Key
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

@Composable
private fun Button(
    text: String,
    onClick: () -> Unit,
) {
    BasicText(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .wrapContentSize(),
        text = text,
        style = TextStyle(
            color = Color.White,
        ),
    )
}

private fun getSecretKey(
    algorithm: String,
    blocks: String,
    paddings: String,
): SecretKey {
    val keyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)
    val keyName = BuildConfig.APPLICATION_ID + ":master:foo"
//    keyStore.deleteEntry(keyName); TODO(); // todo
//    val key = keyStore.getKey(keyName, null); if (key != null) return key as SecretKey
    val keyGenerator = KeyGenerator.getInstance(
        algorithm,
        keyStore.provider,
    )
    val purposes = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    val keySize = 256
    val spec = KeyGenParameterSpec
        .Builder(keyName, purposes)
        .setBlockModes(blocks)
        .setEncryptionPaddings(paddings)
        .setKeySize(keySize)
        .setUserAuthenticationRequired(true)
//        .setUserConfirmationRequired(true)
        .setInvalidatedByBiometricEnrollment(false)
        .setUserAuthenticationValidityDurationSeconds(-1)
        .build()
    keyGenerator.init(spec)
    return keyGenerator.generateKey()
}

//private var iv: ByteArray? = null
private var parameters: AlgorithmParameters? = null

private fun getCipher(
    algorithm: String,
    blocks: String,
    paddings: String,
): Cipher {
    return Cipher.getInstance( "$algorithm/$blocks/$paddings")
}

private fun encrypt(
    cipher: Cipher,
    key: SecretKey,
    decrypted: String,
): ByteArray {
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val encrypted = cipher.doFinal(decrypted.toByteArray())
    parameters = cipher.parameters
    return encrypted
}

private fun decrypt(
    cipher: Cipher,
    key: SecretKey,
    encrypted: ByteArray,
): String {
    val parameters = checkNotNull(parameters)
    cipher.init(Cipher.DECRYPT_MODE, key, parameters)
    return String(cipher.doFinal(encrypted))
}

private val scope = CoroutineScope(Dispatchers.Main + Job())

private sealed interface Broadcast {
    data class OnError(val code: Int) : Broadcast
    data class OnSucceeded(val result: BiometricPrompt.AuthenticationResult) : Broadcast
}

private val broadcast = MutableSharedFlow<Broadcast>()

private val callback = object : BiometricPrompt.AuthenticationCallback() {
    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        scope.launch {
            broadcast.emit(Broadcast.OnError(code = errorCode))
        }
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        scope.launch {
            broadcast.emit(Broadcast.OnSucceeded(result = result))
        }
    }

    override fun onAuthenticationFailed() {
        println("[BiometricPrompt] failed!")
        TODO("onAuthenticationFailed")
    }
}

@Composable
internal fun MainScreen() {
    val insets = LocalView.current.rootWindowInsets.toPaddings()
    val context = LocalContext.current
    val activity = context.findActivity() ?: TODO()
    val biometricManager = remember { BiometricManager.from(context) }
//    val authenticators = BiometricManager.Authenticators.DEVICE_CREDENTIAL
//    val authenticators = BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_WEAK
//    val authenticators = BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_STRONG
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    val algorithm = KeyProperties.KEY_ALGORITHM_AES
//    val blocks = KeyProperties.BLOCK_MODE_GCM
    val blocks = KeyProperties.BLOCK_MODE_CBC
//    val paddings = KeyProperties.ENCRYPTION_PADDING_NONE
    val paddings = KeyProperties.ENCRYPTION_PADDING_PKCS7
//    val ldp: EncryptedLocalDataProvider = remember {
//        FinalEncryptedLocalDataProvider(context)
//    }
    val secret = "foobar"
    LaunchedEffect(Unit) {
        broadcast.collect {
            when (it) {
                is Broadcast.OnError -> {
                    TODO("on error: ${it.code}")
                }
                is Broadcast.OnSucceeded -> {
                    val ldp = FinalEncryptedLocalDataProvider(context)
                    ldp.secret = "time: " + System.currentTimeMillis()
                    val decrypted = ldp.secret
                    context.showToast("decrypted: $decrypted")
//                    val cryptoObject = it.result.cryptoObject ?: TODO()
//                    println("""
//                        cipher: ${cryptoObject.cipher}
//                    """.trimIndent())
//                    val encrypted = encrypt(
//                        algorithm = algorithm,
//                        blocks = blocks,
//                        paddings = paddings,
//                        decrypted = secret,
//                    )
//                    context.showToast("encrypted: " + encrypted.size + " bytes")
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(insets),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
        ) {
            Button(
                text = "encrypt",
                onClick = {
                    val status = biometricManager.canAuthenticate(authenticators)
                    when (status) {
                        BiometricManager.BIOMETRIC_SUCCESS -> {
//                            ldp.secret = "time: " + System.currentTimeMillis()
//                            val decrypted = ldp.secret
//                            context.showToast("decrypted: $decrypted")
                            val info = BiometricPrompt.PromptInfo.Builder()
                                .setTitle("BiometricPrompt:${BuildConfig.APPLICATION_ID}:title")
                                .setSubtitle("BiometricPrompt:${BuildConfig.APPLICATION_ID}:subtitle")
                                .setAllowedAuthenticators(authenticators)
                                .setConfirmationRequired(true)
                                .setDeviceCredentialAllowed(true)
                                .build()
//                            val cipher = getCipher(algorithm, blocks, paddings)
//                            val key = getSecretKey(
//                                algorithm = algorithm,
//                                blocks = blocks,
//                                paddings = paddings,
//                            )
//                        val key: SecretKey? = null
//                            cipher.init(Cipher.ENCRYPT_MODE, key)
//                            BiometricPrompt(activity, callback).authenticate(info, BiometricPrompt.CryptoObject(cipher))
                            BiometricPrompt(activity, callback).authenticate(info)
                        }
                        else -> TODO("authenticators: $authenticators status: $status")
                    }
                }
            )
            Button(
                text = "decrypt",
                onClick = {
                    onDecrypt(context)
//                    val encrypted = encrypt(
//                        algorithm = algorithm,
//                        blocks = blocks,
//                        paddings = paddings,
//                        decrypted = secret,
//                    )
//                    val decrypted = decrypt(
//                        algorithm = algorithm,
//                        blocks = blocks,
//                        paddings = paddings,
//                        encrypted = encrypted,
//                    )
//                    context.showToast("decrypted: $decrypted")
                }
            )
        }
    }
}

private fun onDecrypt(context: Context) {
    val ldp = try {
        FinalEncryptedLocalDataProvider(context)
    } catch (e: Throwable) {
        context.showToast("error: $e")
        return
    }
//    ldp.secret = "time: " + System.currentTimeMillis()
    val decrypted = ldp.secret
    context.showToast("decrypted: $decrypted")
}
