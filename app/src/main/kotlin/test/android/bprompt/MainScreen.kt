package test.android.bprompt

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import test.android.bprompt.provider.EncryptedLocalDataProvider
import test.android.bprompt.provider.FinalEncryptedLocalDataProvider
import test.android.bprompt.util.showToast
import test.android.bprompt.util.toPaddings
import java.security.AlgorithmParameterGenerator
import java.security.AlgorithmParameters
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

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
//    keyStore.deleteEntry(keyName) // todo
    val key = keyStore.getKey(keyName, null)
    if (key != null) return key as SecretKey
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
        .build()
    keyGenerator.init(spec)
    return keyGenerator.generateKey()
}

//private var iv: ByteArray? = null
private var parameters: AlgorithmParameters? = null

private fun encrypt(
    algorithm: String,
    blocks: String,
    paddings: String,
    decrypted: String,
): ByteArray {
    val key = getSecretKey(
        algorithm = algorithm,
        blocks = blocks,
        paddings = paddings,
    )
    val cipher = Cipher.getInstance( "$algorithm/$blocks/$paddings")
//    val iv = "foobar"
//    val ivParameters = IvParameterSpec(iv.toByteArray())
    cipher.init(Cipher.ENCRYPT_MODE, key)
//    cipher.init(Cipher.ENCRYPT_MODE, key, ivParameters)
    val encrypted = cipher.doFinal(decrypted.toByteArray())
//    error("parameters: ${cipher.parameters}")
//    iv = cipher.iv
    parameters = cipher.parameters
    return encrypted
}

private fun decrypt(
    algorithm: String,
    blocks: String,
    paddings: String,
    encrypted: ByteArray,
): String {
    val key = getSecretKey(
        algorithm = algorithm,
        blocks = blocks,
        paddings = paddings,
    )
    val cipher = Cipher.getInstance( "$algorithm/$blocks/$paddings")
//    val iv = Base64.decode("foobar", Base64.NO_WRAP)
//    val iv = checkNotNull(iv)
    val parameters = checkNotNull(parameters)
//    cipher.init(Cipher.DECRYPT_MODE, key)
//    cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
    cipher.init(Cipher.DECRYPT_MODE, key, parameters)
    return String(cipher.doFinal(encrypted))
}

@Composable
internal fun MainScreen() {
    val insets = LocalView.current.rootWindowInsets.toPaddings()
    val context = LocalContext.current
    val algorithm = KeyProperties.KEY_ALGORITHM_AES
//    val blocks = KeyProperties.BLOCK_MODE_GCM
    val blocks = KeyProperties.BLOCK_MODE_CBC
//    val paddings = KeyProperties.ENCRYPTION_PADDING_NONE
    val paddings = KeyProperties.ENCRYPTION_PADDING_PKCS7
    val ldp: EncryptedLocalDataProvider = remember {
        FinalEncryptedLocalDataProvider(context)
    }
    val secret = "foobar"
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
                    val encrypted = encrypt(
                        algorithm = algorithm,
                        blocks = blocks,
                        paddings = paddings,
                        decrypted = secret,
                    )
                    context.showToast("encrypted: " + encrypted.size + " bytes")
                }
            )
            Button(
                text = "decrypt",
                onClick = {
                    val encrypted = encrypt(
                        algorithm = algorithm,
                        blocks = blocks,
                        paddings = paddings,
                        decrypted = secret,
                    )
                    val decrypted = decrypt(
                        algorithm = algorithm,
                        blocks = blocks,
                        paddings = paddings,
                        encrypted = encrypted,
                    )
                    context.showToast("decrypted: $decrypted")
                }
            )
        }
    }
}
