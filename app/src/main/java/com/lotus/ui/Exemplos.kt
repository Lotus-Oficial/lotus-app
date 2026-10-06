package com.lotus.ui

import com.lotus.dados.Agenda
import com.lotus.dados.EstadoQuadro
import com.lotus.dados.Falha
import com.lotus.dados.InfoQuadro
import com.lotus.dados.Modo
import com.lotus.dados.Nivel
import com.lotus.dados.Sensores
import com.lotus.dados.Site
import com.lotus.dados.SiteId
import com.lotus.dados.Zona

/** Casas de exemplo para os @Preview, uma por estado que as telas precisam cobrir. */
object Exemplos {
    private val agora = System.currentTimeMillis()

    private val parado = EstadoQuadro(Modo.PARADO, null, null, null, null, null, agora / 1000)

    val esp = Site(
        id = SiteId.ESP,
        nome = SiteId.ESP.casa,
        online = true,
        vistoEm = agora,
        info = InfoQuadro("esp32-relay", "0.1.0", 7, temPressao = false, temVazao = false, temBotoes = true),
        estado = EstadoQuadro(Modo.REGANDO, 3, 412, 4, null, null, agora / 1000),
        estadoRecebidoEm = agora,
        zonas = listOf(
            "Gramado da frente", "Canteiro de ervas", "Horta", "Jardim lateral",
            "Fundos", "Vasos da varanda", "Pomar",
        ).mapIndexed { i, n -> Zona(i + 1, ligada = i == 2, nome = n, duracaoPadraoS = (8 + i * 2) * 60) },
        sensores = Sensores(chuva = false, nivel = Nivel.OK, pressaoKpa = null, vazaoLpm = null),
        agenda = Agenda(true, 127, listOf(360, 1080)),
    )

    val clp = Site(
        id = SiteId.CLP,
        nome = SiteId.CLP.casa,
        online = true,
        vistoEm = agora,
        info = InfoQuadro("plc-delta", "0.1.0", 5, temPressao = true, temVazao = true, temBotoes = true),
        estado = parado,
        estadoRecebidoEm = agora,
        zonas = listOf("Gramado", "Cerca viva", "Canteiro central", "Horta", "Árvores do fundo")
            .mapIndexed { i, n -> Zona(i + 1, false, n, 15 * 60) },
        sensores = Sensores(chuva = false, nivel = Nivel.OK, pressaoKpa = 0.0, vazaoLpm = 0.0),
        agenda = Agenda(true, 62, listOf(330)),
    )

    val carregando = clp.copy(online = null, info = null, estado = null, zonas = emptyList(), sensores = null, agenda = null)
    val foraDoAr = esp.copy(online = false, vistoEm = agora - 23 * 60_000)
    val semAgua = clp.copy(
        estado = parado.copy(falha = Falha.SECO),
        sensores = clp.sensores!!.copy(nivel = Nivel.BAIXO),
    )
    val pausado = esp.copy(estado = esp.estado!!.copy(modo = Modo.PAUSADO), zonas = esp.zonas.map { it.copy(ligada = false) })
    val adiado = clp.copy(estado = parado.copy(chuvaAte = agora / 1000 + 48 * 3600))
    val manual = clp.copy(estado = parado.copy(modo = Modo.MANUAL))
    val chuva = clp.copy(sensores = clp.sensores!!.copy(chuva = true))
    val agendaVazia = clp.copy(agenda = Agenda(false, 127, emptyList()))
}
