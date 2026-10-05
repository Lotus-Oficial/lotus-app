package com.lotus.dados

/*
 * Modelos do contrato MQTT lotus/v1 (lotus-bkd/docs/CONTRATO-MQTT.md).
 * Os nomes ficam em português no app; o comentário de cada campo diz qual é o campo no JSON.
 */

/**
 * `{site}` nos tópicos e a casa onde cada quadro está. O contrato não tem nome de casa,
 * então ele fica aqui. A ordem é a da tela inicial.
 */
enum class SiteId(val topico: String, val casa: String) {
    ESP("esp", "Casa do Fernando"),
    CLP("clp", "Casa do Felipe"),
}

/** `lotus/v1/{site}/info` */
data class InfoQuadro(
    val tipo: String,            // type: esp32-relay | plc-delta
    val firmware: String,        // fw
    val zonas: Int,              // zones
    val temPressao: Boolean,     // hasPressure
    val temVazao: Boolean,       // hasFlow
    val temBotoes: Boolean,      // hasButtons
)

/** `mode` */
enum class Modo { PARADO, REGANDO, PAUSADO, MANUAL }

/** `fault`; `INVERSOR`, `SEM_VAZAO` e `WATCHDOG` só existem no site CLP. */
enum class Falha { SECO, INVERSOR, SEM_VAZAO, WATCHDOG }

/** `lotus/v1/{site}/state` */
data class EstadoQuadro(
    val modo: Modo,
    val zonaAtiva: Int?,         // activeZone
    val restanteS: Int?,         // remainingS
    val proximaZona: Int?,       // nextZone
    val chuvaAte: Long?,         // rainDelayUntil (epoch s)
    val falha: Falha?,           // fault
    val ts: Long?,               // ts (epoch s), ausente sem relógio sincronizado
)

/** `lotus/v1/{site}/zone/{n}/state` */
data class Zona(
    val numero: Int,
    val ligada: Boolean,         // on
    val nome: String,            // name
    val duracaoPadraoS: Int,     // defaultDurationS
)

/** `level` */
enum class Nivel { OK, BAIXO }

/** `lotus/v1/{site}/sensors` */
data class Sensores(
    val chuva: Boolean,          // rain: sensor molhado
    val nivel: Nivel,            // level
    val pressaoKpa: Double?,     // pressureKpa (só CLP)
    val vazaoLpm: Double?,       // flowLpm (só CLP)
)

/** `lotus/v1/{site}/schedule` */
data class Agenda(
    val ativa: Boolean,          // enabled
    val dias: Int,               // days: bit 0 = domingo ... bit 6 = sábado
    val inicios: List<Int>,      // starts: minutos desde a meia-noite, até 4
) {
    companion object {
        const val MAX_INICIOS = 4
        const val TODOS_OS_DIAS = 127
    }
}

/** Comandos publicados em `lotus/v1/{site}/cmd`. */
sealed interface Comando {
    data class RegarZona(val zona: Int, val duracaoS: Int? = null) : Comando   // start_zone
    data object RegarTudo : Comando                                             // start_cycle
    data object PararTudo : Comando                                             // stop_all
    data object Pausar : Comando                                                // pause
    data object Retomar : Comando                                               // resume
    data class AdiarPorChuva(val horas: Int) : Comando                          // rain_delay (0 cancela)
    data class MudarZona(val zona: Int, val duracaoPadraoS: Int) : Comando      // set_zone
    data class MudarAgenda(val agenda: Agenda) : Comando                        // set_schedule
}

/** `error` do `cmd/ack`. */
enum class ErroComando(val codigo: String) {
    PEDIDO_INVALIDO("bad_request"),
    OP_DESCONHECIDA("unknown_op"),
    ZONA_INVALIDA("invalid_zone"),
    DURACAO_INVALIDA("invalid_duration"),
    HORAS_INVALIDAS("invalid_hours"),
    SEM_AGUA("fault_dry"),
    SEM_RELOGIO("no_time"),
    MODO_MANUAL("manual"),
}

/** O que voltou de um comando: `ack` ok, `ack` com erro, ou nada em ~10 s. */
sealed interface Resposta {
    data object Ok : Resposta
    data class Erro(val erro: ErroComando) : Resposta
    data object SemResposta : Resposta
}

/**
 * Tudo o que o app sabe de um site, juntando os tópicos retidos.
 * Campos nulos = a mensagem ainda não chegou.
 */
data class Site(
    val id: SiteId,
    val nome: String,
    val online: Boolean?,            // status; null = ainda não sabemos
    val vistoEm: Long?,              // última mensagem recebida (epoch ms do celular)
    val info: InfoQuadro?,
    val estado: EstadoQuadro?,
    val estadoRecebidoEm: Long?,     // quando o `state` chegou (epoch ms), base da contagem local
    val zonas: List<Zona>,
    val sensores: Sensores?,
    val agenda: Agenda?,
) {
    val carregado: Boolean get() = info != null && estado != null
    fun zona(numero: Int?): Zona? = zonas.firstOrNull { it.numero == numero }
}
