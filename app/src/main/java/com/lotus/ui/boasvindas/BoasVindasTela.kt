package com.lotus.ui.boasvindas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lotus.BuildConfig
import com.lotus.R
import com.lotus.dados.CasasDoUsuario
import com.lotus.ui.componentes.BotaoComando
import com.lotus.ui.componentes.EstiloBotao
import com.lotus.ui.componentes.PreviewLotus
import com.lotus.ui.componentes.TracoDourado
import com.lotus.ui.componentes.bordaDeVidro
import com.lotus.ui.tema.Lotus
import com.lotus.ui.tema.LotusTheme

/** Primeira tela, só na primeira vez que o app abre. Petróleo nos dois temas, como no canvas. */
@Composable
fun BoasVindasTela(onComecar: () -> Unit, modifier: Modifier = Modifier) {
    val d = Lotus.destaque
    Box(
        modifier
            .fillMaxSize()
            .background(d.fundo),
    ) {
        // Enfeites do canvas: anel dourado e círculo lilás saindo pela direita.
        Spacer(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = 70.dp)
                .size(150.dp)
                .border(6.dp, d.dourado, CircleShape),
        )
        Spacer(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 110.dp, y = 170.dp)
                .size(240.dp)
                .background(d.lilas.copy(alpha = 0.9f), CircleShape),
        )

        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(start = 32.dp, end = 32.dp, top = 72.dp, bottom = 48.dp),
        ) {
            Box(
                Modifier
                    .size(208.dp)
                    .background(d.texto.copy(alpha = 0.72f), CircleShape)
                    .bordaDeVidro(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painterResource(R.drawable.lotus_icone),
                    contentDescription = "Símbolo do Lótus",
                    modifier = Modifier.size(150.dp),
                )
            }

            Spacer(Modifier.weight(1f))

            // Texto num cartão de vidro petróleo, como no canvas.
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(d.fundo.copy(alpha = 0.7f))
                    .bordaDeVidro(MaterialTheme.shapes.extraLarge)
                    .padding(24.dp),
            ) {
                Text("${BuildConfig.NOME_USUARIO}, boas-vindas ao", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 22.sp), color = d.texto)
                Text("projeto Lótus", style = MaterialTheme.typography.headlineLarge, color = d.texto)
                TracoDourado(largura = 96.dp)
                Text(
                    "Acompanhe a irrigação ${if (CasasDoUsuario.size == 1) "da sua casa" else "das suas casas"}, " +
                        "a chuva e a caixa d'água de onde você estiver.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.Normal),
                    color = d.textoSuave,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(Modifier.size(20.dp))
            BotaoComando(
                texto = "Começar",
                icone = null,
                enviando = false,
                onClick = onComecar,
                estilo = EstiloBotao.DouradoClaro,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewLotus
@Composable
private fun BoasVindasPreview() {
    LotusTheme { BoasVindasTela(onComecar = {}) }
}
