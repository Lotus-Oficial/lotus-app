package com.lotus.ui.casa

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lotus.dados.Comando
import com.lotus.dados.Modo
import com.lotus.dados.Nivel
import com.lotus.dados.Site
import com.lotus.dados.Zona
import com.lotus.ui.Exemplos
import com.lotus.ui.Resumo
import com.lotus.ui.componentes.AnelProgresso
import com.lotus.ui.componentes.BotaoComando
import com.lotus.ui.componentes.Cartao
import com.lotus.ui.componentes.CartaoAlerta
import com.lotus.ui.componentes.CartaoDestaque
import com.lotus.ui.componentes.Esqueleto
import com.lotus.ui.componentes.EstiloBotao
import com.lotus.ui.componentes.LogoPequeno
import com.lotus.ui.componentes.FaixaForaDoAr
import com.lotus.ui.componentes.PontoDeSituacao
import com.lotus.ui.componentes.PreviewLotus
import com.lotus.ui.componentes.SeletorCasa
import com.lotus.ui.componentes.TituloSecao
import com.lotus.ui.componentes.TracoDourado
import com.lotus.ui.componentes.agoraMs
import com.lotus.ui.duracao
import com.lotus.ui.emCasa
import com.lotus.ui.hora
import com.lotus.ui.proximaRega
import com.lotus.ui.quando
import com.lotus.ui.quandoCurto
import com.lotus.ui.relogio
import com.lotus.ui.resumo
import com.lotus.ui.tema.Lotus
import com.lotus.ui.tema.LotusTheme
import com.lotus.ui.tema.NumeroGrande
import com.lotus.ui.tema.NumeroMedio
import com.lotus.ui.tema.RotuloMono
import com.lotus.ui.tema.TextoMono
import com.lotus.ui.texto

/**
 * Aba Zonas: o que está regando agora (anel com a contagem), a sequência das zonas,
 * e os sensores. [pendentes] são os comandos desta casa esperando `ack`.
 */
@Composable
fun ZonasTela(
    sites: List<Site>,
    casa: Site,
    pendentes: Set<Comando>,
    onEscolherCasa: (Site) -> Unit,
    onEnviar: (Comando) -> Unit,
    modifier: Modifier = Modifier,
    margens: PaddingValues = PaddingValues(),
) {
    val agora = agoraMs()
    val resumo = resumo(casa, agora)
    // Fora do ar, o destaque mostra o último estado conhecido (esmaecido), não um "nada regando".
    val ultimoEstado = if (resumo is Resumo.ForaDoAr && casa.carregado) resumo(casa.copy(online = true), agora) else resumo
    // Fora do ar ou sem dados: mostra o último estado, mas não deixa comandar.
    val podeComandar = resumo !is Resumo.ForaDoAr && resumo != Resumo.Carregando && resumo != Resumo.Manual
    // Sem água o quadro recusa abrir zona (fault_dry): nem oferece.
    val podeRegar = podeComandar && resumo !is Resumo.ComFalha && casa.sensores?.nivel != Nivel.BAIXO

    var zonaAberta by rememberSaveable { mutableStateOf<Int?>(null) }
    var adiarAberto by rememberSaveable { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf<Confirmacao?>(null) }

    /** Comandos que interrompem uma rega pedem confirmação antes. */
    fun pedir(comando: Comando) {
        val c = confirmacao(comando, resumo)
        if (c != null) confirmar = c else onEnviar(comando)
    }

    val esmaecido = Modifier.alpha(if (resumo is Resumo.ForaDoAr) 0.55f else 1f)
    // Fundo próprio: dá a cor de texto certa (onBackground) também fora do Scaffold.
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = margens.calculateTopPadding() + 24.dp,
                bottom = margens.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { LinhaDoQuadro(casa, resumo) }
            item { SeletorCasa(sites, casa, agora, onEscolherCasa) }
            if (resumo == Resumo.Carregando) {
                item { EsqueletoZonas() }
                return@LazyColumn
            }
            item { Alertas(casa, resumo, pendentes, onEnviar) }
            item {
                Agora(
                    casa = casa,
                    resumo = ultimoEstado,
                    podeComandar = podeComandar,
                    podeRegar = podeRegar,
                    pendentes = pendentes,
                    onEnviar = ::pedir,
                    onAdiar = { adiarAberto = true },
                    modifier = esmaecido,
                )
            }
            val feitas = zonasFeitas(casa)
            item {
                TituloSecao(if (feitas != null) "Sequência de agora" else "Zonas", Modifier.padding(top = 4.dp)) {
                    Text(
                        if (feitas != null) "$feitas/${casa.zonas.size} feitas" else "${casa.zonas.size} zonas",
                        style = TextoMono,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(casa.zonas, key = { it.numero }) { zona ->
                LinhaZona(
                    zona = zona,
                    feita = feitas != null && zona.numero < (casa.estado?.zonaAtiva ?: 0),
                    restanteS = (ultimoEstado as? Resumo.Regando)?.restanteS ?: (ultimoEstado as? Resumo.Pausado)?.restanteS,
                    enviando = pendentes.any { it is Comando.RegarZona && it.zona == zona.numero },
                    onAbrir = { zonaAberta = zona.numero },
                    modifier = esmaecido,
                )
            }
            item { Sensores(casa, agora, esmaecido.padding(top = 4.dp)) }
        }
    }

    casa.zona(zonaAberta)?.let { zona ->
        ZonaFolha(
            zona = zona,
            habilitado = podeComandar,
            podeRegar = podeRegar,
            pendentes = pendentes,
            onRegar = { duracaoS -> zonaAberta = null; pedir(Comando.RegarZona(zona.numero, duracaoS)) },
            onSalvarPadrao = { s -> onEnviar(Comando.MudarZona(zona.numero, s)) },
            onFechar = { zonaAberta = null },
        )
    }
    if (adiarAberto) {
        AdiarFolha(
            onAdiar = { horas -> adiarAberto = false; onEnviar(Comando.AdiarPorChuva(horas)) },
            onFechar = { adiarAberto = false },
        )
    }
    confirmar?.let { c ->
        AlertDialog(
            onDismissRequest = { confirmar = null },
            title = { Text(c.titulo) },
            text = { Text(c.texto) },
            confirmButton = { TextButton(onClick = { confirmar = null; onEnviar(c.comando) }) { Text(c.botao) } },
            dismissButton = { TextButton(onClick = { confirmar = null }) { Text("Cancelar") } },
        )
    }
}

/**
 * Quantas zonas do ciclo já regaram, ou null se não há ciclo rodando.
 * O contrato não diz isso direto: o ciclo roda as zonas em ordem, então quando a próxima
 * é a seguinte da ativa, as anteriores já foram. Rega de uma zona só não tem sequência.
 */
private fun zonasFeitas(casa: Site): Int? {
    val e = casa.estado ?: return null
    if (e.modo != Modo.REGANDO && e.modo != Modo.PAUSADO) return null
    val ativa = e.zonaAtiva ?: return null
    return if (e.proximaZona == ativa + 1) ativa - 1 else null
}

/** "ESP32 · 7 zonas" e o pontinho de online, como no topo do canvas. */
@Composable
private fun LinhaDoQuadro(casa: Site, resumo: Resumo) {
    val quadro = when (casa.info?.tipo) {
        "esp32-relay" -> "ESP32"
        "plc-delta" -> "CLP Delta"
        else -> "Quadro"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        LogoPequeno(26.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            quadro + (casa.info?.let { " · ${it.zonas} zonas" } ?: ""),
            style = TextoMono,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        PontoDeSituacao(if (resumo is Resumo.ForaDoAr) resumo else Resumo.Parado(null, false))
        Spacer(Modifier.width(6.dp))
        Text(
            when {
                resumo is Resumo.ForaDoAr -> "fora do ar"
                casa.online == true -> "online"
                else -> "conectando"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class Confirmacao(val comando: Comando, val titulo: String, val texto: String, val botao: String)

private fun confirmacao(comando: Comando, resumo: Resumo): Confirmacao? {
    val (zona, nome, restante) = when (resumo) {
        is Resumo.Regando -> Triple(resumo.zona, resumo.nomeZona, resumo.restanteS)
        is Resumo.Pausado -> Triple(resumo.zona, resumo.nomeZona, resumo.restanteS)
        else -> return null
    }
    val atual = nome ?: "a zona $zona"
    val falta = restante?.let { ", que ainda tinha ${duracao(it)}" } ?: ""
    return when (comando) {
        Comando.PararTudo -> Confirmacao(
            comando, "Parar tudo?",
            "$atual para agora$falta. As próximas zonas do ciclo não vão rodar. A agenda continua valendo.",
            "Parar tudo",
        )
        is Comando.RegarZona -> if (comando.zona == zona) null else Confirmacao(
            comando, "Trocar de zona?",
            "Isso interrompe $atual$falta e o resto do ciclo.",
            "Regar zona ${comando.zona}",
        )
        else -> null
    }
}

@Composable
private fun Alertas(site: Site, resumo: Resumo, pendentes: Set<Comando>, onEnviar: (Comando) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (resumo) {
            is Resumo.ForaDoAr -> FaixaForaDoAr(resumo.vistoEm)
            is Resumo.ComFalha -> {
                val t = resumo.falha.texto()
                CartaoAlerta(
                    Icons.Rounded.Warning, t.titulo, t.oQueFazer,
                    fundo = MaterialTheme.colorScheme.errorContainer,
                    conteudo = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Resumo.Manual -> CartaoAlerta(
                Icons.Rounded.PanTool, "Quadro no modo manual",
                "Alguém está comandando pelo painel. O app volta a comandar quando ele voltar para o automático.",
                fundo = Lotus.estado.atencao.fundo, conteudo = Lotus.estado.atencao.texto,
            )
            is Resumo.AdiadoPorChuva -> {
                val agora = emCasa(System.currentTimeMillis())
                val cancelando = Comando.AdiarPorChuva(0) in pendentes
                CartaoAlerta(
                    Icons.Rounded.Umbrella, "Agenda adiada pela chuva",
                    "Nenhuma rega automática até ${quando(resumo.ate, agora)}. Dá para regar na mão se precisar.",
                    fundo = Lotus.estado.agua.fundo, conteudo = Lotus.estado.agua.texto,
                    acao = (if (cancelando) "Cancelando…" else "Cancelar adiamento") to {
                        if (!cancelando) onEnviar(Comando.AdiarPorChuva(0))
                    },
                )
            }
            else -> Unit
        }
        if (site.sensores?.nivel == Nivel.BAIXO && resumo !is Resumo.ComFalha) {
            CartaoAlerta(
                Icons.Rounded.Warning, "Caixa d'água baixa",
                "O quadro não abre nenhuma zona até o nível voltar.",
                fundo = MaterialTheme.colorScheme.errorContainer,
                conteudo = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

/** O cartão petróleo: anel com a contagem quando rega, próxima irrigação quando não. */
@Composable
private fun Agora(
    casa: Site,
    resumo: Resumo,
    podeComandar: Boolean,
    podeRegar: Boolean,
    pendentes: Set<Comando>,
    onEnviar: (Comando) -> Unit,
    onAdiar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = Lotus.destaque
    val (zona, nome, restante) = when (resumo) {
        is Resumo.Regando -> Triple(resumo.zona, resumo.nomeZona, resumo.restanteS)
        is Resumo.Pausado -> Triple(resumo.zona ?: 0, resumo.nomeZona, resumo.restanteS)
        else -> Triple(null, null, null)
    }
    if (zona != null) {
        val total = casa.zona(zona)?.duracaoPadraoS ?: restante ?: 1
        val progresso = restante?.let { 1f - it.toFloat() / maxOf(total, it) } ?: 0f
        val pausado = resumo is Resumo.Pausado
        CartaoDestaque(modifier, anel = false) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnelProgresso(progresso) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.semantics { contentDescription = "Faltam ${duracao(restante ?: 0)}" },
                    ) {
                        Text(relogio(restante ?: 0), style = NumeroGrande, color = d.texto)
                        Text("RESTANTE", style = RotuloMono.copy(letterSpacing = RotuloMono.letterSpacing * 1.5f), color = d.textoSuave)
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (pausado) "Pausado" else "Regando agora", style = TextoMono, color = d.rotulo)
                    Text(
                        "Zona $zona · ${nome ?: ""}".trimEnd(' ', '·'),
                        style = MaterialTheme.typography.headlineSmall,
                        color = d.texto,
                    )
                    val proxima = (resumo as? Resumo.Regando)?.proximaZona
                    Text(
                        when {
                            pausado -> "Bomba e válvula desligadas"
                            proxima != null -> "Depois: ${casa.zona(proxima)?.nome ?: "zona $proxima"}"
                            else -> "Última zona"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = d.textoSuave,
                    )
                }
            }
            Botoes {
                if (pausado) {
                    BotaoComando("Retomar", Icons.Rounded.PlayArrow, Comando.Retomar in pendentes, { onEnviar(Comando.Retomar) },
                        Modifier.weight(1f), EstiloBotao.Dourado, podeComandar, altura = 44.dp)
                } else {
                    BotaoComando("Pausar", Icons.Rounded.Pause, Comando.Pausar in pendentes, { onEnviar(Comando.Pausar) },
                        Modifier.weight(1f), EstiloBotao.ContornoNoDestaque, podeComandar, altura = 44.dp)
                }
                BotaoComando("Parar", Icons.Rounded.Stop, Comando.PararTudo in pendentes, { onEnviar(Comando.PararTudo) },
                    Modifier.weight(1f), EstiloBotao.ContornoNoDestaque, podeComandar, altura = 44.dp)
            }
        }
        return
    }

    val hoje = emCasa(System.currentTimeMillis())
    val proxima = casa.agenda?.let { proximaRega(it, hoje) }
    CartaoDestaque(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(0.66f)) {
            Text(
                when {
                    resumo is Resumo.AdiadoPorChuva -> "Agenda suspensa"
                    proxima != null -> "Próxima irrigação"
                    else -> "Rega automática desligada"
                },
                style = MaterialTheme.typography.labelMedium,
                color = d.rotulo,
            )
            Text(
                when {
                    resumo is Resumo.AdiadoPorChuva -> "Nada regando"
                    proxima != null -> quandoCurto(proxima, hoje)
                    else -> "Nada regando"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = d.texto,
            )
            TracoDourado(largura = 72.dp)
        }
        Botoes {
            BotaoComando("Regar tudo", Icons.Rounded.WaterDrop, Comando.RegarTudo in pendentes, { onEnviar(Comando.RegarTudo) },
                Modifier.weight(1f), EstiloBotao.Dourado, podeRegar, altura = 44.dp)
            if (resumo !is Resumo.AdiadoPorChuva) {
                BotaoComando("Adiar", Icons.Rounded.Umbrella, pendentes.any { it is Comando.AdiarPorChuva }, onAdiar,
                    Modifier.weight(1f), EstiloBotao.ContornoNoDestaque, podeComandar, altura = 44.dp)
            }
        }
    }
}

@Composable
private fun Botoes(conteudo: @Composable RowScope.() -> Unit) {
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), content = conteudo)
}

/** Linha da sequência: feita (✓ lilás), regando (petróleo com número dourado) ou esperando. */
@Composable
private fun LinhaZona(
    zona: Zona,
    feita: Boolean,
    restanteS: Int?,
    enviando: Boolean,
    onAbrir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val d = Lotus.destaque
    val agora = zona.ligada
    Surface(
        onClick = onAbrir,
        shape = MaterialTheme.shapes.small,
        color = if (agora) d.fundo else Color.Transparent,
        contentColor = if (agora) d.texto else if (feita) cs.onSurfaceVariant else cs.onSurface,
        border = if (agora) BorderStroke(1.5.dp, d.dourado) else null,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp),
        ) {
            Box(
                Modifier
                    .size(26.dp)
                    .background(
                        when {
                            agora -> d.dourado
                            feita -> d.lilas
                            else -> cs.surfaceContainerHigh
                        },
                        MaterialTheme.shapes.extraSmall,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (feita) Icon(Icons.Rounded.Check, contentDescription = "Feita", tint = d.sobreLilas, modifier = Modifier.size(16.dp))
                else Text(
                    "${zona.numero}",
                    style = TextoMono.copy(fontWeight = FontWeight.SemiBold),
                    color = if (agora) d.sobreDourado else cs.primary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                zona.nome,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (agora) FontWeight.Bold else FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            when {
                enviando -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                agora && restanteS != null -> Text(relogio(restanteS), style = NumeroMedio.copy(fontSize = TextoMono.fontSize * 1.1f), color = d.rotulo)
                feita -> Text("feito", style = TextoMono, color = Lotus.estado.regando.cor)
                else -> Text("${(zona.duracaoPadraoS + 59) / 60} min", style = TextoMono, color = cs.onSurfaceVariant)
            }
        }
    }
}

/** Os quatro quadradinhos do canvas, em mono; o site CLP ganha pressão e vazão. */
@Composable
private fun Sensores(casa: Site, agora: Long, modifier: Modifier = Modifier) {
    val s = casa.sensores ?: return
    val e = Lotus.estado
    val cs = MaterialTheme.colorScheme
    val regando = casa.estado?.modo == Modo.REGANDO
    val proxima = casa.agenda?.let { proximaRega(it, emCasa(agora)) }
    val itens = buildList {
        add(Triple("CHUVA", if (s.chuva) "chovendo" else "seco", if (s.chuva) e.agua.texto else e.atencao.texto))
        // O contrato não publica a bomba: ela liga junto com qualquer zona aberta.
        add(Triple("BOMBA", if (regando) "ligada" else "parada", if (regando) e.ok.texto else cs.onSurfaceVariant))
        add(Triple("ÁGUA", if (s.nivel == Nivel.OK) "nível ok" else "baixa", if (s.nivel == Nivel.OK) e.regando.cor else cs.error))
        add(Triple("PRÓXIMO", proxima?.let { hora(it.hour * 60 + it.minute) } ?: "—", cs.onSurface))
        if (casa.info?.temPressao == true && s.pressaoKpa != null) add(Triple("PRESSÃO", "%.0f kPa".format(s.pressaoKpa), cs.onSurface))
        if (casa.info?.temVazao == true && s.vazaoLpm != null) add(Triple("VAZÃO", "%.1f L/min".format(s.vazaoLpm).replace('.', ','), cs.onSurface))
    }
    val porLinha = if (itens.size > 4) 3 else 4
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        itens.chunked(porLinha).forEach { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                linha.forEach { (rotulo, valor, cor) ->
                    Cartao(Modifier.weight(1f), forma = MaterialTheme.shapes.small) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(rotulo, style = RotuloMono, color = cs.onSurfaceVariant)
                            Text(
                                valor,
                                style = if (rotulo == "PRÓXIMO" || rotulo == "PRESSÃO" || rotulo == "VAZÃO") NumeroMedio.copy(fontSize = NumeroMedio.fontSize * 0.93f)
                                else MaterialTheme.typography.labelLarge.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize, fontWeight = FontWeight.Bold),
                                color = cor,
                                maxLines = 1,
                            )
                        }
                    }
                }
                repeat(porLinha - linha.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun EsqueletoZonas() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Esqueleto(Modifier.fillMaxWidth().height(160.dp), MaterialTheme.shapes.large)
        Esqueleto(Modifier.width(180.dp).height(22.dp))
        repeat(5) { Esqueleto(Modifier.fillMaxWidth().height(44.dp)) }
    }
}

@Composable
private fun PreviewZonas(casa: Site, pendentes: Set<Comando> = emptySet()) {
    LotusTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            ZonasTela(listOf(Exemplos.esp, Exemplos.clp), casa, pendentes, {}, {})
        }
    }
}

@PreviewLotus @Composable private fun ZonasRegandoPreview() = PreviewZonas(Exemplos.esp, setOf(Comando.Pausar))
@PreviewLotus @Composable private fun ZonasParadaPreview() = PreviewZonas(Exemplos.clp)
@PreviewLotus @Composable private fun ZonasPausadaPreview() = PreviewZonas(Exemplos.pausado)
@PreviewLotus @Composable private fun ZonasCarregandoPreview() = PreviewZonas(Exemplos.carregando)
@PreviewLotus @Composable private fun ZonasForaDoArPreview() = PreviewZonas(Exemplos.foraDoAr)
@PreviewLotus @Composable private fun ZonasSemAguaPreview() = PreviewZonas(Exemplos.semAgua)
@PreviewLotus @Composable private fun ZonasAdiadaPreview() = PreviewZonas(Exemplos.adiado)
@PreviewLotus @Composable private fun ZonasManualPreview() = PreviewZonas(Exemplos.manual)
