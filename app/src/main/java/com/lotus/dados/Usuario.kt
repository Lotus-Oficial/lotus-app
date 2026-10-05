package com.lotus.dados

import com.lotus.BuildConfig

/**
 * As casas deste APK, vindas do `CASAS` de env/<usuario>.env (o Gradle já validou os valores).
 * Fica na ordem de [SiteId], que é a da tela inicial.
 */
val CasasDoUsuario: List<SiteId> = SiteId.entries.filter { it.topico in BuildConfig.CASAS }
