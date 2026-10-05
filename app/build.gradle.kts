plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Um APK por usuário: cada env/<nome>.env vira o flavor <nome>, todos com o mesmo applicationId.
val pastaEnv: File = rootProject.file("env")
val arquivosEnv: List<File> = pastaEnv.listFiles { f -> f.isFile && f.extension == "env" }.orEmpty().sortedBy { it.name }
val chavesEnv = listOf("NOME_USUARIO", "ID_USUARIO")
// CASAS=esp,clp: as casas que o APK mostra. Mesmos valores de SiteId.topico (dados/Contrato.kt).
val casasValidas = listOf("esp", "clp")

if (arquivosEnv.isEmpty()) {
    throw GradleException("Nenhum .env em $pastaEnv. Copie env/exemplo.env.example para env/<usuario>.env.")
}

/** Lê `CHAVE=valor`, ignorando linhas vazias e comentários com `#`. */
fun lerEnv(arquivo: File): Map<String, String> = arquivo.readLines()
    .map { it.trim() }
    .filter { it.isNotEmpty() && !it.startsWith("#") }
    .associate { linha ->
        val partes = linha.split("=", limit = 2)
        if (partes.size != 2) throw GradleException("${arquivo.name}: linha sem '=': $linha")
        partes[0].trim() to partes[1].trim()
    }

/** Valor pronto para o `buildConfigField` (literal String de Java). */
fun String.comoLiteral() = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.lotus"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.lotus"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "usuario"
    productFlavors {
        arquivosEnv.forEach { arquivo ->
            val nome = arquivo.nameWithoutExtension
            if (!nome.matches(Regex("[a-z][A-Za-z0-9]*"))) {
                throw GradleException("${arquivo.name}: o nome do arquivo vira o flavor; use letras e números, começando por minúscula.")
            }
            val env = lerEnv(arquivo)
            create(nome) {
                dimension = "usuario"
                chavesEnv.forEach { chave ->
                    val valor = env[chave] ?: throw GradleException("${arquivo.name}: falta $chave")
                    buildConfigField("String", chave, valor.comoLiteral())
                }
                val casas = (env["CASAS"] ?: throw GradleException("${arquivo.name}: falta CASAS"))
                    .split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                if (casas.isEmpty() || casas.any { it !in casasValidas }) {
                    throw GradleException("${arquivo.name}: CASAS precisa ser uma lista de ${casasValidas.joinToString(", ")}, separadas por vírgula.")
                }
                buildConfigField("String[]", "CASAS", casas.joinToString(", ", "{", "}") { it.comoLiteral() })
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}