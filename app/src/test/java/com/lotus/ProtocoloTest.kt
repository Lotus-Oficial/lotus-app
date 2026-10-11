package com.lotus

import com.lotus.dados.Agenda
import com.lotus.dados.Comando
import com.lotus.dados.ErroComando
import com.lotus.dados.Falha
import com.lotus.dados.Modo
import com.lotus.dados.Nivel
import com.lotus.dados.Resposta
import com.lotus.dados.Site
import com.lotus.dados.SiteId
import com.lotus.dados.aplicar
import com.lotus.dados.json
import com.lotus.dados.lerAck
import com.lotus.dados.siteDoTopico
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exemplos tirados de lotus-bkd/docs/CONTRATO-MQTT.md. */
class ProtocoloTest {

    private val vazio = Site(
        id = SiteId.ESP, nome = SiteId.ESP.casa, online = null, vistoEm = null, info = null, estado = null,
        estadoRecebidoEm = null, zonas = emptyList(), sensores = null, agenda = null,
    )

    private fun Site.msg(resto: String, payload: String, agora: Long = 1_000) =
        aplicar("lotus/v1/esp/$resto", payload, agora)

    @Test fun topicoDoSite() {
        assertEquals(SiteId.CLP, siteDoTopico("lotus/v1/clp/state"))
        assertNull(siteDoTopico("lotus/v2/esp/state"))
        assertNull(siteDoTopico("lotus/v1/xyz/state"))
    }

    @Test fun statusEInfo() {
        val s = vazio.msg("status", "online")
            .msg("info", """{"type":"esp32-relay","fw":"0.1.0","zones":7,"hasPressure":false,"hasFlow":false,"hasButtons":true}""")
        assertEquals(true, s.online)
        assertEquals(7, s.info?.zonas)
        assertEquals("esp32-relay", s.info?.tipo)
        assertEquals(1_000L, s.vistoEm)
        assertFalse(s.msg("status", "offline").online!!)
    }

    @Test fun estadoComNulos() {
        val s = vazio.msg(
            "state",
            """{"mode":"running","activeZone":3,"remainingS":412,"nextZone":4,"rainDelayUntil":null,"fault":null,"ts":1791230400}""",
            agora = 5_000,
        )
        val e = s.estado!!
        assertEquals(Modo.REGANDO, e.modo)
        assertEquals(3, e.zonaAtiva)
        assertEquals(412, e.restanteS)
        assertNull(e.chuvaAte)
        assertNull(e.falha)
        assertEquals(1791230400L, e.ts)
        assertEquals(5_000L, s.estadoRecebidoEm)
    }

    @Test fun estadoSemRelogioEComFalha() {
        val e = vazio.msg("state", """{"mode":"idle","activeZone":null,"remainingS":null,"nextZone":null,"rainDelayUntil":null,"fault":"dry"}""").estado!!
        assertEquals(Modo.PARADO, e.modo)
        assertEquals(Falha.SECO, e.falha)
        assertNull(e.ts)
    }

    @Test fun zonasEmOrdemELimitadasPeloInfo() {
        val s = vazio
            .msg("zone/2/state", """{"on":true,"name":"Horta","defaultDurationS":600}""")
            .msg("zone/1/state", """{"on":false,"name":"Gramado","defaultDurationS":480}""")
            .msg("zone/9/state", """{"on":false,"name":"Velha","defaultDurationS":60}""")
        assertEquals(listOf(1, 2, 9), s.zonas.map { it.numero })
        val comInfo = s.msg("info", """{"type":"esp32-relay","fw":"0.1.0","zones":7,"hasPressure":false,"hasFlow":false,"hasButtons":true}""")
        assertEquals(listOf(1, 2), comInfo.zonas.map { it.numero })
        assertTrue(comInfo.zona(2)!!.ligada)
        // Atualizar uma zona troca, não duplica.
        assertEquals(2, comInfo.msg("zone/2/state", """{"on":false,"name":"Horta","defaultDurationS":600}""").zonas.size)
    }

    @Test fun sensoresEAgenda() {
        val s = vazio
            .msg("sensors", """{"rain":true,"level":"low"}""")
            .msg("schedule", """{"enabled":true,"days":62,"starts":[360,1080]}""")
        assertTrue(s.sensores!!.chuva)
        assertEquals(Nivel.BAIXO, s.sensores!!.nivel)
        assertNull(s.sensores!!.pressaoKpa)
        assertEquals(Agenda(true, 62, listOf(360, 1080)), s.agenda)
    }

    @Test fun mensagemForaDoContratoNaoMudaNada() {
        assertSame(vazio, vazio.msg("state", "{quebrado"))
        assertSame(vazio, vazio.msg("state", """{"mode":"voando"}"""))
        assertSame(vazio, vazio.msg("cmd", """{"id":"1","op":"stop_all"}"""))
        assertSame(vazio, vazio.msg("status", ""))
        assertSame(vazio, vazio.aplicar("lotus/v1/clp/status", "online", 1))
    }

    @Test fun comandos() {
        fun o(c: Comando) = JSONObject(c.json("x"))
        o(Comando.RegarZona(2, 300)).let {
            assertEquals("x", it.getString("id"))
            assertEquals("start_zone", it.getString("op"))
            assertEquals(2, it.getInt("zone"))
            assertEquals(300, it.getInt("durationS"))
        }
        assertFalse(o(Comando.RegarZona(2)).has("durationS"))
        assertEquals("start_cycle", o(Comando.RegarTudo).getString("op"))
        assertEquals("stop_all", o(Comando.PararTudo).getString("op"))
        assertEquals("pause", o(Comando.Pausar).getString("op"))
        assertEquals("resume", o(Comando.Retomar).getString("op"))
        assertEquals(24, o(Comando.AdiarPorChuva(24)).getInt("hours"))
        o(Comando.MudarZona(3, 900)).let {
            assertEquals("set_zone", it.getString("op"))
            assertEquals(900, it.getInt("defaultDurationS"))
        }
        o(Comando.MudarAgenda(Agenda(true, 127, listOf(360)))).let {
            assertEquals("set_schedule", it.getString("op"))
            assertTrue(it.getBoolean("enabled"))
            assertEquals(127, it.getInt("days"))
            assertEquals(360, it.getJSONArray("starts").getInt(0))
        }
    }

    @Test fun acks() {
        assertEquals("a" to Resposta.Ok, lerAck("""{"id":"a","ok":true}"""))
        assertEquals("b" to Resposta.Erro(ErroComando.SEM_AGUA), lerAck("""{"id":"b","ok":false,"error":"fault_dry"}"""))
        assertEquals("c" to Resposta.Erro(ErroComando.PEDIDO_INVALIDO), lerAck("""{"id":"c","ok":false,"error":"novo"}"""))
        assertNull(lerAck("""{"ok":true}"""))
    }
}
