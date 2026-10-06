package com.lotus.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.lotus.ui.tema.Lotus
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlin.math.PI
import kotlin.math.sin

/**
 * O que o vidro desfoca: o fundo de cada tela (sol, nuvens, bolha lilás) marcado com
 * [fonteDoVidro]. Vem do `LotusApp`; sem ele (nos @Preview) o vidro fica só translúcido.
 */
val LocalVidro = staticCompositionLocalOf<HazeState?> { null }

/** Marca este fundo como o que fica atrás do vidro. */
@Composable
fun Modifier.fonteDoVidro(): Modifier {
    val estado = LocalVidro.current ?: return this
    return hazeSource(estado)
}

/**
 * Superfície Liquid Glass do canvas: [cor] translúcida sobre o fundo desfocado, borda clara
 * e brilho na borda de cima. Abaixo do Android 12 não há desfoque, só a cor translúcida.
 * [opaco]: o que passa por baixo nunca aparece nítido (a barra, que flutua sobre a lista).
 */
@Composable
fun Modifier.vidro(forma: Shape, cor: Color = Lotus.vidro.fundo, opaco: Boolean = false): Modifier {
    val v = Lotus.vidro
    val estado = LocalVidro.current
    val fundo = MaterialTheme.colorScheme.background
    val superficie = if (estado != null) {
        Modifier.hazeEffect(
            estado,
            HazeStyle(
                backgroundColor = fundo,
                tint = HazeTint(cor),
                blurRadius = 24.dp,
                noiseFactor = 0f,
                fallbackTint = if (opaco) HazeTint(cor.compositeOver(fundo)) else HazeTint.Unspecified,
            ),
        )
    } else if (opaco) {
        Modifier.background(cor.compositeOver(fundo))
    } else {
        Modifier.background(cor)
    }
    return clip(forma)
        .then(superficie)
        .border(1.dp, Brush.verticalGradient(0f to v.brilho, 0.3f to v.borda), forma)
}

/** Borda e brilho do vidro em superfícies cheias (cartão petróleo, aba aberta, dia marcado). */
@Composable
fun Modifier.bordaDeVidro(forma: Shape): Modifier {
    val v = Lotus.vidro
    return border(1.dp, Brush.verticalGradient(0f to v.brilhoDestaque, 0.3f to v.bordaDestaque), forma)
}

/**
 * Círculo lilás que boia devagar atrás do conteúdo, como no fundo de Zonas e Agenda no canvas:
 * é o que aparece desfocado através do vidro. [x] e [y] = canto de cima à esquerda.
 */
@Composable
fun BolhaLilas(x: Int, y: Int, modifier: Modifier = Modifier) {
    val relogio = relogioDaAnimacao()
    Spacer(
        modifier
            .offset(x.dp, y.dp)
            .graphicsLayer { translationY = sin(2f * PI.toFloat() * relogio.value / 10f) * 12.dp.toPx() }
            .size(220.dp)
            .background(Lotus.vidro.bolha, CircleShape),
    )
}
