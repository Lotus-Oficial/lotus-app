package com.lotus.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lotus.dados.Site
import com.lotus.ui.Resumo
import com.lotus.ui.resumo
import com.lotus.ui.tema.Lotus

enum class Aba(val rotulo: String, val icone: ImageVector) {
    Inicio("Início", Icons.Rounded.Home),
    Zonas("Áreas", Icons.Rounded.GridView),
    Agenda("Agenda", Icons.Rounded.CalendarMonth),
}

/** Espaço que as telas deixam embaixo para o conteúdo não terminar escondido atrás da barra. */
val EspacoDaBarra = 70.dp + 22.dp + 16.dp

/**
 * Barra de baixo Liquid Glass do canvas: pílula de vidro flutuando sobre o conteúdo,
 * com a aba aberta numa pílula petróleo.
 */
@Composable
fun BarraNavegacao(atual: Aba, onEscolher: (Aba) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .padding(start = 18.dp, end = 18.dp, bottom = 22.dp)
            .fillMaxWidth()
            .height(70.dp)
            .vidro(RoundedCornerShape(35.dp), Lotus.vidro.barra, opaco = true)
            .padding(6.dp)
            .selectableGroup(),
    ) {
        Aba.entries.forEach { aba ->
            val selecionada = aba == atual
            val forma = RoundedCornerShape(29.dp)
            val cor = if (selecionada) cs.onPrimary else cs.primary
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(forma)
                    .then(if (selecionada) Modifier.background(cs.primary).bordaDeVidro(forma) else Modifier)
                    .selectable(selected = selecionada, onClick = { onEscolher(aba) }, role = Role.Tab),
            ) {
                Icon(aba.icone, contentDescription = null, tint = cor, modifier = Modifier.size(22.dp))
                Text(
                    aba.rotulo,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (selecionada) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                    color = cor,
                )
            }
        }
    }
}

/**
 * Troca entre as casas do usuário. Cada uma leva um pontinho com a situação dela,
 * para ver a outra casa de relance sem trocar. Com uma casa só, vira o nome e a situação dela.
 */
@Composable
fun SeletorCasa(sites: List<Site>, atual: Site, agora: Long, onEscolher: (Site) -> Unit, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth().selectableGroup()) {
        sites.forEach { site ->
            val selecionada = site.id == atual.id
            val cs = MaterialTheme.colorScheme
            Surface(
                shape = CircleShape,
                color = if (selecionada) cs.primary else cs.surfaceContainerLowest,
                contentColor = if (selecionada) cs.onPrimary else cs.primary,
                border = if (selecionada) null else BorderStroke(1.5.dp, cs.outlineVariant),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .selectable(selected = selecionada, onClick = { onEscolher(site) }, role = Role.Tab),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    PontoDeSituacao(resumo(site, agora))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        site.nome,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Pontinho de 8 dp: lilás regando, dourado pedindo atenção, vermelho com problema, verde ok. */
@Composable
fun PontoDeSituacao(resumo: Resumo, modifier: Modifier = Modifier) {
    val e = Lotus.estado
    val cor = when (resumo) {
        is Resumo.Regando -> Lotus.destaque.lilas
        is Resumo.Pausado, Resumo.Manual, is Resumo.AdiadoPorChuva -> Lotus.destaque.dourado
        is Resumo.ForaDoAr, is Resumo.ComFalha -> MaterialTheme.colorScheme.error
        is Resumo.Parado -> e.ok.cor
        Resumo.Carregando -> MaterialTheme.colorScheme.outline
    }
    Spacer(modifier.size(8.dp).background(cor, CircleShape))
}
