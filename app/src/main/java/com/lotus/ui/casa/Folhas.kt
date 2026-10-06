package com.lotus.ui.casa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lotus.dados.Comando
import com.lotus.dados.Zona
import com.lotus.ui.componentes.BotaoComando
import com.lotus.ui.componentes.EstiloBotao
import com.lotus.ui.componentes.TracoDourado
import com.lotus.ui.duracao
import com.lotus.ui.tema.NumeroMedio

private val OpcoesDeTempoMin = listOf(5, 10, 15, 30)

/** Folha de uma zona: regar agora por um tempo e mudar o tempo padrão dela no ciclo. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ZonaFolha(
    zona: Zona,
    habilitado: Boolean,
    podeRegar: Boolean,
    pendentes: Set<Comando>,
    onRegar: (duracaoS: Int?) -> Unit,
    onSalvarPadrao: (duracaoS: Int) -> Unit,
    onFechar: () -> Unit,
) {
    // null = tempo padrão da zona (o quadro decide, sem mandar durationS)
    var tempoMin by rememberSaveable { mutableStateOf<Int?>(null) }
    var padraoMin by rememberSaveable(zona.duracaoPadraoS) { mutableIntStateOf((zona.duracaoPadraoS + 59) / 60) }
    val salvando = pendentes.any { it is Comando.MudarZona && it.zona == zona.numero }
    val mudouPadrao = padraoMin * 60 != zona.duracaoPadraoS

    ModalBottomSheet(onDismissRequest = onFechar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            Text("Área ${zona.numero}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(zona.nome, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            TracoDourado(largura = 56.dp)

            Spacer(Modifier.height(24.dp))
            Text("Regar agora", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = tempoMin == null,
                    onClick = { tempoMin = null },
                    label = { Text("Padrão · ${duracao(zona.duracaoPadraoS)}") },
                )
                OpcoesDeTempoMin.forEach { min ->
                    FilterChip(selected = tempoMin == min, onClick = { tempoMin = min }, label = { Text("$min min") })
                }
            }
            Spacer(Modifier.height(12.dp))
            BotaoComando(
                texto = "Regar por ${duracao(tempoMin?.times(60) ?: zona.duracaoPadraoS)}",
                icone = Icons.Rounded.WaterDrop,
                enviando = false,
                onClick = { onRegar(tempoMin?.times(60)) },
                habilitado = podeRegar && !zona.ligada,
                modifier = Modifier.fillMaxWidth(),
            )
            if (zona.ligada) {
                Text(
                    "Esta área já está regando.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 24.dp))

            Text("Tempo no ciclo", style = MaterialTheme.typography.titleMedium)
            Text(
                "Quanto esta área rega quando a agenda roda ou você pede para regar tudo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = { padraoMin-- }, enabled = padraoMin > 1) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Menos um minuto")
                }
                Text(
                    "$padraoMin min",
                    style = NumeroMedio.copy(fontSize = NumeroMedio.fontSize * 1.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalIconButton(onClick = { padraoMin++ }, enabled = padraoMin < 120) {
                    Icon(Icons.Rounded.Add, contentDescription = "Mais um minuto")
                }
            }
            Spacer(Modifier.height(12.dp))
            BotaoComando(
                texto = if (mudouPadrao || salvando) "Salvar tempo" else "Tempo salvo",
                icone = Icons.Rounded.Check,
                enviando = salvando,
                onClick = { onSalvarPadrao(padraoMin * 60) },
                estilo = EstiloBotao.Contorno,
                habilitado = habilitado && mudouPadrao,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val OpcoesDeAdiamento = listOf(
    24 to "Até amanhã",
    48 to "Por 2 dias",
    72 to "Por 3 dias",
    168 to "Por 1 semana",
)

/** Folha de adiar a agenda por causa da chuva (`rain_delay`). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdiarFolha(onAdiar: (horas: Int) -> Unit, onFechar: () -> Unit) {
    var horas by rememberSaveable { mutableIntStateOf(24) }
    ModalBottomSheet(onDismissRequest = onFechar) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            Text("Adiar por chuva", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            TracoDourado(largura = 56.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                "A agenda fica suspensa. Você ainda pode regar na mão quando quiser.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Column(Modifier.selectableGroup()) {
                OpcoesDeAdiamento.forEach { (h, texto) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .selectable(selected = horas == h, onClick = { horas = h }, role = Role.RadioButton),
                    ) {
                        RadioButton(selected = horas == h, onClick = null)
                        Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            BotaoComando(
                texto = "Adiar",
                icone = Icons.Rounded.Umbrella,
                enviando = false,
                onClick = { onAdiar(horas) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
