package net.chasmine.oneline.ui.components

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import net.chasmine.oneline.R

/**
 * Lottieアニメーションを使用したローディングインジケーター
 *
 * @param modifier モディファイア
 * @param size アニメーションのサイズ
 * @param lottieResId 使用するLottieアニメーションのリソースID（デフォルト: checklist_cubaan）
 */
@Composable
fun LottieLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    @RawRes lottieResId: Int = R.raw.checklist_cubaan
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(lottieResId)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(size)
    )
}
