---
name: lotus-telas
description: Designer das telas do app Lótus (Android, Kotlin). Use sempre que for criar, redesenhar ou ajustar uma tela, componente ou fluxo do app — "nova tela", "tela de zonas", "layout", "design", "mockup", "protótipo", "cores", "ícone", "estado de erro/carregamento". Garante identidade visual (petróleo, creme, dourado e lilás do canvas "Lótus — App"), linguagem e estados consistentes com o contrato MQTT lotus/v1.
---

# Lótus — designer de telas

Você está desenhando telas do app Android do **Lótus**: irrigação automática de duas casas
operadas por um único app que fala o contrato MQTT `lotus/v1`:

- **Casa do Fernando** = site **ESP** (ESP32 + relés, 7 zonas) — é a 1ª casa.
- **Casa do Felipe** = site **CLP** (CLP Delta + inversor, 5 zonas, pressão e vazão).

Lema: **"O quintal se rega sozinho. Você só acompanha."** O app é de acompanhar, não de
operar o tempo todo: a informação mais importante é *o que está acontecendo agora*.

## Antes de desenhar

1. **Fonte da verdade do visual: o canvas "Lótus — App"**
   (https://claude.ai/artifact/5Xoe1wWhiVDjNoU5sNxMGe). Leia os `.dc.html` dele com a
   ferramenta Artifact (`read` com `path`) antes de criar uma tela nova ou mudar o visual.
   Se o canvas e esta skill discordarem, vale o canvas — e atualize esta skill.
2. Leia o que já existe em `lotus-app` (tema, componentes, telas) e reaproveite. Nunca crie
   uma segunda versão de um componente que já existe.
3. Confira os dados que a tela vai mostrar no contrato: `../lotus-bkd/docs/CONTRATO-MQTT.md`.
   Não invente campo, comando ou estado que o contrato não tem. Se o canvas mostrar algo
   fora do contrato, **aponte para a usuária** e use o dado real mais próximo.
4. Se o pedido for ambíguo (qual casa, qual fluxo), pergunte uma vez e siga.

**Versão aprovada pela usuária** (outubro de 2026): as telas da página
https://claude.ai/artifact/CmByXf4jV7pi3XXyWda1w8 (Boas-vindas, Início, Zonas, Agenda,
claro e escuro). Telas novas devem parecer da mesma família que essas.

## Identidade visual (do canvas)

**Cores** — tokens em `ui/tema/Cores.kt`:

| Nome | Cor | Uso |
|---|---|---|
| Petróleo | `#1F5F73` | `primary`, texto principal, cartão de destaque, linha da zona regando |
| Creme | `#F5F3EE` | Fundo das telas; texto sobre petróleo |
| **Dourado** | `#C9A54C` | Botão de destaque ("Começar", "Ver zonas", "Regar tudo"), traço sob títulos, anel de progresso, número da zona atual, tracinho da aba aberta |
| Dourado claro | `#E4C77E` | Rótulos sobre o petróleo ("Regando agora", "Próxima irrigação"), botão de contorno no destaque |
| Dourado suave / texto | `#F1E7CC` / `#7A5C17` | Fundo e texto de coisas douradas claras (botão tracejado, ícone das zonas) |
| **Lilás** | `#B3A4D6` | Pílula "Irrigando", zona feita (✓), círculo de enfeite, caixa d'água, dias marcados da Agenda |
| Lilás suave / texto | `#E6E0F3` / `#5A4A8A`, sobre lilás `#2E2347` | Texto e fundos lilases |
| Texto secundário | `#3E5A63`, `#5B7178` | Legendas, rótulos |
| Bordas | `#E6E2D8` (cartões), `#C9D3D6` (campos) | Cartões brancos com 1 dp de borda |
| Verde | `#2E9D6B` | Só "online" e "tudo certo" |

**Tema escuro** (o canvas só tem o claro; derivado dele): fundo `#0C1F25`, cartões
`#12303A` (mais claros que o fundo), texto `#EEF0EA`, dourado claro `#E4C77E`, lilás
`#CFC4EC`. O cartão de destaque e a Boas-vindas ficam petróleo nos dois temas.

**Tipografia** — `ui/tema/Tipografia.kt`, fontes em `res/font`:
- **Outfit** (Bold/SemiBold): títulos, nomes de zona, botões.
- **Nunito Sans**: texto corrido.
- **IBM Plex Mono**: números e rótulos técnicos — contagem regressiva, horários,
  "CHUVA", "RESTANTE", "ESP32 · 7 zonas", "2/7 feitas", "12 min".

**Formas**: botões em pílula (altura 56, ou 44 dentro do destaque); cartões com cantos 20;
destaque 24; linhas e campos 14; folhas 28. Alvos de toque ≥ 48 dp.

**Padrões do canvas**:
- **Cartão de destaque** petróleo com **anel dourado** e **círculo lilás** no canto
  (`CartaoDestaque`). Texto em ~66% da largura para não passar por baixo dos enfeites.
  Quando tem o anel de progresso, o enfeite vira só um círculo lilás apagado (`anel = false`).
- **Traço dourado** de 4 dp embaixo dos títulos grandes (`TracoDourado`).
- **Anel de progresso** dourado com miolo escuro e o tempo em mono (`AnelProgresso`).
- **Sequência de zonas**: feita = quadradinho lilás com ✓ e "feito"; regando = linha
  petróleo com borda dourada, número em dourado e o tempo; esperando = número cinza e "12 min".
- **Quadradinhos de sensor** com rótulo mono em caixa alta.
- **Botão tracejado dourado** para acrescentar algo a uma lista ("Adicionar horário").
- **Barra de baixo** com ícone, nome e tracinho dourado sob a aba aberta.
- Logo pequena num quadradinho creme (`LogoPequeno`), para aparecer no tema escuro.
- **Animação de clima** (prancheta "Animação de clima" e fundo do Início no canvas): sol dourado
  no canto de cima com raios girando, nuvens passando, e nuvens cinza com gotas na chuva.
  `FundoClima(tempo)` em `ui/componentes/`, cores em `Lotus.clima`. Só no Início, atrás da lista.

Regras:
- Tudo como token no tema; nunca cor solta na tela.
- **Tema claro e escuro** desde a primeira versão.
- Contraste mínimo 4.5:1 para texto; estado nunca só por cor — sempre texto (e ícone quando couber).
- Sem cor dinâmica do Android 12+.

## Linguagem

- Tudo em **português do Brasil**, tom calmo e direto: "Regando agora", "Próxima irrigação",
  "Sem água na caixa".
- Fale em termos do quintal, não do hardware: "zona", "ciclo", "agenda", "chuva"; evite
  "relé", "Modbus", "QoS", "payload". Exceção do canvas: a linha mono do topo da aba
  Zonas diz o tipo de quadro ("ESP32", "CLP Delta").
- Tempos legíveis: `412 s` → "6 min 52 s" ou "6:52" na contagem; `360` → "06:00";
  `days: 62` → "seg a sex". Títulos de data: "Hoje, 18:00", "Amanhã, 05:30", "Quarta, 06:00".

## Estados que toda tela precisa ter

- **Carregando**: esqueleto com o formato do conteúdo, não spinner solto.
- **Quadro fora do ar**: faixa no topo, último estado conhecido esmaecido (alpha ~0.55),
  "visto há X min", comandos desligados.
- **Comando enviado**: botão em progresso até o `ack`; erro vira mensagem humana
  (`fault_dry` → "Sem água na caixa", `manual` → "O quadro está no modo manual").
- **Falhas** (`dry`, `inverter`, `no_flow`, `watchdog`): cartão de alerta no topo com o que fazer.
- **Vazio**: agenda desligada, nenhum horário etc. — explique e ofereça a ação.

## Estrutura do app

- **Boas-vindas** (só na primeira abertura) → três abas: **Início**, **Zonas**, **Agenda**.
- **Sem seletor de casa** nas abas (pedido da usuária, outubro de 2026): no lugar dele fica um
  espaço vazio de 44 dp no topo de Início, Zonas e Agenda (no Início, deixa a animação de
  clima aparecer). Cada APK mostra só a primeira casa de `CASAS`; hoje cada `.env` tem uma casa.
  `SeletorCasa` continua em `Navegacao.kt`, sem uso, para quando um APK tiver duas casas.
- Fora do contrato, por isso **fora do app** até existirem: login/conta e "Perfil",
  "Adicionar zona" (as zonas são fixas no quadro), umidade do solo, litros por dia.
  Onde o canvas mostra isso, use chuva, caixa d'água, próxima rega, pressão e vazão.
- Ações que interrompem (trocar de zona regando, parar tudo) pedem confirmação curta.

## Como entregar um design

1. **Resumo**: objetivo da tela, de onde vêm os dados (tópicos/campos do contrato).
2. **Mockup**: se for tela nova, desenhe no canvas "Lótus — App" ou em ASCII.
3. **Estados**: todos os estados acima aplicados a essa tela.
4. **Implementação**: Jetpack Compose + Material 3, componentes pequenos, `@PreviewLotus`
   (claro + escuro) para cada estado relevante.

## O que já existe (reaproveite)

Código em `app/src/main/java/com/lotus/`:

- `ui/tema/`: `Cores.kt` (esquemas, `CoresDestaque`, `CoresDeEstado` com
  `regando|ok|atencao|agua`), `Tema.kt` (`LotusTheme`, `Lotus.estado`, `Lotus.destaque`),
  `Tipografia.kt` (`Outfit`, `NunitoSans`, `PlexMono`, `NumeroGrande`, `NumeroMedio`,
  `RotuloMono`, `TextoMono`).
- `ui/componentes/`: `Componentes.kt` (`Pilula`, `Cartao`, `CartaoDestaque`, `AnelProgresso`,
  `TracoDourado`, `CartaoAlerta`, `FaixaForaDoAr`, `Esqueleto`, `BotaoComando` com estilos
  `Principal|Dourado|Contorno|ContornoNoDestaque`, `TituloSecao`, `LogoPequeno`, `agoraMs()`,
  `@PreviewLotus`), `Navegacao.kt` (`BarraNavegacao`, `SeletorCasa`, `PontoDeSituacao`),
  `PilulaResumo.kt`.
- `ui/Resumo.kt`: `resumo(site, agora)` decide a situação da casa (prioridade: fora do ar →
  falha → manual → modo). `ui/Formatos.kt` e `ui/Textos.kt`: todo texto de tempo, dia e erro.
- `ui/Exemplos.kt`: uma casa de exemplo por estado, para os `@Preview`.
- Telas: `boasvindas/BoasVindasTela`, `inicio/InicioTela`, `casa/ZonasTela`
  (+ `Folhas.kt`: zona e adiar por chuva), `agenda/AgendaTela`.
  Dados: `dados/Contrato.kt` (`SiteId.casa` tem o nome da casa), `LotusRepositorio`, `SimuladorLotus`.

## Decisões já tomadas

- Texto colorido de estado usa `tom.texto` sobre `tom.fundo`; `tom.cor` só em ícone.
- Falha ou caixa baixa: desliga os botões de regar (o quadro recusaria com `fault_dry`),
  mas deixa adiar e editar. Dourado desligado fica translúcido, ainda visível no petróleo.
- "Bomba" é derivada (liga junto com qualquer zona aberta): o contrato não publica a bomba.
  Valores curtos para caber no quadradinho: "ligada" / "parada".
- "x/7 feitas" só aparece quando dá para saber pelo contrato: a próxima zona é a seguinte da
  ativa (ciclo em ordem). Rega de uma zona só não mostra sequência.
- Nome longo de casa: o seletor e os títulos usam uma linha com reticências.
- Dias por extenso em datas ("quarta às 06:00"); abreviados só em listas ("seg, qua, sex").
- Botões dentro do destaque: texto curto ("Parar", não "Parar tudo") para caber em uma linha.
- Tempo da animação (`tempo(site)` em `ui/Clima.kt`), outubro de 2026: por enquanto só o sensor
  de chuva (molhado = chuva, seco = sol); fora do ar ou sem sensores, sem animação. "Nublado"
  espera a previsão do tempo (Open-Meteo, por coordenada da casa), que fica para quando o app
  tiver internet. A animação é enfeite: o estado continua escrito no cartão "Chuva".
  Com as animações do Android desligadas, fica parada.
- Dias marcados da Agenda: círculo lilás (`tertiary`) com letra `onTertiary`, pedido da usuária
  (outubro de 2026). Desmarcado continua branco com borda.

## Conferir visualmente

O emulador desta máquina não sobe (virtualização desligada na BIOS). Para ver as telas,
renderize os `@Preview` com o plugin `com.android.compose.screenshot`
(`updateDebugScreenshotTest`) numa cópia do projeto em caminho **sem acento**: a pasta
`Lótus` quebra o classpath dos testes no Windows.

Se uma decisão nova de design surgir (cor, componente, padrão), registre-a nesta skill para
que as próximas telas sigam a mesma regra.
