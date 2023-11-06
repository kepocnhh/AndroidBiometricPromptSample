package test.android.bprompt.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal fun Int.px(density: Float): Dp {
    return (this / density).dp
}

@Composable
internal fun Int.px(density: Density = LocalDensity.current): Dp {
    return px(density = density.density)
}
