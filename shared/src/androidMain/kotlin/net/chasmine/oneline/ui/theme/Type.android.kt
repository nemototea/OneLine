package net.chasmine.oneline.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import net.chasmine.oneline.shared.R

actual val NotoSansJpFontFamily: FontFamily = FontFamily(
    Font(R.font.noto_sans_jp_light, FontWeight.Light),
    Font(R.font.noto_sans_jp_regular, FontWeight.Normal),
    Font(R.font.noto_sans_jp_medium, FontWeight.Medium),
    Font(R.font.noto_sans_jp_semibold, FontWeight.SemiBold),
    Font(R.font.noto_sans_jp_bold, FontWeight.Bold)
)
