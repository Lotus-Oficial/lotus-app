package com.lotus.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

private val LocalCoresDeEstado = staticCompositionLocalOf { EstadoClaro }

// Raios do canvas: 14 (campos, linhas), 20 (cartões), 24 (destaque), 28 (botões e folhas).
private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Tema do Lótus. Sem cor dinâmica do Android 12+: a paleta é a do canvas,
 * igual em todos os aparelhos.
 */
@Composable
fun LotusTheme(
    escuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalCoresDeEstado provides if (escuro) EstadoEscuro else EstadoClaro) {
        MaterialTheme(
            colorScheme = if (escuro) CoresEscuras else CoresClaras,
            typography = Tipografia,
            shapes = Formas,
            content = content,
        )
    }
}

/** Atalhos: `Lotus.estado.regando.cor`, `Lotus.destaque.fundo`. */
object Lotus {
    val estado: CoresDeEstado
        @Composable @ReadOnlyComposable get() = LocalCoresDeEstado.current

    val destaque: CoresDestaque get() = Destaque
}
