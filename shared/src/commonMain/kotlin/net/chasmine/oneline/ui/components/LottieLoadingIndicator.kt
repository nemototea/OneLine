package net.chasmine.oneline.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import oneline.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Compottieを使用したローディングインジケーター（Compose Multiplatform対応）
 *
 * @param modifier モディファイア
 * @param size アニメーションのサイズ
 * @param lottieFileName 使用するLottieアニメーションのファイル名（デフォルト: checklist_cubaan.json）
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun LottieLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    lottieFileName: String = "checklist_cubaan.json"
) {
    val composition = rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/$lottieFileName").decodeToString()
        )
    }

    val progress = animateLottieCompositionAsState(
        composition = composition.value,
        iterations = Compottie.IterateForever
    )

    androidx.compose.foundation.Image(
        painter = rememberLottiePainter(
            composition = composition.value,
            progress = { progress.value }
        ),
        contentDescription = null,
        modifier = modifier.size(size)
    )
}
