package com.lotus.dados

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/** Onde e como entrar no broker: vem de env/<usuario>.env (`MQTT_URL`, `MQTT_USUARIO`, `MQTT_SENHA`). */
data class ConfigMqtt(
    val url: String,        // ssl://xxxx.emqxsl.com:8883 (nuvem) ou tcp://10.0.2.2:1883 (Mosquitto local)
    val usuario: String,    // app-esp | app-clp | app-admin (lotus-bkd/broker/README.md)
    val senha: String,
)

/**
 * Os quadros de verdade, pelo broker MQTT. Assina `lotus/v1/{site}/#` de cada casa de [casas],
 * monta o [Site] com as mensagens retidas e publica os comandos em `cmd`, esperando o `cmd/ack`
 * com o mesmo `id`. A conexão vive enquanto [escopo] viver.
 *
 * TLS pelo trust store do Android: a CA do EMQX (DigiCert Global Root G2) já vem no sistema.
 */
class MqttLotus(
    private val escopo: CoroutineScope,
    private val casas: List<SiteId>,
    private val config: ConfigMqtt,
    private val agora: () -> Long = System::currentTimeMillis,
) : LotusRepositorio {

    private val _sites = MutableStateFlow(casas.map(::siteVazio))
    override val sites: StateFlow<List<Site>> = _sites.asStateFlow()

    /** Comandos esperando o `ack`, pelo `id`. */
    private val aguardando = ConcurrentHashMap<String, CompletableDeferred<Resposta>>()

    // Um id por abertura do app: o EMQX derruba a sessão antiga se outro cliente entrar com o mesmo id.
    private val cliente = MqttAsyncClient(config.url, "lotus-app-${UUID.randomUUID()}", MemoryPersistence())

    private val opcoes = MqttConnectOptions().apply {
        userName = config.usuario
        password = config.senha.toCharArray()
        // Sessão limpa: o estado vem todo das mensagens retidas, e as inscrições são refeitas a cada conexão.
        isCleanSession = true
        isAutomaticReconnect = true
        maxReconnectDelay = 60_000
        keepAliveInterval = 30
        connectionTimeout = 10
    }

    init {
        cliente.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String) {
                Log.i(TAG, if (reconnect) "reconectado" else "conectado a $serverURI")
                // Uma inscrição por site: um filtro mais amplo seria negado inteiro pelo EMQX.
                val filtros = casas.map { it.filtro() }.toTypedArray()
                runCatching { cliente.subscribe(filtros, IntArray(filtros.size) { 1 }) }
                    .onFailure { Log.w(TAG, "inscrição falhou", it) }
            }

            override fun connectionLost(causa: Throwable?) {
                Log.w(TAG, "conexão perdida", causa)
                semSinal()
            }

            override fun messageArrived(topico: String, mensagem: MqttMessage) {
                // Exceção aqui derruba a conexão do Paho: nada pode escapar.
                runCatching { receber(topico, String(mensagem.payload, Charsets.UTF_8)) }
                    .onFailure { Log.w(TAG, "mensagem ignorada em $topico", it) }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
        })

        escopo.launch(Dispatchers.IO) { conectar() }
        escopo.coroutineContext.job.invokeOnCompletion {
            aguardando.values.forEach { it.complete(Resposta.SemResposta) }
            // Desconectar bloqueia; não segura a thread de quem encerrou o escopo.
            thread(name = "lotus-mqtt-fechar") {
                runCatching { cliente.disconnectForcibly(1_000) }
                runCatching { cliente.close() }
            }
        }
    }

    override suspend fun enviar(site: SiteId, comando: Comando): Resposta {
        val id = UUID.randomUUID().toString()
        val resposta = CompletableDeferred<Resposta>()
        // Registra antes de publicar: o ack pode chegar antes de o publish retornar.
        aguardando[id] = resposta
        try {
            val mensagem = MqttMessage(comando.json(id).toByteArray(Charsets.UTF_8)).apply {
                qos = 1
                isRetained = false // retido, o quadro executaria de novo a cada reconexão
            }
            try {
                cliente.publish(site.topicoComando(), mensagem)
            } catch (e: MqttException) {
                Log.w(TAG, "não deu para publicar $comando", e)
                return Resposta.SemResposta
            }
            return withTimeoutOrNull(TEMPO_ACK_MS) { resposta.await() } ?: Resposta.SemResposta
        } finally {
            aguardando.remove(id)
        }
    }

    /**
     * O Paho só reconecta sozinho depois da primeira conexão bem-sucedida.
     * Até lá, tenta de novo com espera crescente.
     */
    private suspend fun conectar() {
        var espera = 2_000L
        while (escopo.isActive) {
            try {
                cliente.connect(opcoes).waitForCompletion()
                return
            } catch (e: MqttException) {
                if (e.reasonCode == MqttException.REASON_CODE_CLIENT_CONNECTED.toInt()) return
                Log.w(TAG, "não conectou (${e.reasonCode}); nova tentativa em ${espera / 1000} s", e)
                semSinal()
            }
            delay(espera)
            espera = (espera * 2).coerceAtMost(60_000)
        }
    }

    private fun receber(topico: String, payload: String) {
        val site = siteDoTopico(topico) ?: return
        if (topico == "${site.topicoComando()}/ack") {
            val (id, resposta) = lerAck(payload) ?: return
            aguardando[id]?.complete(resposta)
            return
        }
        val agoraMs = agora()
        _sites.update { lista -> lista.map { if (it.id == site) it.aplicar(topico, payload, agoraMs) else it } }
    }

    /**
     * Sem broker não dá para saber dos quadros: mostra as casas como fora do ar, com o último "visto".
     * Ao reconectar, o `status` retido corrige.
     */
    private fun semSinal() {
        _sites.update { lista -> lista.map { it.copy(online = false) } }
    }

    private companion object {
        const val TAG = "lotus-mqtt"
        const val TEMPO_ACK_MS = 10_000L

        fun siteVazio(id: SiteId) = Site(
            id = id, nome = id.casa, online = null, vistoEm = null, info = null, estado = null,
            estadoRecebidoEm = null, zonas = emptyList(), sensores = null, agenda = null,
        )
    }
}
