package com.lotus.dados

import kotlinx.coroutines.flow.StateFlow

/**
 * Fonte dos dados das casas do usuário ([CasasDoUsuario]). Hoje é o [SimuladorLotus]; a versão MQTT
 * implementa esta mesma interface, e as telas não mudam.
 */
interface LotusRepositorio {
    val sites: StateFlow<List<Site>>

    /** Publica o comando e espera o `ack` (até ~10 s). */
    suspend fun enviar(site: SiteId, comando: Comando): Resposta
}
