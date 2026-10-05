package com.lotus.dados

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Dois quadros de mentira que seguem o contrato lotus/v1: retêm o último estado,
 * republicam o `state` a cada 15 s durante a irrigação e respondem aos comandos
 * com os mesmos erros do quadro de verdade. Serve para desenhar e testar as telas
 * antes do broker existir.
 */
class SimuladorLotus(
    private val escopo: CoroutineScope,
    private val agora: () -> Long = System::currentTimeMillis,
) : LotusRepositorio {

    /** O que o quadro sabe e não publica: quando a zona termina e o que vem depois. */
    private class Quadro(
        var site: Site,
        var fimDaZonaEm: Long? = null,
        var restantePausadoMs: Long? = null,
        val fila: ArrayDeque<Int> = ArrayDeque(),
        var ultimoStateEm: Long = 0,
    )

    private val trava = Mutex()
    private val quadros = linkedMapOf(
        SiteId.ESP to Quadro(siteVazio(SiteId.ESP)),
        SiteId.CLP to Quadro(siteVazio(SiteId.CLP)),
    )

    private val _sites = MutableStateFlow(quadros.values.map { it.site })
    override val sites: StateFlow<List<Site>> = _sites.asStateFlow()

    init {
        escopo.launch {
            // As mensagens retidas chegam logo depois da inscrição, uma casa de cada vez.
            delay(1_200)
            trava.withLock { carregarEsp(); publicar() }
            delay(600)
            trava.withLock { carregarClp(); publicar() }
            while (true) {
                delay(1_000)
                trava.withLock { quadros.values.forEach { avancar(it) }; publicar() }
            }
        }
    }

    override suspend fun enviar(site: SiteId, comando: Comando): Resposta {
        delay(700) // ida e volta pelo broker
        return trava.withLock {
            val quadro = quadros.getValue(site)
            if (quadro.site.online != true) {
                delay(9_300)
                return@withLock Resposta.SemResposta
            }
            val erro = executar(quadro, comando)
            publicar()
            if (erro == null) Resposta.Ok else Resposta.Erro(erro)
        }
    }

    private fun executar(q: Quadro, comando: Comando): ErroComando? {
        val estado = q.site.estado ?: return ErroComando.PEDIDO_INVALIDO
        val totalZonas = q.site.info?.zonas ?: 0
        if (estado.modo == Modo.MANUAL) return ErroComando.MODO_MANUAL
        val agoraMs = agora()

        when (comando) {
            is Comando.RegarZona -> {
                if (comando.zona !in 1..totalZonas) return ErroComando.ZONA_INVALIDA
                if (comando.duracaoS != null && comando.duracaoS !in 1..DIA_S) return ErroComando.DURACAO_INVALIDA
                if (q.site.sensores?.nivel == Nivel.BAIXO) return ErroComando.SEM_AGUA
                q.fila.clear()
                abrir(q, comando.zona, comando.duracaoS)
            }
            Comando.RegarTudo -> {
                if (q.site.sensores?.nivel == Nivel.BAIXO) return ErroComando.SEM_AGUA
                q.fila.clear()
                q.fila.addAll(2..totalZonas)
                abrir(q, 1, null)
            }
            Comando.PararTudo -> parar(q)
            Comando.Pausar -> if (estado.modo == Modo.REGANDO) {
                q.restantePausadoMs = (q.fimDaZonaEm ?: agoraMs) - agoraMs
                q.fimDaZonaEm = null
                q.site = q.site.copy(zonas = q.site.zonas.map { it.copy(ligada = false) })
                mudarEstado(q) { it.copy(modo = Modo.PAUSADO) }
            }
            Comando.Retomar -> if (estado.modo == Modo.PAUSADO) {
                val zona = estado.zonaAtiva ?: return null
                q.fimDaZonaEm = agoraMs + (q.restantePausadoMs ?: 0)
                q.restantePausadoMs = null
                q.site = q.site.copy(zonas = q.site.zonas.map { it.copy(ligada = it.numero == zona) })
                mudarEstado(q) { it.copy(modo = Modo.REGANDO) }
            }
            is Comando.AdiarPorChuva -> {
                if (comando.horas !in 0..336) return ErroComando.HORAS_INVALIDAS
                if (estado.ts == null) return ErroComando.SEM_RELOGIO
                parar(q)
                val ate = if (comando.horas == 0) null else agoraMs / 1000 + comando.horas * 3600L
                mudarEstado(q) { it.copy(chuvaAte = ate) }
            }
            is Comando.MudarZona -> {
                if (comando.zona !in 1..totalZonas) return ErroComando.ZONA_INVALIDA
                if (comando.duracaoPadraoS !in 1..DIA_S) return ErroComando.DURACAO_INVALIDA
                q.site = q.site.copy(zonas = q.site.zonas.map {
                    if (it.numero == comando.zona) it.copy(duracaoPadraoS = comando.duracaoPadraoS) else it
                })
            }
            is Comando.MudarAgenda -> {
                val a = comando.agenda
                if (a.dias !in 0..Agenda.TODOS_OS_DIAS || a.inicios.size > Agenda.MAX_INICIOS ||
                    a.inicios.any { it !in 0 until 24 * 60 }
                ) return ErroComando.PEDIDO_INVALIDO
                q.site = q.site.copy(agenda = a.copy(inicios = a.inicios.sorted()))
            }
        }
        return null
    }

    private fun abrir(q: Quadro, zona: Int, duracaoS: Int?) {
        val duracao = duracaoS ?: q.site.zona(zona)?.duracaoPadraoS ?: 600
        q.fimDaZonaEm = agora() + duracao * 1000L
        q.restantePausadoMs = null
        q.site = q.site.copy(zonas = q.site.zonas.map { it.copy(ligada = it.numero == zona) })
        medirBomba(q, ligada = true)
        mudarEstado(q) { it.copy(modo = Modo.REGANDO, zonaAtiva = zona, proximaZona = q.fila.firstOrNull()) }
    }

    private fun parar(q: Quadro) {
        q.fila.clear()
        q.fimDaZonaEm = null
        q.restantePausadoMs = null
        q.site = q.site.copy(zonas = q.site.zonas.map { it.copy(ligada = false) })
        medirBomba(q, ligada = false)
        mudarEstado(q) { it.copy(modo = Modo.PARADO, zonaAtiva = null, restanteS = null, proximaZona = null) }
    }

    private fun avancar(q: Quadro) {
        val fim = q.fimDaZonaEm ?: return
        val agoraMs = agora()
        if (agoraMs >= fim) {
            val proxima = q.fila.removeFirstOrNull()
            if (proxima != null) abrir(q, proxima, null) else parar(q)
        } else if (agoraMs - q.ultimoStateEm >= 15_000) {
            mudarEstado(q) { it }
        }
    }

    /** Muda o `state` e o "republica" com o `remainingS` do momento. */
    private fun mudarEstado(q: Quadro, mudanca: (EstadoQuadro) -> EstadoQuadro) {
        val agoraMs = agora()
        val restanteMs = q.fimDaZonaEm?.let { it - agoraMs } ?: q.restantePausadoMs
        val novo = mudanca(q.site.estado ?: return).copy(
            restanteS = restanteMs?.let { ((it + 999) / 1000).toInt() },
            ts = agoraMs / 1000,
        )
        q.ultimoStateEm = agoraMs
        q.site = q.site.copy(estado = novo, estadoRecebidoEm = agoraMs, vistoEm = agoraMs)
    }

    /** Só o site CLP mede: pressão constante pelo PID do inversor e vazão da zona aberta. */
    private fun medirBomba(q: Quadro, ligada: Boolean) {
        val sensores = q.site.sensores ?: return
        if (q.site.info?.temPressao != true) return
        q.site = q.site.copy(
            sensores = sensores.copy(
                pressaoKpa = if (ligada) 280.0 else 0.0,
                vazaoLpm = if (ligada) 18.5 else 0.0,
            ),
        )
    }

    private fun publicar() {
        _sites.value = quadros.values.map { it.site }
    }

    private fun carregarEsp() {
        val q = quadros.getValue(SiteId.ESP)
        val nomes = listOf(
            "Gramado da frente", "Canteiro de ervas", "Horta", "Jardim lateral",
            "Fundos", "Vasos da varanda", "Pomar",
        )
        q.site = q.site.copy(
            online = true,
            info = InfoQuadro("esp32-relay", "0.1.0", zonas = 7, temPressao = false, temVazao = false, temBotoes = true),
            zonas = nomes.mapIndexed { i, nome -> Zona(i + 1, false, nome, duracaoPadraoS = (8 + i * 2) * 60) },
            sensores = Sensores(chuva = false, nivel = Nivel.OK, pressaoKpa = null, vazaoLpm = null),
            agenda = Agenda(ativa = true, dias = Agenda.TODOS_OS_DIAS, inicios = listOf(6 * 60, 18 * 60)),
            estado = EstadoQuadro(Modo.PARADO, null, null, null, null, null, null),
        )
        // Chega no meio do ciclo da manhã: zona 3 aberta, faltando quase 7 minutos.
        q.fila.addAll(4..7)
        abrir(q, 3, 412)
    }

    private fun carregarClp() {
        val q = quadros.getValue(SiteId.CLP)
        val nomes = listOf("Gramado", "Cerca viva", "Canteiro central", "Horta", "Árvores do fundo")
        q.site = q.site.copy(
            online = true,
            info = InfoQuadro("plc-delta", "0.1.0", zonas = 5, temPressao = true, temVazao = true, temBotoes = true),
            zonas = nomes.mapIndexed { i, nome -> Zona(i + 1, false, nome, duracaoPadraoS = 15 * 60) },
            sensores = Sensores(chuva = false, nivel = Nivel.OK, pressaoKpa = 0.0, vazaoLpm = 0.0),
            agenda = Agenda(ativa = true, dias = 0b0111110, inicios = listOf(5 * 60 + 30)),
            estado = EstadoQuadro(Modo.PARADO, null, null, null, null, null, null),
        )
        mudarEstado(q) { it }
    }

    private companion object {
        const val DIA_S = 24 * 60 * 60

        fun siteVazio(id: SiteId) = Site(
            id = id, nome = id.casa, online = null, vistoEm = null, info = null, estado = null,
            estadoRecebidoEm = null, zonas = emptyList(), sensores = null, agenda = null,
        )
    }
}
