package com.lotus.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    Zonas("Zonas", Icons.Rounded.GridView),
    Agenda("Agenda", Icons.Rounded.CalendarMonth),
}

/** Barra de baixo do canvas: ícone, nome e o tracinho dourado sob a aba aberta. */
@Composable
fun BarraNavegacao(atual: Aba, onEscolher: (Aba) -> Unit, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = modifier) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 6.dp, bottom = 8.dp).selectableGroup(),
            ) {
                Aba.entries.forEach { aba ->
                    val selecionada = aba == atual
                    val cor = if (selecionada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .width(80.dp)
                            .heightIn(min = 56.dp)
                            .selectable(selected = selecionada, onClick = { onEscolher(aba) }, role = Role.Tab)
                            .padding(top = 6.dp),
                    ) {
                        Icon(aba.icone, contentDescription = null, tint = cor)
                        Text(
                            aba.rotulo,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selecionada) FontWeight.Bold else FontWeight.SemiBold,
                            ),
                            color = cor,
                        )
                        Spacer(
                            Modifier
                                .width(18.dp)
                                .height(3.dp)
                                .background(
                                    if (selecionada) Lotus.destaque.dourado else MaterialTheme.colorScheme.surfaceContainerLowest,
                                    RoundedCornerShape(2.dp),
                                ),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Troca entre as duas casas. Cada uma leva um pontinho com a situação dela,
 * para ver a outra casa de relance sem trocar.
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
