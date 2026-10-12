package com.lotus.dados

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/*
 * Tradução entre o JSON do contrato lotus/v1 (lotus-bkd/docs/CONTRATO-MQTT.md) e os modelos de Contrato.kt.
 * Sem MQTT aqui: só texto entrando e saindo, para dar para testar sem broker.
 */

const val PREFIXO_TOPICO = "lotus/v1"

/** Inscrição de um site. O EMQX nega a inscrição inteira se o filtro for mais amplo que isto. */
fun SiteId.filtro() = "$PREFIXO_TOPICO/$topico/#"

fun SiteId.topicoComando() = "$PREFIXO_TOPICO/$topico/cmd"

/** O site dono de um tópico `lotus/v1/{site}/...`, ou null se não for nosso. */
fun siteDoTopico(topico: String): SiteId? {
    val partes = topico.split('/')
    if (partes.size < 3 || "${partes[0]}/${partes[1]}" != PREFIXO_TOPICO) return null
    return SiteId.entries.firstOrNull { it.topico == partes[2] }
}

/**
 * O site com a mensagem de [topico] aplicada, ou o mesmo site se a mensagem não for de estado
 * (o `cmd` que o próprio app publicou, o `cmd/ack`) ou não seguir o contrato.
 */
fun Site.aplicar(topico: String, payload: String, agoraMs: Long): Site {
    val resto = topico.removePrefix("$PREFIXO_TOPICO/${id.topico}/")
    if (resto == topico || payload.isEmpty()) return this
    val novo = try {
        when {
            resto == "status" -> when (payload) {
                "online" -> copy(online = true)
                "offline" -> copy(online = false)
                else -> null
            }
            resto == "info" -> lerInfo(JSONObject(payload)).let { info ->
                copy(info = info, zonas = zonas.filter { it.numero <= info.zonas })
            }
            resto == "state" -> copy(estado = lerEstado(JSONObject(payload)), estadoRecebidoEm = agoraMs)
            resto == "sensors" -> copy(sensores = lerSensores(JSONObject(payload)))
            resto == "schedule" -> copy(agenda = lerAgenda(JSONObject(payload)))
            resto.startsWith("zone/") && resto.endsWith("/state") -> {
                val numero = resto.removePrefix("zone/").removeSuffix("/state").toIntOrNull()
                val limite = info?.zonas ?: Int.MAX_VALUE
                if (numero == null || numero !in 1..limite) null
                else copy(zonas = (zonas.filter { it.numero != numero } + lerZona(numero, JSONObject(payload))).sortedBy { it.numero })
            }
            else -> null
        }
    } catch (_: JSONException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
    return novo?.copy(vistoEm = agoraMs) ?: this
}

/** O JSON publicado em `cmd`. */
fun Comando.json(id: String): String {
    val o = JSONObject().put("id", id)
    when (this) {
        is Comando.RegarZona -> o.put("op", "start_zone").put("zone", zona).apply { duracaoS?.let { put("durationS", it) } }
        Comando.RegarTudo -> o.put("op", "start_cycle")
        Comando.PararTudo -> o.put("op", "stop_all")
        Comando.Pausar -> o.put("op", "pause")
        Comando.Retomar -> o.put("op", "resume")
        is Comando.AdiarPorChuva -> o.put("op", "rain_delay").put("hours", horas)
        is Comando.MudarZona -> o.put("op", "set_zone").put("zone", zona).put("defaultDurationS", duracaoPadraoS)
        is Comando.MudarAgenda -> o.put("op", "set_schedule")
            .put("enabled", agenda.ativa)
            .put("days", agenda.dias)
            .put("starts", JSONArray(agenda.inicios))
    }
    return o.toString()
}

/** `cmd/ack`: o `id` do comando e a resposta, ou null se não for um ack válido. */
fun lerAck(payload: String): Pair<String, Resposta>? = try {
    val o = JSONObject(payload)
    val id = o.getString("id")
    val resposta = if (o.getBoolean("ok")) Resposta.Ok else {
        val codigo = o.optString("error")
        // Erro fora do contrato: trata como pedido inválido em vez de perder a resposta.
        Resposta.Erro(ErroComando.entries.firstOrNull { it.codigo == codigo } ?: ErroComando.PEDIDO_INVALIDO)
    }
    id to resposta
} catch (_: JSONException) {
    null
}

private fun lerInfo(o: JSONObject) = InfoQuadro(
    tipo = o.getString("type"),
    firmware = o.getString("fw"),
    zonas = o.getInt("zones"),
    temPressao = o.optBoolean("hasPressure"),
    temVazao = o.optBoolean("hasFlow"),
    temBotoes = o.optBoolean("hasButtons"),
)

private fun lerEstado(o: JSONObject) = EstadoQuadro(
    modo = when (val modo = o.getString("mode")) {
        "idle" -> Modo.PARADO
        "running" -> Modo.REGANDO
        "paused" -> Modo.PAUSADO
        "manual" -> Modo.MANUAL
        else -> throw IllegalArgumentException("mode desconhecido: $modo")
    },
    zonaAtiva = o.intOuNull("activeZone"),
    restanteS = o.intOuNull("remainingS"),
    proximaZona = o.intOuNull("nextZone"),
    chuvaAte = o.longOuNull("rainDelayUntil"),
    falha = when (val falha = o.textoOuNull("fault")) {
        null -> null
        "dry" -> Falha.SECO
        "inverter" -> Falha.INVERSOR
        "no_flow" -> Falha.SEM_VAZAO
        "watchdog" -> Falha.WATCHDOG
        else -> throw IllegalArgumentException("fault desconhecido: $falha")
    },
    ts = o.longOuNull("ts"),
)

private fun lerZona(numero: Int, o: JSONObject) = Zona(
    numero = numero,
    ligada = o.getBoolean("on"),
    nome = nomeDeArea(o.getString("name")),
    duracaoPadraoS = o.getInt("defaultDurationS"),
)

private val nomeGenerico = Regex("""\s*zona\s*(\d+)\s*""", RegexOption.IGNORE_CASE)

/** O quadro sai de fábrica com "Zona 3"; a tela chama de área. Nome dado de verdade ("Horta") passa igual. */
private fun nomeDeArea(nome: String) = nomeGenerico.matchEntire(nome)?.let { "Área ${it.groupValues[1]}" } ?: nome

private fun lerSensores(o: JSONObject) = Sensores(
    chuva = o.getBoolean("rain"),
    nivel = if (o.getString("level") == "low") Nivel.BAIXO else Nivel.OK,
    pressaoKpa = o.doubleOuNull("pressureKpa"),
    vazaoLpm = o.doubleOuNull("flowLpm"),
)

private fun lerAgenda(o: JSONObject): Agenda {
    val inicios = o.getJSONArray("starts")
    return Agenda(
        ativa = o.getBoolean("enabled"),
        dias = o.getInt("days"),
        inicios = List(inicios.length()) { inicios.getInt(it) },
    )
}

// isNull cobre os dois casos do contrato: campo ausente e campo `null`.
private fun JSONObject.intOuNull(chave: String) = if (isNull(chave)) null else getInt(chave)
private fun JSONObject.longOuNull(chave: String) = if (isNull(chave)) null else getLong(chave)
private fun JSONObject.doubleOuNull(chave: String) = if (isNull(chave)) null else getDouble(chave)
private fun JSONObject.textoOuNull(chave: String) = if (isNull(chave)) null else getString(chave)
