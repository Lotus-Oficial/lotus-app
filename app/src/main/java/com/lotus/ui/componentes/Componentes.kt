package com.lotus.ui.componentes

import android.content.res.Configuration
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lotus.R
import com.lotus.ui.haQuanto
import com.lotus.ui.tema.Lotus
import com.lotus.ui.tema.TomDeEstado
import kotlinx.coroutines.delay

/** Claro e escuro lado a lado em todo @Preview do app. */
@Preview(name = "Claro", showBackground = true, backgroundColor = 0xFFF5F3EE, widthDp = 390)
@Preview(name = "Escuro", showBackground = true, backgroundColor = 0xFF0C1F25, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class PreviewLotus

/** Relógio que anda de segundo em segundo: base da contagem regressiva local. */
@Composable
fun agoraMs(): Long {
    val agora by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1_000 - System.currentTimeMillis() % 1_000)
            value = System.currentTimeMillis()
        }
    }
    return agora
}

/** Pílula de estado: sempre ícone + texto, nunca só a cor. [contorno] = "Aguardando" do canvas. */
@Composable
fun Pilula(
    texto: String,
    icone: ImageVector?,
    tom: TomDeEstado,
    modifier: Modifier = Modifier,
    contorno: Boolean = false,
) {
    Surface(
        color = if (contorno) Color.Transparent else tom.fundo,
        contentColor = tom.texto,
        shape = MaterialTheme.shapes.small,
        border = if (contorno) BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline) else null,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = if (icone != null) 10.dp else 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        ) {
            if (icone != null) {
                Icon(icone, contentDescription = null, tint = tom.cor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(texto, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Cartão branco com borda fina, como os do canvas. */
@Composable
fun Cartao(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    forma: Shape = MaterialTheme.shapes.medium,
    conteudo: @Composable () -> Unit,
) {
    val borda = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val cor = MaterialTheme.colorScheme.surfaceContainerLowest
    if (onClick != null) {
        Surface(onClick = onClick, color = cor, shape = forma, border = borda, modifier = modifier, content = conteudo)
    } else {
        Surface(color = cor, shape = forma, border = borda, modifier = modifier, content = conteudo)
    }
}

/** O traço dourado que o canvas põe embaixo dos títulos. */
@Composable
fun TracoDourado(largura: Dp = 64.dp, modifier: Modifier = Modifier) {
    Spacer(
        modifier
            .width(largura)
            .height(4.dp)
            .background(Lotus.destaque.dourado, RoundedCornerShape(2.dp)),
    )
}

/**
 * Cartão de destaque petróleo, com o anel dourado e o círculo lilás de enfeite no canto.
 * Igual nos dois temas.
 */
@Composable
fun CartaoDestaque(
    modifier: Modifier = Modifier,
    anel: Boolean = true,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val d = Lotus.destaque
    Box(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(d.fundo),
    ) {
        Enfeite(anel)
        Column(Modifier.padding(horizontal = 24.dp, vertical = 22.dp), content = conteudo)
    }
}

/** [anel] = false quando o cartão já tem o anel de progresso: fica só o círculo lilás, apagado. */
@Composable
private fun BoxScope.Enfeite(anel: Boolean) {
    val d = Lotus.destaque
    if (!anel) {
        Spacer(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-40).dp)
                .size(110.dp)
                .background(d.lilas.copy(alpha = 0.35f), CircleShape),
        )
        return
    }
    Spacer(
        Modifier
            .align(Alignment.TopEnd)
            .offset(x = (-30).dp, y = (-18).dp)
            .size(72.dp)
            .border(5.dp, d.dourado, CircleShape),
    )
    Spacer(
        Modifier
            .align(Alignment.TopEnd)
            .offset(x = 46.dp, y = 22.dp)
            .size(104.dp)
            .background(d.lilas, CircleShape),
    )
}

/** Anel de progresso dourado do canvas, com o miolo escuro e o conteúdo no centro. */
@Composable
fun AnelProgresso(
    progresso: Float,
    modifier: Modifier = Modifier,
    tamanho: Dp = 116.dp,
    conteudo: @Composable () -> Unit,
) {
    val d = Lotus.destaque
    Box(modifier.size(tamanho), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val traco = 9.dp.toPx()
            val lado = size.minDimension - traco
            val canto = Offset(traco / 2, traco / 2)
            drawArc(d.anelTrilho, 0f, 360f, false, canto, Size(lado, lado), style = Stroke(traco))
            drawArc(
                d.dourado, -90f, 360f * progresso.coerceIn(0f, 1f), false, canto, Size(lado, lado),
                style = Stroke(traco, cap = StrokeCap.Round),
            )
        }
        Box(
            Modifier.size(tamanho - 18.dp).background(d.anelMiolo, CircleShape),
            contentAlignment = Alignment.Center,
        ) { conteudo() }
    }
}

/**
 * Cartão de alerta no topo da tela: o que aconteceu e o que a pessoa pode fazer.
 * [fundo]/[conteudo] vêm de um [TomDeEstado] ou do par error/errorContainer do tema.
 */
@Composable
fun CartaoAlerta(
    icone: ImageVector,
    titulo: String,
    texto: String,
    fundo: Color,
    conteudo: Color,
    modifier: Modifier = Modifier,
    acao: Pair<String, () -> Unit>? = null,
) {
    Surface(
        color = fundo,
        contentColor = conteudo,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(Modifier.padding(16.dp)) {
            Icon(icone, contentDescription = null, modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(2.dp))
                Text(texto, style = MaterialTheme.typography.bodyMedium)
                if (acao != null) {
                    TextButton(onClick = acao.second, modifier = Modifier.padding(top = 4.dp)) {
                        Text(acao.first, color = conteudo)
                    }
                }
            }
        }
    }
}

/** Faixa de quadro fora do ar, com há quanto tempo a casa deu sinal. */
@Composable
fun FaixaForaDoAr(vistoEm: Long?, modifier: Modifier = Modifier) {
    CartaoAlerta(
        icone = Icons.Rounded.CloudOff,
        titulo = "O quadro está fora do ar",
        texto = buildString {
            append("Mostrando o último estado conhecido")
            if (vistoEm != null) append(", visto ${haQuanto(vistoEm, System.currentTimeMillis())}")
            append(". A agenda continua rodando no quadro mesmo sem internet.")
        },
        fundo = MaterialTheme.colorScheme.errorContainer,
        conteudo = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier,
    )
}

/** Bloco de esqueleto que pulsa enquanto os dados não chegam. */
@Composable
fun Esqueleto(modifier: Modifier = Modifier, forma: Shape = MaterialTheme.shapes.small) {
    val pulso by rememberInfiniteTransition(label = "esqueleto").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )
    Spacer(
        modifier
            .alpha(pulso)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, forma),
    )
}

enum class EstiloBotao {
    /** Petróleo cheio: ação principal ("Entrar", "Salvar agenda"). */
    Principal,
    /** Dourado cheio: chamada de destaque ("Começar", "Ver detalhes", "Regar tudo"). */
    Dourado,
    /** Só contorno petróleo ("Cancelar", "Parar tudo"). */
    Contorno,
    /** Contorno dourado claro, para usar em cima do cartão petróleo ("Pausar"). */
    ContornoNoDestaque,
}

/** Botão em pílula que envia um comando: mostra progresso até o `ack` chegar. */
@Composable
fun BotaoComando(
    texto: String,
    icone: ImageVector?,
    enviando: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    estilo: EstiloBotao = EstiloBotao.Principal,
    habilitado: Boolean = true,
    altura: Dp = 56.dp,
) {
    val conteudo: @Composable () -> Unit = {
        if (enviando) {
            CircularProgressIndicator(strokeWidth = 2.dp, color = LocalContentColor.current, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        } else if (icone != null) {
            Icon(icone, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
    val m = modifier.heightIn(min = maxOf(altura, 48.dp))
    val ativo = habilitado && !enviando
    val d = Lotus.destaque
    val cs = MaterialTheme.colorScheme
    val pilula = CircleShape
    val espaco = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
    when (estilo) {
        EstiloBotao.Principal -> Button(onClick, m, enabled = ativo, shape = pilula, contentPadding = espaco) { conteudo() }
        EstiloBotao.Dourado -> Button(
            onClick, m, enabled = ativo, shape = pilula,
            colors = ButtonDefaults.buttonColors(containerColor = d.dourado, contentColor = d.sobreDourado,
                disabledContainerColor = d.dourado.copy(alpha = 0.3f), disabledContentColor = d.texto.copy(alpha = 0.55f)),
            contentPadding = espaco,
        ) { conteudo() }
        EstiloBotao.Contorno -> OutlinedButton(
            onClick, m, enabled = ativo, shape = pilula, contentPadding = espaco,
            border = BorderStroke(2.dp, cs.primary.copy(alpha = if (ativo) 1f else 0.38f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.primary),
        ) { conteudo() }
        EstiloBotao.ContornoNoDestaque -> OutlinedButton(
            onClick, m, enabled = ativo, shape = pilula, contentPadding = espaco,
            border = BorderStroke(1.5.dp, d.rotulo.copy(alpha = if (ativo) 1f else 0.38f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = d.rotulo, disabledContentColor = d.rotulo.copy(alpha = 0.38f)),
        ) { conteudo() }
    }
}

/** Título de seção (Outfit) com algo opcional à direita: um link ou um contador em mono. */
@Composable
fun TituloSecao(texto: String, modifier: Modifier = Modifier, acao: (@Composable () -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth().heightIn(min = 44.dp),
    ) {
        Text(texto, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        acao?.invoke()
    }
}

/** Símbolo do Lótus num quadradinho creme: some no fundo claro e aparece no escuro, como no canvas. */
@Composable
fun LogoPequeno(tamanho: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(Lotus.destaque.texto, RoundedCornerShape(10.dp))
            .padding(4.dp),
    ) {
        Image(painterResource(R.drawable.lotus_icone), contentDescription = null, modifier = Modifier.size(tamanho))
    }
}
