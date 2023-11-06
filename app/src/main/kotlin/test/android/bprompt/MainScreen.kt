package test.android.bprompt

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
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
import androidx.compose.runtime.mutableStateOf
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
import test.android.bprompt.util.BiometricUtil
import test.android.bprompt.util.SecurityUtil
import test.android.bprompt.util.findActivity
import test.android.bprompt.util.showToast
import test.android.bprompt.util.toPaddings
import java.security.AlgorithmParameters
import java.security.Key
import java.security.KeyStore
import java.security.KeyStoreException
import java.util.Date
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

private fun withoutBiometric(context: Context, secret: String) {
    val encrypted = try {
        SecurityUtil.encrypt(secret)
    } catch (e: Throwable) {
        context.showToast("error: $e")
        return
    }
    context.showToast("encrypted: " + encrypted.size)
}

private fun withBiometric(context: Context, type: BiometricUtil.Broadcast.OnSucceeded.Type) {
    val biometricManager = BiometricManager.from(context)
    val status = biometricManager.canAuthenticate(BiometricUtil.authenticators)
    when (status) {
        BiometricManager.BIOMETRIC_SUCCESS -> {
            val activity = context.findActivity() ?: TODO("No activity!")
            BiometricUtil.authenticate(activity, type)
        }
        else -> TODO("authenticators: ${BiometricUtil.authenticators} status: $status")
    }
}

@Composable
internal fun MainScreen() {
    val insets = LocalView.current.rootWindowInsets.toPaddings()
    val context = LocalContext.current
    val secret = remember { "${Date()}" }
    val encryptedState = remember { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(Unit) {
        BiometricUtil.broadcast.collect {
            when (it) {
                is BiometricUtil.Broadcast.OnError -> {
                    TODO("on error: ${it.code}")
                }
                is BiometricUtil.Broadcast.OnSucceeded -> {
                    when (it.type) {
                        BiometricUtil.Broadcast.OnSucceeded.Type.ENCRYPTION -> {
                            val encrypted = it.cipher.doFinal(secret.toByteArray())
                            encryptedState.value = encrypted
                            context.showToast("encrypted: " + encrypted.size)
                        }
                        BiometricUtil.Broadcast.OnSucceeded.Type.DECRYPTION -> {
                            val encrypted = encryptedState.value ?: TODO("No encrypted!")
                            val decrypted = it.cipher.doFinal(encrypted)
                            context.showToast("decrypted: " + String(decrypted))
                        }
                    }
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
                    withBiometric(context = context, BiometricUtil.Broadcast.OnSucceeded.Type.ENCRYPTION)
                },
            )
            Button(
                text = "decrypt",
                onClick = {
                    withBiometric(context = context, BiometricUtil.Broadcast.OnSucceeded.Type.DECRYPTION)
                },
            )
            Button(
                text = "encrypt without biometric",
                onClick = {
                    withoutBiometric(context, secret = secret)
                }
            )
            Button(
                text = "getKeyOrCreate",
                onClick = {
                    val key = SecurityUtil.getKeyOrCreate()
                    context.showToast("key: ${key.hashCode()}")
                }
            )
            Button(
                text = "deleteEntry",
                onClick = {
                    SecurityUtil.deleteEntry()
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
