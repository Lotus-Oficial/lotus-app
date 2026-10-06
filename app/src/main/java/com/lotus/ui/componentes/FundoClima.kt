package com.lotus.ui.componentes

import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lotus.ui.Tempo
import com.lotus.ui.tema.CoresClima
import com.lotus.ui.tema.Lotus
import com.lotus.ui.tema.LotusTheme
import kotlin.math.PI
import kotlin.math.cos

/**
 * Fundo animado do Início conforme o [tempo] (componente "Animação de clima" do canvas):
 * sol com raios girando no canto de cima, nuvens passando, e gotas caindo na chuva.
 * Só enfeite: o texto da tela já diz se está chovendo. `null` = sem animação.
 * Com as animações do Android desligadas (acessibilidade), fica parado no primeiro quadro.
 */
@Composable
fun FundoClima(tempo: Tempo?, modifier: Modifier = Modifier) {
    val relogio = relogioDaAnimacao()
    val cores = Lotus.clima
    Crossfade(tempo, animationSpec = tween(800), modifier = modifier.clipToBounds(), label = "clima") { t ->
        Box(Modifier.fillMaxSize()) {
            when (t) {
                Tempo.SOL -> Sol(cores, relogio, Modifier.fillMaxSize())
                Tempo.NUBLADO -> Nuvens(cores, relogio, chuva = false, Modifier.fillMaxWidth().height(AlturaNuvens))
                Tempo.CHUVA -> Nuvens(cores, relogio, chuva = true, Modifier.fillMaxWidth().height(AlturaNuvens))
                null -> Unit
            }
        }
    }
}

// Medidas do canvas (tela de 390 de largura), em dp.
private val AlturaNuvens = 422.dp

/** Segundos desde que a animação apareceu; lido só no desenho, então não recompõe a tela. */
@Composable
internal fun relogioDaAnimacao(): State<Float> {
    val segundos = remember { mutableFloatStateOf(0f) }
    val contexto = LocalContext.current
    val desligadas = remember(contexto) {
        runCatching {
            Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    if (!desligadas) {
        LaunchedEffect(Unit) {
            val inicio = withFrameNanos { it }
            while (true) withFrameNanos { segundos.floatValue = (it - inicio) / 1e9f }
        }
    }
    return segundos
}

/** Onde está um ciclo de [duracao] s que começou [atraso] s antes (0 a 1), como no CSS. */
private fun fase(t: Float, duracao: Float, atraso: Float): Float {
    val f = ((t + atraso) / duracao) % 1f
    return if (f < 0f) f + 1f else f
}

/** Vai e volta suave de 0 a 1 em [periodo] s. */
private fun pulso(t: Float, periodo: Float): Float = 0.5f - 0.5f * cos(2f * PI.toFloat() * t / periodo)

private fun entre(de: Float, ate: Float, f: Float) = de + (ate - de) * f

@Composable
private fun Sol(cores: CoresClima, relogio: State<Float>, modifier: Modifier) {
    // Liquid Glass: sol e raios levemente desfocados (só Android 12+; antes fica nítido).
    Canvas(modifier.blur(5.dp, BlurredEdgeTreatment.Unbounded)) {
        val t = relogio.value
        // Sol meio escondido no canto de cima, à direita.
        val centro = Offset(size.width + 15.dp.toPx(), -25.dp.toPx())
        val alcance = 760.dp.toPx()
        val raios = Brush.radialGradient(
            0.18f to cores.raio,
            0.55f to cores.raio.copy(alpha = cores.raio.alpha * 0.6f),
            1f to Color.Transparent,
            center = centro,
            radius = alcance,
        )
        scale(1f + 0.04f * pulso(t, 6f), pivot = centro) {
            rotate(t / 120f * 360f, pivot = centro) {
                // 24 raios de 6°, um a cada 15°.
                for (i in 0 until 24) {
                    drawArc(
                        brush = raios,
                        startAngle = i * 15f,
                        sweepAngle = 6f,
                        useCenter = true,
                        topLeft = centro - Offset(alcance, alcance),
                        size = Size(alcance * 2, alcance * 2),
                    )
                }
            }
        }
        val halo = 130.dp.toPx() * (1f + 0.06f * pulso(t, 4f))
        drawCircle(
            Brush.radialGradient(0f to cores.halo, 0.65f to Color.Transparent, center = centro, radius = halo),
            radius = halo,
            center = centro,
        )
        drawCircle(cores.bordaSol, 62.dp.toPx(), centro)
        drawCircle(cores.sol, 56.dp.toPx(), centro)
        // Reflexo em cruz sobre o sol, que brilha devagar.
        reflexo(centro, 280.dp.toPx(), 4.dp.toPx(), cores.reflexo, 0.4f + 0.6f * pulso(t, 5f))
        reflexo(centro, 4.dp.toPx(), 125.dp.toPx(), cores.reflexo, 0.4f + 0.6f * pulso(t + 2f, 6f))
    }
}

/** Elipse de luz de raios [rx] × [ry], mais forte no meio. */
private fun DrawScope.reflexo(centro: Offset, rx: Float, ry: Float, cor: Color, alfa: Float) {
    val r = maxOf(rx, ry)
    scale(rx / r, ry / r, pivot = centro) {
        drawCircle(
            Brush.radialGradient(
                0f to cor.copy(alpha = cor.alpha * alfa),
                0.3f to cor.copy(alpha = cor.alpha * alfa * 0.55f),
                0.7f to Color.Transparent,
                center = centro,
                radius = r,
            ),
            radius = r,
            center = centro,
        )
    }
}

private class Nuvem(val topo: Float, val escala: Float, val duracao: Float, val atraso: Float)

private val nuvens = listOf(
    Nuvem(20f, 1.1f, 46f, 8f),
    Nuvem(92f, 0.75f, 60f, 34f),
    Nuvem(150f, 0.95f, 52f, 20f),
    Nuvem(48f, 0.6f, 70f, 52f),
)

/** Posições "aleatórias" fixas, as mesmas do canvas. */
private class Gota(val esquerda: Float, val altura: Float, val duracao: Float, val atraso: Float)

private val gotas = List(44) { i ->
    Gota(
        esquerda = (i * 37) % 118 / 100f,
        altura = 16f + (i * 7) % 18,
        duracao = 0.6f + (i * 13) % 45 / 100f,
        atraso = (i * 0.173f) % 1.3f,
    )
}

@Composable
private fun Nuvens(cores: CoresClima, relogio: State<Float>, chuva: Boolean, modifier: Modifier) {
    val tons = if (chuva) cores.nuvensDeChuva else cores.nuvens
    Canvas(
        modifier.graphicsLayer {
            alpha = if (chuva) 0.9f else 0.85f
            // Camada própria para o esmaecer de baixo (DstIn) só apagar o que é dela.
            compositingStrategy = CompositingStrategy.Offscreen
        },
    ) {
        val t = relogio.value
        val u = 1.dp.toPx()
        nuvens.forEachIndexed { i, n ->
            val k = n.escala * u
            val x = entre(-150f * k, size.width, fase(t, n.duracao, n.atraso))
            val y = n.topo * u
            val cor = tons[i % tons.size]
            drawRoundRect(cor, Offset(x, y + 26f * k), Size(150f * k, 44f * k), CornerRadius(22f * k))
            drawCircle(cor, 28f * k, Offset(x + 52f * k, y + 32f * k))
            drawCircle(cor, 35f * k, Offset(x + 97f * k, y + 23f * k))
        }
        if (chuva) {
            gotas.forEach { g ->
                val h = g.altura * u
                val x = g.esquerda * size.width
                val y = entre(-h, size.height, fase(t, g.duracao, g.atraso))
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color.Transparent, cores.gota), startY = y, endY = y + h),
                    Offset(x, y),
                    Size(2f * u, h),
                    CornerRadius(u),
                )
            }
        }
        // Some aos poucos da metade para baixo, para não brigar com o conteúdo.
        drawRect(Brush.verticalGradient(0.55f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
    }
}

@PreviewLotus
@Composable
private fun SolPreview() {
    LotusTheme { FundoDePreview(Tempo.SOL) }
}

@PreviewLotus
@Composable
private fun NubladoPreview() {
    LotusTheme { FundoDePreview(Tempo.NUBLADO) }
}

@PreviewLotus
@Composable
private fun ChuvaPreview() {
    LotusTheme { FundoDePreview(Tempo.CHUVA) }
}

@Composable
private fun FundoDePreview(tempo: Tempo) {
    Surface(color = MaterialTheme.colorScheme.background) { FundoClima(tempo, Modifier.size(390.dp, 422.dp)) }
}
