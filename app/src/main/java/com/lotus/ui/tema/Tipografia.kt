package com.lotus.ui.tema

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lotus.R

// Outfit e Nunito Sans são fontes variáveis: um arquivo, um peso por eixo `wght`.
@OptIn(ExperimentalTextApi::class)
private fun variavel(arquivo: Int, peso: FontWeight) =
    Font(arquivo, peso, variationSettings = FontVariation.Settings(FontVariation.weight(peso.weight)))

/** Títulos, botões e nomes de zona. */
val Outfit = FontFamily(
    variavel(R.font.outfit, FontWeight.Normal),
    variavel(R.font.outfit, FontWeight.Medium),
    variavel(R.font.outfit, FontWeight.SemiBold),
    variavel(R.font.outfit, FontWeight.Bold),
)

/** Texto corrido. */
val NunitoSans = FontFamily(
    variavel(R.font.nunito_sans, FontWeight.Normal),
    variavel(R.font.nunito_sans, FontWeight.SemiBold),
    variavel(R.font.nunito_sans, FontWeight.Bold),
)

/** Números, horários e rótulos técnicos ("CHUVA", "02:38", "ESP32 · 7 zonas"). */
val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

private val Padrao = Typography()

private fun TextStyle.outfit(peso: FontWeight = FontWeight.Bold) = copy(fontFamily = Outfit, fontWeight = peso)
private fun TextStyle.nunito(peso: FontWeight = FontWeight.Normal) = copy(fontFamily = NunitoSans, fontWeight = peso)

internal val Tipografia = Typography(
    displayLarge = Padrao.displayLarge.outfit(),
    displayMedium = Padrao.displayMedium.outfit(),
    displaySmall = Padrao.displaySmall.outfit(),
    headlineLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 46.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = Padrao.bodyLarge.nunito(),
    bodyMedium = TextStyle(fontFamily = NunitoSans, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = NunitoSans, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp),
)

/** Contagem regressiva no anel. */
val NumeroGrande = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 28.sp)

/** Valores dos sensores e horários da agenda. */
val NumeroMedio = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp)

/** Rótulo técnico em caixa alta: "CHUVA", "RESTANTE". */
val RotuloMono = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Normal, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 1.sp)

/** Linha técnica: "ESP32 · Casa do Fernando · 7 zonas", "12 min". */
val TextoMono = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
