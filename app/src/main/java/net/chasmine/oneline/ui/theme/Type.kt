package net.chasmine.oneline.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import net.chasmine.oneline.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val notoSansJpFont = GoogleFont("Noto Sans JP")

val NotoSansJpFontFamily = FontFamily(
    Font(googleFont = notoSansJpFont, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = notoSansJpFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = notoSansJpFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = notoSansJpFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = notoSansJpFont, fontProvider = provider, weight = FontWeight.Bold)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = NotoSansJpFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily = NotoSansJpFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NotoSansJpFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NotoSansJpFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = NotoSansJpFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp
    )
)