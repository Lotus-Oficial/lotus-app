package com.lotus

import com.lotus.dados.Agenda
import com.lotus.ui.FusoDaCasa
import com.lotus.ui.dias
import com.lotus.ui.duracao
import com.lotus.ui.haQuanto
import com.lotus.ui.hora
import com.lotus.ui.proximaRega
import com.lotus.ui.quando
import com.lotus.ui.quandoCurto
import com.lotus.ui.saudacao
import com.lotus.ui.relogio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZonedDateTime

class FormatosTest {

    @Test fun duracaoLegivel() {
        assertEquals("6 min 52 s", duracao(412))
        assertEquals("10 min", duracao(600))
        assertEquals("45 s", duracao(45))
        assertEquals("1 h 30 min", duracao(5400))
        assertEquals("2 h", duracao(7200))
    }

    @Test fun relogioDaContagem() {
        assertEquals("6:52", relogio(412))
        assertEquals("0:05", relogio(5))
        assertEquals("1:02:05", relogio(3725))
    }

    @Test fun horaDaAgenda() {
        assertEquals("06:00", hora(360))
        assertEquals("18:30", hora(1110))
    }

    @Test fun diasDaSemana() {
        assertEquals("todos os dias", dias(127))
        assertEquals("seg a sex", dias(62))
        assertEquals("fins de semana", dias(65))
        assertEquals("nenhum dia", dias(0))
        assertEquals("seg, qua, sex", dias(0b0101010))
        assertEquals("ter, dom", dias(0b0000101))
    }

    @Test fun haQuantoTempo() {
        assertEquals("agora há pouco", haQuanto(0, 30_000))
        assertEquals("há 5 min", haQuanto(0, 5 * 60_000))
        assertEquals("há 2 h", haQuanto(0, 2 * 3_600_000))
    }

    // Domingo, 4 de outubro de 2026, 10:00 em São Paulo.
    private val domingo10h = ZonedDateTime.of(2026, 10, 4, 10, 0, 0, 0, FusoDaCasa)

    @Test fun proximaRegaNoMesmoDia() {
        val agenda = Agenda(true, 127, listOf(1080, 360))
        val proxima = proximaRega(agenda, domingo10h)!!
        assertEquals("hoje às 18:00", quando(proxima, domingo10h))
    }

    @Test fun proximaRegaPulaDiasForaDaAgenda() {
        val segASex = Agenda(true, 62, listOf(360))
        assertEquals("amanhã às 06:00", quando(proximaRega(segASex, domingo10h)!!, domingo10h))
        val soQuarta = Agenda(true, 0b0001000, listOf(360))
        assertEquals("quarta às 06:00", quando(proximaRega(soQuarta, domingo10h)!!, domingo10h))
    }

    @Test fun quandoCurtoParaTitulos() {
        assertEquals("Hoje, 18:00", quandoCurto(domingo10h.withHour(18), domingo10h))
        assertEquals("Amanhã, 06:00", quandoCurto(domingo10h.plusDays(1).withHour(6), domingo10h))
        assertEquals("Quarta, 06:00", quandoCurto(domingo10h.plusDays(3).withHour(6), domingo10h))
    }

    @Test fun saudacaoPelaHora() {
        assertEquals("Bom dia", saudacao(domingo10h))
        assertEquals("Boa tarde", saudacao(domingo10h.withHour(15)))
        assertEquals("Boa noite", saudacao(domingo10h.withHour(22)))
    }

    @Test fun agendaDesligadaNaoTemProximaRega() {
        assertNull(proximaRega(Agenda(false, 127, listOf(360)), domingo10h))
        assertNull(proximaRega(Agenda(true, 127, emptyList()), domingo10h))
        assertNull(proximaRega(Agenda(true, 0, listOf(360)), domingo10h))
    }
}
