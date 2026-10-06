# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

App Android (Kotlin, Jetpack Compose + Material 3) do **Lótus**: acompanha e comanda a irrigação
de duas casas pelo broker MQTT. Código, comentários, commits e textos da interface em **português do Brasil**.

## Comandos

Há product flavors por usuário (veja abaixo), então as tarefas levam o nome do flavor:

```sh
./gradlew assembleFernandoDebug                 # APK de um usuário
./gradlew assembleFernandoRelease               # assinado se existir assinatura/ (veja abaixo)
./gradlew gerarReleases                         # release de todos os flavors + versão e assinatura de cada APK
./gradlew testFernandoDebugUnitTest             # testes de unidade
./gradlew testFernandoDebugUnitTest --tests "com.lotus.FormatosTest.duracaoLegivel"   # um teste só
./gradlew lintFernandoDebug
```

- A pasta `Lótus` tem acento: o build depende de `android.overridePathCheck=true` (`gradle.properties`),
  e os testes de unidade **não rodam** nesta pasta no Windows ("Could not execute test class": o acento quebra
  o classpath). Para rodá-los, copie o projeto para um caminho sem acento.
- O emulador desta máquina não sobe. Para ver telas, renderize os `@Preview` (detalhes na skill `lotus-telas`).

## Release

1. Aumente `versionCode` (inteiro, sempre maior que o instalado; senão o celular não atualiza) e `versionName`
   (texto que a pessoa vê) no `defaultConfig` de `app/build.gradle.kts`. Valem para todos os flavors.
2. `./gradlew gerarReleases`: roda `assembleRelease` e, para cada flavor, lê o APK com `aapt2` (versão) e
   `apksigner` (certificado). Saída esperada: `CN=Lotus, O=Projeto Lotus, C=BR`, SHA-256 `907e8a01…7069dc`.
   Outro SHA-256 = outra chave, e os celulares recusam a atualização.
3. APKs em `app/build/outputs/apk/<flavor>/release/app-<flavor>-release.apk`. Mesmo `applicationId` em todos:
   instalar o de um flavor substitui o de outro no mesmo aparelho.

R8 desligado no release (`optimization { enable = false }`): com `packageScope` incluindo `kotlin.**`, ele tirava
`CollectionsKt__MutableCollectionsJVMKt` do pacote `kotlin.collections` e o app fechava ao abrir com
`IllegalAccessError`. Ao religar, deixe `kotlin.**` fora do `packageScope` e teste o release num aparelho antes de distribuir.

Assinatura: `assinatura/assinatura.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`, com
`storeFile` relativo à pasta) e `assinatura/lotus-release.jks`, ambos fora do git e com backup fora da máquina.
**Nunca gere outra keystore**: em máquina nova, restaure a pasta do backup. Sem o arquivo, o release sai
`-unsigned` (e `gerarReleases` mostra "SEM ASSINATURA"); o debug não depende dele.

## Um APK por usuário (`env/`)

`app/build.gradle.kts` lê `env/*.env` na configuração do Gradle: cada `env/<nome>.env` vira o
flavor `<nome>` (dimensão `usuario`, mesmo `applicationId` `com.lotus`) e suas chaves
`NOME_USUARIO` e `ID_USUARIO` viram `BuildConfig.NOME_USUARIO` / `BuildConfig.ID_USUARIO`.
`CASAS=esp,clp` diz quais casas o APK mostra: vira `BuildConfig.CASAS` (`String[]`, validado contra
`casasValidas` no Gradle) e, no app, `CasasDoUsuario` (`dados/Usuario.kt`), que alimenta o repositório
e a casa inicial. Casa nova no contrato: acrescentar em `SiteId` e em `casasValidas`.

- `env/*.env` fica fora do git; só `env/exemplo.env.example` é versionado. Sem nenhum `.env`, o build falha de propósito.
- Usuário novo: copiar o modelo para `env/<nome>.env` (letras e números, começando por minúscula). Não precisa editar o Gradle.
- Chave nova: acrescentar em `chavesEnv` no `app/build.gradle.kts`, no modelo e em todos os `.env`.
- `BuildConfig` é legível num APK descompilado: não serve para senha do broker.

## Arquitetura

O "backend" é o repositório irmão `../lotus-bkd`: dois quadros ESP32 (sites `esp` e `clp`) que falam
com o app por um broker MQTT na nuvem (EMQX, TLS 8883). Não há API HTTP.

- **Contrato**: `../lotus-bkd/docs/CONTRATO-MQTT.md` (`lotus/v1`). `dados/Contrato.kt` é o espelho dele em
  português, com o nome do campo JSON em comentário. Não invente campo, comando ou estado fora do contrato.
- **Usuários e permissões do broker**: `../lotus-bkd/broker/README.md` (`app-esp`, `app-clp`, `app-admin`). O EMQX
  nega a inscrição inteira se o filtro for mais amplo que o permitido, então assine `lotus/v1/{site}/#` por site.
- **Fonte de dados**: as telas só conhecem `LotusRepositorio` (`sites: StateFlow<List<Site>>` e
  `enviar(site, comando): Resposta`). Hoje a implementação é `SimuladorLotus`, que imita os quadros
  (mensagens retidas, `state` a cada 15 s, mesmos erros de `ack`, timeout de ~10 s); o cliente MQTT de
  verdade ainda não existe (nem dependência MQTT/JSON nem permissão de internet no manifest).
  A troca é feita em `ui/LotusViewModel.kt`.
- **Fluxo de comando**: `LotusViewModel.enviar` marca o comando como `Pendente` (o botão entra em progresso),
  espera a `Resposta` e só emite aviso (snackbar) em caso de erro: o novo estado na tela já é a confirmação.
- **Situação da casa**: `ui/Resumo.kt` reduz um `Site` a um `Resumo` com prioridade fixa
  (carregando/fora do ar → falha → manual → modo) e desconta localmente o `remainingS` desde o último `state`.
- **Textos**: tempos, datas e saudação em `ui/Formatos.kt` (fuso fixo `America/Sao_Paulo`, coberto por
  `FormatosTest`); mensagens de erro e falha em `ui/Textos.kt`, sempre em termos do quintal, não do hardware.
- **Navegação**: `MainActivity.kt` mostra Boas-vindas na primeira abertura (flag em SharedPreferences) e
  depois três abas (Início, Áreas, Agenda; "área" é o nome que a tela dá à zona do contrato) da casa escolhida, sem biblioteca de navegação.
- **Nome do usuário nas telas**: vem de `BuildConfig.NOME_USUARIO` (Boas-vindas e cabeçalho do Início).
  Ainda fixos no código: nomes das casas em `SiteId` (`Contrato.kt`) e zonas e agendas do simulador.

## Design

Antes de criar ou mudar tela, componente ou cor, use a skill **`lotus-telas`** (`.claude/skills/lotus-telas/SKILL.md`):
ela tem a paleta, a tipografia, os componentes existentes, os estados obrigatórios e as decisões já tomadas.
A referência visual é o canvas "Lótus — App"; os tokens ficam em `ui/tema/`, nunca cor solta na tela.
Previews usam `@PreviewLotus` (claro + escuro) com as casas de `ui/Exemplos.kt`.

## Commits

Pela skill `commit`: Conventional Commits com corpo em PT-BR, sem `Co-Authored-By`.
