package com.lotus.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lotus.BuildConfig
import com.lotus.dados.Nivel
import com.lotus.dados.Site
import com.lotus.dados.Zona
import com.lotus.ui.Exemplos
import com.lotus.ui.Resumo
import com.lotus.ui.componentes.BotaoComando
import com.lotus.ui.componentes.Cartao
import com.lotus.ui.componentes.CartaoDestaque
import com.lotus.ui.componentes.Esqueleto
import com.lotus.ui.componentes.FundoClima
import com.lotus.ui.componentes.GotaDaCaixa
import com.lotus.ui.componentes.fonteDoVidro
import com.lotus.ui.componentes.EstiloBotao
import com.lotus.ui.componentes.LogoPequeno
import com.lotus.ui.componentes.Pilula
import com.lotus.ui.componentes.PreviewLotus
import com.lotus.ui.componentes.TituloSecao
import com.lotus.ui.componentes.TracoDourado
import com.lotus.ui.componentes.agoraMs
import com.lotus.ui.duracao
import com.lotus.ui.emCasa
import com.lotus.ui.haQuanto
import com.lotus.ui.quandoCurto
import com.lotus.ui.relogio
import com.lotus.ui.resumo
import com.lotus.ui.saudacao
import com.lotus.ui.tempo
import com.lotus.ui.tema.Lotus
import com.lotus.ui.tema.LotusTheme
import com.lotus.ui.texto

/**
 * Início: a casa escolhida em destaque (próxima irrigação ou o que está regando),
 * chuva e caixa d'água, e as primeiras zonas.
 * Atrás de tudo, a animação do tempo ([FundoClima]).
 */
@Composable
fun InicioTela(
    casa: Site,
    onVerZonas: () -> Unit,
    modifier: Modifier = Modifier,
    margens: PaddingValues = PaddingValues(),
    escondidas: Set<Int> = emptySet(),
) {
    val agora = agoraMs()
    val resumo = resumo(casa, agora)
    // Fundo próprio: dá a cor de texto certa (onBackground) também fora do Scaffold.
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // Fica parado atrás da lista: o sol no canto, as nuvens no alto.
        FundoClima(tempo(casa), Modifier.fillMaxSize().fonteDoVidro())
        LazyColumn(
            contentPadding = PaddingValues(
                start = 24.dp, end = 24.dp,
                top = margens.calculateTopPadding() + 28.dp,
                bottom = margens.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { Cabecalho(agora) }
            // Espaço vazio onde ficava o seletor de casa: a troca de casa fica em Zonas e Agenda.
            item { Spacer(Modifier.height(44.dp)) }
            item { Destaque(casa, resumo, agora, onVerZonas) }
            if (casa.sensores != null) item { Sensores(casa) }
            if (casa.zonas.any { it.numero !in escondidas }) {
                item {
                    TituloSecao("Minhas áreas") {
                        TextButton(onClick = onVerZonas) { Text("Ver todas", style = MaterialTheme.typography.labelMedium) }
                    }
                }
                items(primeirasZonas(casa.zonas.filterNot { it.numero in escondidas }), key = { it.numero }) { zona ->
                    LinhaZona(zona, onClick = onVerZonas)
                }
            }
        }
    }
}

/** A zona regando primeiro, depois as outras, até três. */
private fun primeirasZonas(zonas: List<Zona>): List<Zona> =
    (zonas.filter { it.ligada } + zonas.filterNot { it.ligada }).take(3)

@Composable
private fun Cabecalho(agora: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LogoPequeno(38.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text("${saudacao(emCasa(agora))},", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(BuildConfig.NOME_USUARIO, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Cartão petróleo do canvas, com o texto certo para cada situação da casa. */
@Composable
private fun Destaque(casa: Site, resumo: Resumo, agora: Long, onVerZonas: () -> Unit) {
    val d = Lotus.destaque
    val hoje = emCasa(agora)
    val (rotulo, titulo, detalhe) = when (resumo) {
        Resumo.Carregando -> Triple("", "", "")
        is Resumo.Regando -> Triple(
            "Regando agora",
            resumo.nomeZona ?: "Área ${resumo.zona}",
            "Área ${resumo.zona}" + (resumo.restanteS?.let { " · faltam ${relogio(it)}" } ?: ""),
        )
        is Resumo.Pausado -> Triple(
            "Pausado",
            resumo.nomeZona ?: "Área ${resumo.zona}",
            resumo.restanteS?.let { "Faltavam ${duracao(it)}" } ?: "",
        )
        is Resumo.Parado -> when (val p = resumo.proximaRega) {
            null -> Triple("Rega automática desligada", "Sem agenda", "Só rega quando você pedir.")
            else -> Triple(
                "Próxima irrigação",
                quandoCurto(p, hoje),
                "Todas as ${casa.zonas.size} áreas · cerca de ${duracao(casa.zonas.sumOf { it.duracaoPadraoS })}" +
                    if (resumo.chovendo) "\nChovendo agora: pode ser pulada." else "",
            )
        }
        is Resumo.AdiadoPorChuva -> Triple(
            "Adiado pela chuva",
            "Até ${quandoCurto(resumo.ate, hoje).replaceFirstChar { it.lowercase() }}",
            "Nenhuma rega automática até lá.",
        )
        Resumo.Manual -> Triple("Modo manual", "No painel", "Alguém está comandando pelo quadro. O app só acompanha.")
        is Resumo.ComFalha -> resumo.falha.texto().let { Triple("Precisa de atenção", it.titulo, it.oQueFazer) }
        is Resumo.ForaDoAr -> Triple(
            "Sem sinal",
            "Fora do ar",
            (resumo.vistoEm?.let { "Visto ${haQuanto(it, agora)}. " } ?: "") + "A agenda continua rodando no quadro.",
        )
    }
    CartaoDestaque {
        if (resumo == Resumo.Carregando) {
            Esqueleto(Modifier.width(140.dp).height(16.dp))
            Spacer(Modifier.height(12.dp))
            Esqueleto(Modifier.width(200.dp).height(32.dp))
            Spacer(Modifier.height(12.dp))
            Esqueleto(Modifier.width(160.dp).height(16.dp))
            return@CartaoDestaque
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(0.66f)) {
            Text(rotulo, style = MaterialTheme.typography.labelMedium, color = d.rotulo)
            Text(titulo, style = MaterialTheme.typography.headlineMedium, color = d.texto)
            if (detalhe.isNotEmpty()) Text(detalhe, style = MaterialTheme.typography.bodyMedium, color = d.textoSuave)
            TracoDourado(largura = 72.dp)
        }
        Spacer(Modifier.height(16.dp))
        BotaoComando(
            texto = "Ver áreas",
            icone = null,
            enviando = false,
            onClick = onVerZonas,
            estilo = EstiloBotao.Dourado,
            altura = 44.dp,
        )
    }
}

/** Os dois cartões pequenos do canvas, com o que o quadro mede de verdade. */
@Composable
private fun Sensores(casa: Site) {
    val s = casa.sensores ?: return
    val e = Lotus.estado
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Medida("Chuva", if (s.chuva) "Chovendo" else "Seco", Modifier.weight(1f)) {
            Icon(
                if (s.chuva) Icons.Rounded.Umbrella else Icons.Rounded.WbSunny,
                contentDescription = null,
                tint = if (s.chuva) e.agua.cor else Lotus.destaque.dourado,
                modifier = Modifier.size(18.dp),
            )
        }
        val comAgua = s.nivel == Nivel.OK
        Medida("Caixa d'água", if (comAgua) "Com água" else "Baixa", Modifier.weight(1f)) {
            GotaDaCaixa(comAgua, if (comAgua) Lotus.destaque.lilas else MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun Medida(nome: String, valor: String, modifier: Modifier, icone: @Composable () -> Unit) {
    Cartao(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icone()
                Spacer(Modifier.width(8.dp))
                Text(nome, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(valor, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun LinhaZona(zona: Zona, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Cartao(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 80.dp).padding(14.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (zona.ligada) cs.primary else cs.secondaryContainer,
                contentColor = if (zona.ligada) cs.onPrimary else cs.primary,
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.WaterDrop, contentDescription = null) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(zona.nome, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "Área ${zona.numero} · ${duracao(zona.duracaoPadraoS)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            // Na lista o texto basta (o ícone da gota já está à esquerda), como no canvas.
            if (zona.ligada) Pilula("Irrigando", null, Lotus.estado.regando)
            else Pilula("Aguardando", null, Lotus.estado.agua, contorno = true)
        }
    }
}

@PreviewLotus
@Composable
private fun InicioRegandoPreview() {
    LotusTheme { InicioTela(Exemplos.esp, {}) }
}

@PreviewLotus
@Composable
private fun InicioParadaPreview() {
    LotusTheme { InicioTela(Exemplos.clp, {}) }
}

@PreviewLotus
@Composable
private fun InicioChuvaPreview() {
    LotusTheme { InicioTela(Exemplos.chuva, {}) }
}

@PreviewLotus
@Composable
private fun InicioForaDoArPreview() {
    LotusTheme { InicioTela(Exemplos.foraDoAr, {}) }
}
