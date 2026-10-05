package com.lotus.ui

import com.lotus.dados.Comando
import com.lotus.dados.ErroComando
import com.lotus.dados.Falha

/** Mensagens em termos do quintal, nunca do hardware. */

fun ErroComando.mensagem(): String = when (this) {
    ErroComando.SEM_AGUA -> "Sem água na caixa. O quadro não abre zona assim."
    ErroComando.MODO_MANUAL -> "O quadro está no modo manual. Volte para o automático no painel."
    ErroComando.ZONA_INVALIDA -> "Essa zona não existe nesta casa."
    ErroComando.DURACAO_INVALIDA -> "Escolha um tempo entre 1 segundo e 24 horas."
    ErroComando.HORAS_INVALIDAS -> "Dá para adiar por no máximo 14 dias."
    ErroComando.SEM_RELOGIO -> "O quadro ainda está acertando o relógio. Tente de novo em instantes."
    ErroComando.PEDIDO_INVALIDO, ErroComando.OP_DESCONHECIDA ->
        "O quadro não entendeu o pedido. Talvez precise atualizar o app."
}

data class TextoFalha(val titulo: String, val oQueFazer: String)

fun Falha.texto(): TextoFalha = when (this) {
    Falha.SECO -> TextoFalha(
        "Sem água na caixa",
        "A irrigação está parada até o nível voltar. Confira a caixa e a boia.",
    )
    Falha.INVERSOR -> TextoFalha(
        "A bomba parou com erro",
        "O inversor da bomba acusou falha. Veja o código no visor dele antes de religar.",
    )
    Falha.SEM_VAZAO -> TextoFalha(
        "A água não está passando",
        "A bomba ligou, mas não saiu água. Pode ser registro fechado ou cano entupido.",
    )
    Falha.WATCHDOG -> TextoFalha(
        "O quadro perdeu o controle remoto",
        "Por segurança as zonas foram fechadas. Ele volta sozinho quando reconectar.",
    )
}

/** Para a mensagem de confirmação: "Pedido enviado: pausar". */
fun Comando.descricao(): String = when (this) {
    is Comando.RegarZona -> "regar a zona $zona"
    Comando.RegarTudo -> "regar todas as zonas"
    Comando.PararTudo -> "parar tudo"
    Comando.Pausar -> "pausar"
    Comando.Retomar -> "retomar"
    is Comando.AdiarPorChuva -> if (horas == 0) "cancelar o adiamento" else "adiar a agenda"
    is Comando.MudarZona -> "mudar o tempo da zona $zona"
    is Comando.MudarAgenda -> "salvar a agenda"
}
