package com.lotus.ui.tema

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Fonte da verdade: o canvas "Lótus — App" (https://claude.ai/artifact/5Xoe1wWhiVDjNoU5sNxMGe).
private val Petroleo = Color(0xFF1F5F73)
private val PetroleoFundo = Color(0xFF164756)
private val Creme = Color(0xFFF5F3EE)
private val Dourado = Color(0xFFC9A54C)
private val DouradoClaro = Color(0xFFE4C77E)
private val DouradoSuave = Color(0xFFF1E7CC)
private val DouradoTexto = Color(0xFF7A5C17)
private val SobreDourado = Color(0xFF2B2210)
private val Lilas = Color(0xFFB3A4D6)
private val LilasSuave = Color(0xFFE6E0F3)
private val LilasTexto = Color(0xFF5A4A8A)
private val SobreLilas = Color(0xFF2E2347)

internal val CoresClaras = lightColorScheme(
    primary = Petroleo,
    onPrimary = Creme,
    primaryContainer = Color(0xFFDDE7EA),
    onPrimaryContainer = PetroleoFundo,
    secondary = Dourado,
    onSecondary = SobreDourado,
    secondaryContainer = DouradoSuave,
    onSecondaryContainer = DouradoTexto,
    tertiary = Lilas,
    onTertiary = SobreLilas,
    tertiaryContainer = LilasSuave,
    onTertiaryContainer = LilasTexto,
    background = Creme,
    onBackground = Petroleo,
    surface = Creme,
    onSurface = Petroleo,
    surfaceVariant = Color(0xFFE6ECEE),
    onSurfaceVariant = Color(0xFF3E5A63),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAF9F5),
    surfaceContainer = Color(0xFFEFECE4),
    surfaceContainerHigh = Color(0xFFE6ECEE),
    surfaceContainerHighest = Color(0xFFDCE4E6),
    outline = Color(0xFF8FA3A9),
    outlineVariant = Color(0xFFE6E2D8),
)

internal val CoresEscuras = darkColorScheme(
    primary = Color(0xFF9CCBD8),
    onPrimary = Color(0xFF0C2E38),
    primaryContainer = Color(0xFF17404C),
    onPrimaryContainer = Color(0xFFDDE7EA),
    secondary = DouradoClaro,
    onSecondary = SobreDourado,
    secondaryContainer = Color(0xFF45391A),
    onSecondaryContainer = DouradoSuave,
    tertiary = Color(0xFFCFC4EC),
    onTertiary = SobreLilas,
    tertiaryContainer = Color(0xFF3B3158),
    onTertiaryContainer = LilasSuave,
    background = Color(0xFF0C1F25),
    onBackground = Color(0xFFEEF0EA),
    surface = Color(0xFF0C1F25),
    onSurface = Color(0xFFEEF0EA),
    surfaceVariant = Color(0xFF1B3A44),
    onSurfaceVariant = Color(0xFFA9BEC4),
    // No escuro os cartões (surfaceContainerLowest) ficam um pouco mais claros que o fundo.
    surfaceContainerLowest = Color(0xFF12303A),
    surfaceContainerLow = Color(0xFF0F2830),
    surfaceContainer = Color(0xFF153540),
    surfaceContainerHigh = Color(0xFF1B3D48),
    surfaceContainerHighest = Color(0xFF224652),
    outline = Color(0xFF6E878E),
    outlineVariant = Color(0xFF22424C),
)

/**
 * Um estado em três tons: [cor] para ícone e preenchimento,
 * [fundo] e [texto] para pílulas e cartões (texto sobre fundo passa 4.5:1).
 */
@Immutable
data class TomDeEstado(val cor: Color, val fundo: Color, val texto: Color)

/**
 * Cartão de destaque petróleo do canvas ("Próxima irrigação", "Regando agora"):
 * igual nos dois temas, com o anel dourado e o círculo lilás de enfeite.
 */
@Immutable
data class CoresDestaque(
    val fundo: Color,
    val texto: Color,
    val textoSuave: Color,
    val rotulo: Color,      // dourado claro: "Regando agora", "Próxima irrigação"
    val dourado: Color,     // anel, traço, botão
    val sobreDourado: Color,
    val lilas: Color,       // círculo de enfeite, zona feita
    val sobreLilas: Color,
    val anelTrilho: Color,
    val anelMiolo: Color,
)

/** Cores de estado do app: regando (lilás), tudo certo (verde), atenção (dourado), água (petróleo). */
@Immutable
data class CoresDeEstado(
    val regando: TomDeEstado,
    val ok: TomDeEstado,
    val atencao: TomDeEstado,
    val agua: TomDeEstado,
)

internal val Destaque = CoresDestaque(
    fundo = Petroleo,
    texto = Creme,
    textoSuave = Color(0xFFCFDDE1),
    rotulo = DouradoClaro,
    dourado = Dourado,
    sobreDourado = SobreDourado,
    lilas = Lilas,
    sobreLilas = SobreLilas,
    anelTrilho = Color(0xFF2B6E83),
    anelMiolo = Color(0xFF174C5C),
)

internal val EstadoClaro = CoresDeEstado(
    regando = TomDeEstado(cor = LilasTexto, fundo = Lilas, texto = SobreLilas),
    ok = TomDeEstado(cor = Color(0xFF2E9D6B), fundo = Color(0xFFDCEFE3), texto = Color(0xFF1E5E40)),
    atencao = TomDeEstado(cor = Dourado, fundo = DouradoSuave, texto = Color(0xFF6B4F12)),
    agua = TomDeEstado(cor = Petroleo, fundo = Color(0xFFDDE7EA), texto = PetroleoFundo),
)

internal val EstadoEscuro = CoresDeEstado(
    regando = TomDeEstado(cor = Color(0xFFCFC4EC), fundo = Color(0xFF3B3158), texto = LilasSuave),
    ok = TomDeEstado(cor = Color(0xFF6FD3A0), fundo = Color(0xFF173A2B), texto = Color(0xFF9BE5BF)),
    atencao = TomDeEstado(cor = DouradoClaro, fundo = Color(0xFF45391A), texto = DouradoSuave),
    agua = TomDeEstado(cor = Color(0xFF9CCBD8), fundo = Color(0xFF17404C), texto = Color(0xFFDDE7EA)),
)
