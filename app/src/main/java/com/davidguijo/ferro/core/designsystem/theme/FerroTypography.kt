package com.davidguijo.ferro.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.davidguijo.ferro.R

val BarlowFontFamily: FontFamily =
    FontFamily(
        Font(R.font.barlow_regular, FontWeight.Normal),
        Font(R.font.barlow_semibold, FontWeight.SemiBold)
    )

val BarlowCondensedFontFamily: FontFamily =
    FontFamily(
        Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
        Font(R.font.barlow_condensed_bold, FontWeight.Bold)
    )

/**
 * Escala tipográfica de Ferro derivada de docs/design/tokens.json.
 * Las cifras (`numberXl`, `number`) usan cifras tabulares (`tnum`).
 */
@Immutable
data class FerroTypography(
    val display: TextStyle =
        TextStyle(
            fontFamily = BarlowCondensedFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.01.em
        ),
    val title: TextStyle =
        TextStyle(
            fontFamily = BarlowCondensedFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 32.sp
        ),
    val numberXl: TextStyle =
        TextStyle(
            fontFamily = BarlowCondensedFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 48.sp,
            lineHeight = 48.sp,
            fontFeatureSettings = "tnum"
        ),
    val number: TextStyle =
        TextStyle(
            fontFamily = BarlowCondensedFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontFeatureSettings = "tnum"
        ),
    val heading: TextStyle =
        TextStyle(
            fontFamily = BarlowFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
    val body: TextStyle =
        TextStyle(
            fontFamily = BarlowFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
    val bodyStrong: TextStyle =
        TextStyle(
            fontFamily = BarlowFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
    val caption: TextStyle =
        TextStyle(
            fontFamily = BarlowFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
    val label: TextStyle =
        TextStyle(
            fontFamily = BarlowFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.08.em
        )
)

internal fun FerroTypography.toMaterialTypography(): Typography = Typography(
    displayLarge = display,
    titleLarge = title,
    titleMedium = heading,
    bodyLarge = body,
    bodyMedium = caption,
    labelLarge = bodyStrong,
    labelSmall = label
)

internal val LocalFerroTypography = staticCompositionLocalOf { FerroTypography() }
