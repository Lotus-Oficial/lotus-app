package com.lotus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lotus.dados.CasasDoUsuario
import com.lotus.dados.Comando
import com.lotus.dados.LotusRepositorio
import com.lotus.dados.Resposta
import com.lotus.dados.SimuladorLotus
import com.lotus.dados.Site
import com.lotus.dados.SiteId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Um comando esperando o `ack`. As telas usam isso para pôr o botão em progresso. */
data class Pendente(val site: SiteId, val comando: Comando)

class LotusViewModel : ViewModel() {

    // Troca pelo repositório MQTT quando o broker estiver no ar.
    private val fonte: LotusRepositorio = SimuladorLotus(viewModelScope, CasasDoUsuario)

    val sites: StateFlow<List<Site>> = fonte.sites

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
}
