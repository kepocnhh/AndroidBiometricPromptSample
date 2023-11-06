package test.android.bprompt.util

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.fragment.app.FragmentActivity

internal fun Context.showToast(message: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

internal fun Context.findActivity(): FragmentActivity? {
    return when(this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
