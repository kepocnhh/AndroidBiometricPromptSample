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

private fun withBiometric(context: Context) {
    val biometricManager = BiometricManager.from(context)
    val status = biometricManager.canAuthenticate(BiometricUtil.authenticators)
    when (status) {
        BiometricManager.BIOMETRIC_SUCCESS -> {
            val activity = context.findActivity() ?: TODO("No activity!")
            BiometricUtil.authenticate(activity)
        }
        else -> TODO("authenticators: ${BiometricUtil.authenticators} status: $status")
    }
}

private fun enc_dec(context: Context, secret: String) {
    val encrypted = SecurityUtil.encrypt(secret)
    context.showToast("encrypted: " + encrypted.size)
    val decrypted = SecurityUtil.decrypt(encrypted)
    context.showToast("decrypted: $decrypted")
}

@Composable
internal fun MainScreen() {
    val insets = LocalView.current.rootWindowInsets.toPaddings()
    val context = LocalContext.current
    val secret = remember { "${Date()}" }
    LaunchedEffect(Unit) {
        BiometricUtil.broadcast.collect {
            when (it) {
                is BiometricUtil.Broadcast.OnError -> {
                    TODO("on error: ${it.code}")
                }
                is BiometricUtil.Broadcast.OnSucceeded -> {
                    val cryptoObject = it.result.cryptoObject ?: TODO("No crypto object!")
                    val cipher = cryptoObject.cipher ?: TODO("No cipher!")
//                    val encrypted = SecurityUtil.encrypt(secret)
                    val encrypted = cipher.doFinal(secret.toByteArray())
                    context.showToast("encrypted: " + encrypted.size)
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
                text = "encrypt/decrypt",
                onClick = {
                    try {
                        enc_dec(context = context, secret = secret)
                    } catch (e: UserNotAuthenticatedException) {
                        context.showToast("error: $e")
                    }
                }
            )
            Button(
                text = "with biometric",
                onClick = {
                    withBiometric(context = context)
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
