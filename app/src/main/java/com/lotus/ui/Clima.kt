package com.lotus.ui

import com.lotus.dados.Site

/** O tempo que a animação do Início mostra. */
enum class Tempo { SOL, NUBLADO, CHUVA }

/**
 * Por enquanto só o sensor de chuva do quadro (`sensors.rain`): molhado = chuva, seco = sol.
 * [Tempo.NUBLADO] fica para a previsão do tempo, que o contrato não tem e o app ainda não busca.
 * Fora do ar ou sem sensores não há animação: o último dado pode estar velho.
 */
fun tempo(site: Site): Tempo? {
    if (site.online != true) return null
    val sensores = site.sensores ?: return null
    return if (sensores.chuva) Tempo.CHUVA else Tempo.SOL
}
