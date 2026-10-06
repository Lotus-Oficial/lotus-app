package com.lotus.dados

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * O que a pessoa mudou nas áreas só dentro do app: quais saídas do quadro ficam fora da lista
 * ([escondidas]) e o nome que ela deu a cada uma ([nomes]).
 * O contrato não cria nem apaga zona (cada uma é uma saída do quadro): o quadro continua com
 * todas e passa por todas no ciclo, escondidas ou não.
 */
data class AreasDaCasa(
    val escondidas: Set<Int> = emptySet(),
    val nomes: Map<Int, String> = emptyMap(),
)

/** Guarda [AreasDaCasa] de cada casa no celular (SharedPreferences), fora do quadro. */
class AreasDoApp(contexto: Context) {

    private val prefs = contexto.getSharedPreferences("lotus_areas", Context.MODE_PRIVATE)

    private val _casas = MutableStateFlow(SiteId.entries.associateWith(::ler))
    val casas: StateFlow<Map<SiteId, AreasDaCasa>> = _casas.asStateFlow()

    /** Tira a área da lista do app e esquece o nome dado a ela. */
    fun remover(site: SiteId, zona: Int) = mudar(site) {
        AreasDaCasa(it.escondidas + zona, it.nomes - zona)
    }

    /** Traz de volta uma saída escondida, com o nome escolhido (vazio = o nome do quadro). */
    fun adicionar(site: SiteId, zona: Int, nome: String) = mudar(site) {
        val limpo = nome.trim()
        AreasDaCasa(it.escondidas - zona, if (limpo.isEmpty()) it.nomes - zona else it.nomes + (zona to limpo))
    }

    private fun mudar(site: SiteId, transformar: (AreasDaCasa) -> AreasDaCasa) {
        val nova = transformar(_casas.value.getValue(site))
        prefs.edit {
            putStringSet(chaveEscondidas(site), nova.escondidas.map(Int::toString).toSet())
            // Uma linha por área: "3=Canteiro novo".
            putStringSet(chaveNomes(site), nova.nomes.map { (n, nome) -> "$n=$nome" }.toSet())
        }
        _casas.value = _casas.value + (site to nova)
    }

    private fun ler(site: SiteId): AreasDaCasa {
        val escondidas = prefs.getStringSet(chaveEscondidas(site), null).orEmpty().mapNotNull(String::toIntOrNull).toSet()
        val nomes = prefs.getStringSet(chaveNomes(site), null).orEmpty().mapNotNull { linha ->
            val (n, nome) = linha.split('=', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
            n.toIntOrNull()?.let { it to nome }
        }.toMap()
        return AreasDaCasa(escondidas, nomes)
    }

    private fun chaveEscondidas(site: SiteId) = "escondidas_${site.topico}"
    private fun chaveNomes(site: SiteId) = "nomes_${site.topico}"
}

/** O site com os nomes que a pessoa deu às áreas no app no lugar dos nomes do quadro. */
fun Site.comNomesDoApp(areas: AreasDaCasa?): Site =
    if (areas == null || areas.nomes.isEmpty()) this
    else copy(zonas = zonas.map { z -> areas.nomes[z.numero]?.let { z.copy(nome = it) } ?: z })
