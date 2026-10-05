package com.lotus.ui.componentes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lotus.ui.Resumo
import com.lotus.ui.duracao
import com.lotus.ui.haQuanto
import com.lotus.ui.quando
import com.lotus.ui.tema.Lotus
import com.lotus.ui.tema.TomDeEstado
import com.lotus.ui.texto
import java.time.ZonedDateTime

/** A pílula que resume a casa. Não mostra nada enquanto carrega (o esqueleto cuida disso). */
@Composable
fun PilulaResumo(resumo: Resumo, modifier: Modifier = Modifier) {
    val e = Lotus.estado
    val erro = TomDeEstado(
        cor = MaterialTheme.colorScheme.error,
        fundo = MaterialTheme.colorScheme.errorContainer,
        texto = MaterialTheme.colorScheme.onErrorContainer,
    )
    when (resumo) {
        Resumo.Carregando -> Unit
        is Resumo.ForaDoAr -> Pilula("Fora do ar", Icons.Rounded.CloudOff, erro, modifier)
        is Resumo.ComFalha -> Pilula("Precisa de atenção", Icons.Rounded.Warning, erro, modifier)
        Resumo.Manual -> Pilula("Modo manual", Icons.Rounded.PanTool, e.atencao, modifier)
        is Resumo.Regando -> Pilula("Irrigando", Icons.Rounded.WaterDrop, e.regando, modifier)
        is Resumo.Pausado -> Pilula("Pausado", Icons.Rounded.Pause, e.atencao, modifier)
        is Resumo.AdiadoPorChuva -> Pilula("Adiado pela chuva", Icons.Rounded.Umbrella, e.agua, modifier)
        is Resumo.Parado ->
            if (resumo.chovendo) Pilula("Chovendo", Icons.Rounded.Umbrella, e.agua, modifier)
            else Pilula("Aguardando", Icons.Rounded.Schedule, e.agua, modifier, contorno = true)
    }
}

/** Uma frase sobre o momento da casa, para a tela inicial. */
fun Resumo.frase(agoraMs: Long, agora: ZonedDateTime): String = when (this) {
    Resumo.Carregando -> ""
    is Resumo.ForaDoAr ->
        if (vistoEm != null) "Sem sinal do quadro. Visto ${haQuanto(vistoEm, agoraMs)}." else "Sem sinal do quadro."
    is Resumo.ComFalha -> falha.texto().titulo + "."
    Resumo.Manual -> "Comandado pelo painel. O app só acompanha."
    is Resumo.Regando -> nomeZona ?: "Zona $zona"
    is Resumo.Pausado ->
        listOfNotNull(nomeZona ?: zona?.let { "Zona $it" }, restanteS?.let { "faltam ${duracao(it)}" })
            .joinToString(", ")
    is Resumo.AdiadoPorChuva -> "Agenda suspensa até ${quando(ate, agora)}."
    is Resumo.Parado -> when {
        proximaRega == null -> "Agenda desligada."
        chovendo -> "Chovendo agora. A rega de ${quando(proximaRega, agora)} será pulada se continuar."
        else -> "Próxima rega ${quando(proximaRega, agora)}."
    }
}
