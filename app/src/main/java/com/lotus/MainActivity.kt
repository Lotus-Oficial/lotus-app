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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.lotus.ui.inicio.InicioTela
import com.lotus.ui.tema.LotusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LotusTheme { LotusApp() } }
    }
}

private const val PREFS = "lotus"
private const val BOAS_VINDAS_VISTA = "boas_vindas_vista"

/** Boas-vindas na primeira vez; depois três abas (Início, Zonas, Agenda) da casa escolhida. */
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
    val avisos = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.avisos.collect { avisos.showSnackbar(it) } }

    var aba by rememberSaveable { mutableStateOf(Aba.Inicio) }
    var casaId by rememberSaveable { mutableStateOf(CasasDoUsuario.first()) }
    BackHandler(enabled = aba != Aba.Inicio) { aba = Aba.Inicio }

    val casa = sites.first { it.id == casaId }
    val pendentesDaCasa = pendentes.filter { it.site == casaId }.map { it.comando }.toSet()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { BarraNavegacao(aba, onEscolher = { aba = it }) },
        snackbarHost = { SnackbarHost(avisos) },
    ) { margens ->
        AnimatedContent(
            targetState = aba,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "abas",
            modifier = Modifier.fillMaxSize(),
        ) { atual ->
            when (atual) {
                Aba.Inicio -> InicioTela(
                    sites = sites,
                    casa = casa,
                    onEscolherCasa = { casaId = it.id },
                    onVerZonas = { aba = Aba.Zonas },
                    margens = margens,
                )
                Aba.Zonas -> ZonasTela(
                    sites = sites,
                    casa = casa,
                    pendentes = pendentesDaCasa,
                    onEscolherCasa = { casaId = it.id },
                    onEnviar = { vm.enviar(casa.id, it) },
                    margens = margens,
                )
                Aba.Agenda -> AgendaTela(
                    sites = sites,
                    casa = casa,
                    salvando = pendentesDaCasa.any { it is Comando.MudarAgenda },
                    onEscolherCasa = { casaId = it.id },
                    onSalvar = { vm.enviar(casa.id, Comando.MudarAgenda(it)) },
                    margens = margens,
                )
            }
        }
    }
}
