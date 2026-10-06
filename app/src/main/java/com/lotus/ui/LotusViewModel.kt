package com.lotus.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lotus.dados.AreasDaCasa
import com.lotus.dados.AreasDoApp
import com.lotus.dados.CasasDoUsuario
import com.lotus.dados.Comando
import com.lotus.dados.LotusRepositorio
import com.lotus.dados.Resposta
import com.lotus.dados.SimuladorLotus
import com.lotus.dados.Site
import com.lotus.dados.SiteId
import com.lotus.dados.comNomesDoApp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Um comando esperando o `ack`. As telas usam isso para pôr o botão em progresso. */
data class Pendente(val site: SiteId, val comando: Comando)

class LotusViewModel(app: Application) : AndroidViewModel(app) {

    // Troca pelo repositório MQTT quando o broker estiver no ar.
    private val fonte: LotusRepositorio = SimuladorLotus(viewModelScope, CasasDoUsuario)

    private val areasDoApp = AreasDoApp(app)

    /** Áreas escondidas e nomes dados no app, por casa. */
    val areas: StateFlow<Map<SiteId, AreasDaCasa>> = areasDoApp.casas

    // As telas já recebem os nomes que a pessoa deu às áreas.
    val sites: StateFlow<List<Site>> = combine(fonte.sites, areas) { sites, areas ->
        sites.map { it.comNomesDoApp(areas[it.id]) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, fonte.sites.value.map { it.comNomesDoApp(areas.value[it.id]) })

    private val _pendentes = MutableStateFlow<Set<Pendente>>(emptySet())
    val pendentes: StateFlow<Set<Pendente>> = _pendentes.asStateFlow()

    private val _avisos = Channel<String>(Channel.BUFFERED)
    // Só erros: quando o comando dá certo, o novo estado na tela já é a confirmação.
    val avisos: Flow<String> = _avisos.receiveAsFlow()

    fun enviar(site: SiteId, comando: Comando) {
        val pendente = Pendente(site, comando)
        if (pendente in _pendentes.value) return
        _pendentes.update { it + pendente }
        viewModelScope.launch {
            val aviso = when (val r = fonte.enviar(site, comando)) {
                Resposta.Ok -> null
                is Resposta.Erro -> r.erro.mensagem()
                Resposta.SemResposta -> "O quadro não respondeu. Não deu para ${comando.descricao()}."
            }
            _pendentes.update { it - pendente }
            aviso?.let { _avisos.send(it) }
        }
    }

    fun removerArea(site: SiteId, zona: Int) = areasDoApp.remover(site, zona)

    fun adicionarArea(site: SiteId, zona: Int, nome: String) = areasDoApp.adicionar(site, zona, nome)
}
