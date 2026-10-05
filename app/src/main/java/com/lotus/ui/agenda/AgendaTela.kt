package com.lotus.ui.agenda

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lotus.dados.Agenda
import com.lotus.dados.Site
import com.lotus.ui.Exemplos
import com.lotus.ui.componentes.BotaoComando
import com.lotus.ui.componentes.Cartao
import com.lotus.ui.componentes.FaixaForaDoAr
import com.lotus.ui.componentes.PreviewLotus
import com.lotus.ui.componentes.SeletorCasa
import com.lotus.ui.componentes.TituloSecao
import com.lotus.ui.componentes.TracoDourado
import com.lotus.ui.componentes.agoraMs
import com.lotus.ui.dias
import com.lotus.ui.duracao
import com.lotus.ui.hora
import com.lotus.ui.tema.Lotus
import com.lotus.ui.tema.LotusTheme
import com.lotus.ui.tema.NumeroMedio
import com.lotus.ui.tema.TextoMono

// Bit 0 = domingo; a tela mostra a semana começando na segunda.
private val Semana = listOf(1 to "Segunda", 2 to "Terça", 3 to "Quarta", 4 to "Quinta", 5 to "Sexta", 6 to "Sábado", 0 to "Domingo")

/**
 * Aba Agenda. Edita um rascunho local e só manda `set_schedule` ao salvar,
 * porque o comando substitui a agenda inteira.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaTela(
    sites: List<Site>,
    casa: Site,
    salvando: Boolean,
    onEscolherCasa: (Site) -> Unit,
    onSalvar: (Agenda) -> Unit,
    modifier: Modifier = Modifier,
    margens: PaddingValues = PaddingValues(),
) {
    val agora = agoraMs()
    val original = casa.agenda ?: Agenda(false, Agenda.TODOS_OS_DIAS, emptyList())
    // O rascunho recomeça ao trocar de casa ou quando a agenda do quadro muda.
    var ativa by rememberSaveable(casa.id, original) { mutableStateOf(original.ativa) }
    var mascara by rememberSaveable(casa.id, original) { mutableStateOf(original.dias) }
    var inicios by rememberSaveable(casa.id, original) { mutableStateOf(original.inicios.sorted()) }
    var escolhendoHora by rememberSaveable { mutableStateOf(false) }

    val rascunho = Agenda(ativa, mascara, inicios)
    val mudou = rascunho != original.copy(inicios = original.inicios.sorted())
    val foraDoAr = casa.online == false
    val cicloS = casa.zonas.sumOf { it.duracaoPadraoS }

    // Fundo próprio: dá a cor de texto certa (onBackground) também fora do Scaffold.
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 24.dp, end = 24.dp,
                top = margens.calculateTopPadding() + 28.dp,
                bottom = margens.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Agenda", style = MaterialTheme.typography.headlineMedium)
                    TracoDourado()
                }
            }
            item { SeletorCasa(sites, casa, agora, onEscolherCasa) }
            if (foraDoAr) item { FaixaForaDoAr(casa.vistoEm) }

            item {
                Cartao(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Rega automática", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (ativa) "O quadro rega sozinho nos horários abaixo."
                                else "Desligada. Só rega quando você pedir.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = ativa, onCheckedChange = { ativa = it })
                    }
                }
            }

            item { TituloSecao("Dias", Modifier.padding(top = 6.dp)) }
            item {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Semana.forEach { (bit, nome) ->
                        Dia(nome, mascara and (1 shl bit) != 0, onClick = { mascara = mascara xor (1 shl bit) })
                    }
                }
                Text(
                    dias(mascara).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            item {
                TituloSecao("Horários", Modifier.padding(top = 6.dp)) {
                    Text("${inicios.size}/${Agenda.MAX_INICIOS}", style = TextoMono, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (inicios.isEmpty()) {
                item {
                    Text(
                        "Nenhum horário ainda. Adicione pelo menos um para o quintal se regar sozinho.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(inicios, key = { it }) { inicio ->
                Cartao(Modifier.fillMaxWidth(), forma = MaterialTheme.shapes.small) {
                    Row(
                        Modifier.heightIn(min = 60.dp).padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(14.dp))
                        Text(hora(inicio), style = NumeroMedio.copy(fontSize = NumeroMedio.fontSize * 1.3f), modifier = Modifier.weight(1f))
                        IconButton(onClick = { inicios = inicios - inicio }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Tirar o horário das ${hora(inicio)}")
                        }
                    }
                }
            }
            if (inicios.size < Agenda.MAX_INICIOS) {
                item { BotaoTracejado("Adicionar horário", onClick = { escolhendoHora = true }) }
            }
            if (cicloS > 0) {
                item {
                    Text(
                        "Cada rega passa pelas ${casa.zonas.size} zonas, uma depois da outra: " +
                            "cerca de ${duracao(cicloS)} no total. Ela é pulada se estiver chovendo ou a caixa estiver baixa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                BotaoComando(
                    texto = if (mudou || salvando) "Salvar agenda" else "Agenda salva",
                    icone = Icons.Rounded.Check,
                    enviando = salvando,
                    onClick = { onSalvar(rascunho) },
                    habilitado = mudou && !foraDoAr,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }

    if (escolhendoHora) {
        val estado = rememberTimePickerState(initialHour = 6, initialMinute = 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { escolhendoHora = false },
            title = { Text("Novo horário") },
            text = { TimePicker(estado) },
            confirmButton = {
                TextButton(onClick = {
                    val min = estado.hour * 60 + estado.minute
                    if (min !in inicios) inicios = (inicios + min).sorted()
                    escolhendoHora = false
                }) { Text("Adicionar") }
            },
            dismissButton = { TextButton(onClick = { escolhendoHora = false }) { Text("Cancelar") } },
        )
    }
}

/** O botão tracejado dourado do canvas ("Adicionar zona"), para acrescentar algo a uma lista. */
@Composable
private fun BotaoTracejado(texto: String, onClick: () -> Unit) {
    val d = Lotus.destaque
    val cor = MaterialTheme.colorScheme.onSecondaryContainer
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = d.dourado.copy(alpha = 0.12f),
        contentColor = cor,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .drawBehind {
                drawRoundRect(
                    color = d.dourado,
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                )
            },
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Dia da semana como um círculo: lilás quando a agenda roda nele. */
@Composable
private fun Dia(nome: String, marcado: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (marcado) cs.tertiary else cs.surfaceContainerLowest,
        contentColor = if (marcado) cs.onTertiary else cs.onSurfaceVariant,
        border = if (marcado) null else BorderStroke(1.5.dp, cs.outlineVariant),
        modifier = Modifier.size(42.dp).semantics {
            contentDescription = nome
            selected = marcado
            role = Role.Checkbox
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(nome.take(1), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@PreviewLotus
@Composable
private fun AgendaPreview() {
    LotusTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            AgendaTela(listOf(Exemplos.esp, Exemplos.clp), Exemplos.esp, false, {}, {})
        }
    }
}

@PreviewLotus
@Composable
private fun AgendaVaziaPreview() {
    LotusTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            AgendaTela(listOf(Exemplos.esp, Exemplos.agendaVazia), Exemplos.agendaVazia, false, {}, {})
        }
    }
}
