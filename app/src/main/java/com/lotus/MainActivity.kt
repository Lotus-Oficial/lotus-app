package com.lotus

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lotus.dados.CasasDoUsuario
import com.lotus.dados.Comando
import com.lotus.ui.LotusViewModel
import com.lotus.ui.agenda.AgendaTela
import com.lotus.ui.boasvindas.BoasVindasTela
import com.lotus.ui.casa.ZonasTela
import com.lotus.ui.componentes.Aba
import com.lotus.ui.componentes.BarraNavegacao
import com.lotus.ui.componentes.EspacoDaBarra
import com.lotus.ui.componentes.LocalVidro
import com.lotus.ui.inicio.InicioTela
import com.lotus.ui.tema.LotusTheme
import dev.chrisbanes.haze.rememberHazeState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LotusTheme { LotusApp() } }
    }
}

private const val PREFS = "lotus"
private const val BOAS_VINDAS_VISTA = "boas_vindas_vista"

/** Boas-vindas na primeira vez; depois três abas (Início, Áreas, Agenda) da casa escolhida. */
@Composable
private fun LotusApp(vm: LotusViewModel = viewModel()) {
    val prefs = LocalContext.current.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    var boasVindas by rememberSaveable { mutableStateOf(!prefs.getBoolean(BOAS_VINDAS_VISTA, false)) }
    if (boasVindas) {
        BoasVindasTela(onComecar = {
            prefs.edit { putBoolean(BOAS_VINDAS_VISTA, true) }
            boasVindas = false
        })
        return
    }

    val sites by vm.sites.collectAsStateWithLifecycle()
    val pendentes by vm.pendentes.collectAsStateWithLifecycle()
    val areas by vm.areas.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.avisos.collect { avisos.showSnackbar(it) } }

    var aba by rememberSaveable { mutableStateOf(Aba.Inicio) }
    // Sem seletor de casa: cada APK mostra só a primeira casa do usuário (CASAS no .env).
    val casaId = CasasDoUsuario.first()
    BackHandler(enabled = aba != Aba.Inicio) { aba = Aba.Inicio }

    val casa = sites.first { it.id == casaId }
    val pendentesDaCasa = pendentes.filter { it.site == casaId }.map { it.comando }.toSet()
    val escondidas = areas[casaId]?.escondidas.orEmpty()

    // O fundo de cada tela é o que o vidro (cartões e barra) desfoca.
    val vidro = rememberHazeState()
    CompositionLocalProvider(LocalVidro provides vidro) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(avisos, Modifier.padding(bottom = EspacoDaBarra - 16.dp)) },
        ) { sistema ->
            // A barra flutua sobre o conteúdo: as telas deixam espaço para ela embaixo.
            val margens = PaddingValues(
                top = sistema.calculateTopPadding(),
                bottom = sistema.calculateBottomPadding() + EspacoDaBarra,
            )
            Box(Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = aba,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "abas",
                    modifier = Modifier.fillMaxSize(),
                ) { atual ->
                    when (atual) {
                        Aba.Inicio -> InicioTela(
                            casa = casa,
                            onVerZonas = { aba = Aba.Zonas },
                            escondidas = escondidas,
                            margens = margens,
                        )
                        Aba.Zonas -> ZonasTela(
                            casa = casa,
                            pendentes = pendentesDaCasa,
                            onEnviar = { vm.enviar(casa.id, it) },
                            escondidas = escondidas,
                            onRemoverArea = { vm.removerArea(casa.id, it) },
                            onAdicionarArea = { n, nome -> vm.adicionarArea(casa.id, n, nome) },
                            margens = margens,
                        )
                        Aba.Agenda -> AgendaTela(
                            casa = casa,
                            salvando = pendentesDaCasa.any { it is Comando.MudarAgenda },
                            onSalvar = { vm.enviar(casa.id, Comando.MudarAgenda(it)) },
                            margens = margens,
                        )
                    }
                }
                BarraNavegacao(
                    aba,
                    onEscolher = { aba = it },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = sistema.calculateBottomPadding()),
                )
            }
        }
    }
}
