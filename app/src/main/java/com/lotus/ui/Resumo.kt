package com.lotus.ui

import com.lotus.dados.Falha
import com.lotus.dados.Modo
import com.lotus.dados.Site
import java.time.ZonedDateTime

/** A situação de uma casa numa frase: é o que aparece primeiro na tela inicial e na da casa. */
sealed interface Resumo {
    data object Carregando : Resumo
    data class ForaDoAr(val vistoEm: Long?) : Resumo
    data class ComFalha(val falha: Falha) : Resumo
    data object Manual : Resumo
    data class Regando(val zona: Int, val nomeZona: String?, val restanteS: Int?, val proximaZona: Int?) : Resumo
    data class Pausado(val zona: Int?, val nomeZona: String?, val restanteS: Int?) : Resumo
    data class AdiadoPorChuva(val ate: ZonedDateTime) : Resumo
    data class Parado(val proximaRega: ZonedDateTime?, val chovendo: Boolean) : Resumo
}

/**
 * Ordem de prioridade: sem dados, fora do ar, falha, manual, depois o modo.
 * [agoraMs] também desconta o tempo desde o último `state` (contagem local).
 */
fun resumo(site: Site, agoraMs: Long): Resumo {
    val estado = site.estado
    if (site.online == false) return Resumo.ForaDoAr(site.vistoEm)
    if (estado == null || !site.carregado) return Resumo.Carregando
    estado.falha?.let { return Resumo.ComFalha(it) }

    val restante = estado.restanteS?.let { r ->
        if (estado.modo == Modo.REGANDO) {
            val passou = ((agoraMs - (site.estadoRecebidoEm ?: agoraMs)) / 1000).toInt()
            (r - passou).coerceAtLeast(0)
        } else r
    }
    val agora = emCasa(agoraMs)
    return when (estado.modo) {
        Modo.MANUAL -> Resumo.Manual
        Modo.REGANDO -> Resumo.Regando(
            zona = estado.zonaAtiva ?: 0,
            nomeZona = site.zona(estado.zonaAtiva)?.nome,
            restanteS = restante,
            proximaZona = estado.proximaZona,
        )
        Modo.PAUSADO -> Resumo.Pausado(estado.zonaAtiva, site.zona(estado.zonaAtiva)?.nome, restante)
        Modo.PARADO -> {
            val chuvaAte = estado.chuvaAte?.let { emCasa(it * 1000) }?.takeIf { it.isAfter(agora) }
            if (chuvaAte != null) Resumo.AdiadoPorChuva(chuvaAte)
            else Resumo.Parado(
                proximaRega = site.agenda?.let { proximaRega(it, agora) },
                chovendo = site.sensores?.chuva == true,
            )
        }
    }
}
