package com.lotus.ui

import com.lotus.dados.Agenda
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Fuso da casa: os horários da agenda vêm em minutos desde a meia-noite neste fuso. */
val FusoDaCasa: ZoneId = ZoneId.of("America/Sao_Paulo")

private val DiasCurtos = listOf("dom", "seg", "ter", "qua", "qui", "sex", "sáb")
private val DiasLongos = listOf("domingo", "segunda", "terça", "quarta", "quinta", "sexta", "sábado")

/** 412 → "6 min 52 s"; 600 → "10 min"; 5400 → "1 h 30 min". */
fun duracao(segundos: Int): String {
    val s = segundos.coerceAtLeast(0)
    val h = s / 3600
    val m = s % 3600 / 60
    val seg = s % 60
    return when {
        h > 0 -> if (m == 0) "$h h" else "$h h $m min"
        m > 0 -> if (seg == 0) "$m min" else "$m min $seg s"
        else -> "$seg s"
    }
}

/** Contagem regressiva: 412 → "6:52"; 3725 → "1:02:05". */
fun relogio(segundos: Int): String {
    val s = segundos.coerceAtLeast(0)
    val h = s / 3600
    val m = s % 3600 / 60
    val seg = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, seg) else "%d:%02d".format(m, seg)
}

/** 360 → "06:00". */
fun hora(minutosDoDia: Int): String = "%02d:%02d".format(minutosDoDia / 60, minutosDoDia % 60)

/** Bitmask de dias (bit 0 = domingo): 127 → "todos os dias", 62 → "seg a sex". */
fun dias(mascara: Int): String {
    val m = mascara and Agenda.TODOS_OS_DIAS
    return when (m) {
        0 -> "nenhum dia"
        Agenda.TODOS_OS_DIAS -> "todos os dias"
        0b0111110 -> "seg a sex"
        0b1000001 -> "fins de semana"
        else -> (1..7).map { it % 7 } // começa na segunda
            .filter { m and (1 shl it) != 0 }
            .joinToString(", ") { DiasCurtos[it] }
    }
}

/** Há quanto tempo, para "visto há 5 min". */
fun haQuanto(desdeMs: Long, agoraMs: Long): String {
    val min = (agoraMs - desdeMs) / 60_000
    return when {
        min < 1 -> "agora há pouco"
        min < 60 -> "há $min min"
        min < 24 * 60 -> "há ${min / 60} h"
        else -> "há ${min / (24 * 60)} dias"
    }
}

/** Próximo início da agenda a partir de [agora], ou null se ela não vai rodar. */
fun proximaRega(agenda: Agenda, agora: ZonedDateTime): ZonedDateTime? {
    if (!agenda.ativa || agenda.inicios.isEmpty() || agenda.dias and Agenda.TODOS_OS_DIAS == 0) return null
    val hoje = agora.truncatedTo(ChronoUnit.DAYS)
    for (d in 0L..7L) {
        val dia = hoje.plusDays(d)
        val bit = dia.dayOfWeek.value % 7 // segunda = 1 ... domingo = 0
        if (agenda.dias and (1 shl bit) == 0) continue
        agenda.inicios.sorted()
            .map { dia.plusMinutes(it.toLong()) }
            .firstOrNull { it.isAfter(agora) }
            ?.let { return it }
    }
    return null
}

/** "hoje às 18:00", "amanhã às 06:00", "quarta às 06:00". */
fun quando(momento: ZonedDateTime, agora: ZonedDateTime): String {
    val dias = ChronoUnit.DAYS.between(agora.toLocalDate(), momento.toLocalDate())
    val h = hora(momento.hour * 60 + momento.minute)
    return when (dias) {
        0L -> "hoje às $h"
        1L -> "amanhã às $h"
        else -> "${DiasLongos[momento.dayOfWeek.value % 7]} às $h"
    }
}

fun emCasa(epochMs: Long): ZonedDateTime = Instant.ofEpochMilli(epochMs).atZone(FusoDaCasa)

/** Para títulos: "Hoje, 18:00", "Amanhã, 06:00", "Quarta, 06:00". */
fun quandoCurto(momento: ZonedDateTime, agora: ZonedDateTime): String {
    val dias = ChronoUnit.DAYS.between(agora.toLocalDate(), momento.toLocalDate())
    val h = hora(momento.hour * 60 + momento.minute)
    val dia = when (dias) {
        0L -> "Hoje"
        1L -> "Amanhã"
        else -> DiasLongos[momento.dayOfWeek.value % 7].replaceFirstChar { it.uppercase() }
    }
    return "$dia, $h"
}

/** "Bom dia", "Boa tarde" ou "Boa noite", pela hora da casa. */
fun saudacao(agora: ZonedDateTime): String = when (agora.hour) {
    in 5..11 -> "Bom dia"
    in 12..17 -> "Boa tarde"
    else -> "Boa noite"
}
