import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.inject.Inject
import org.gradle.process.ExecOperations

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

// Assinatura do release: assinatura/assinatura.properties + .jks, fora do git. Sem eles o release sai "-unsigned".
val arquivoAssinatura: File = rootProject.file("assinatura/assinatura.properties")
val assinatura: Properties? = arquivoAssinatura.takeIf { it.isFile }?.let { f ->
    val props = Properties()
    f.inputStream().use { props.load(it) }
    props
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
        versionCode = 2
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

    signingConfigs {
        assinatura?.let { props ->
            create("release") {
                fun chave(nome: String): String = props.getProperty(nome)
                    ?: throw GradleException("${arquivoAssinatura.name}: falta $nome")
                storeFile = arquivoAssinatura.resolveSibling(chave("storeFile"))
                storePassword = chave("storePassword")
                keyAlias = chave("keyAlias")
                keyPassword = chave("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            optimization {
                // Desligado: o R8 move classes do kotlin.collections para fora do pacote e o app fecha
                // com IllegalAccessError ao abrir. Religar só depois de testar o release num celular.
                enable = false
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

/** Lê versão (aapt2) e assinatura (apksigner) do APK release de cada flavor. */
abstract class ConferirReleases : DefaultTask() {
    @get:Inject abstract val exec: ExecOperations
    @get:Internal abstract val pastaApks: DirectoryProperty
    @get:Internal abstract val flavors: ListProperty<String>
    @get:Internal abstract val buildTools: DirectoryProperty

    @TaskAction
    fun conferir() {
        val windows = System.getProperty("os.name").startsWith("Windows")
        val aapt2 = buildTools.file(if (windows) "aapt2.exe" else "aapt2").get().asFile
        val apksigner = buildTools.file(if (windows) "apksigner.bat" else "apksigner").get().asFile

        flavors.get().forEach { flavor ->
            val apk = pastaApks.dir("$flavor/release").get().asFile
                .listFiles { f -> f.extension == "apk" }?.singleOrNull()
                ?: throw GradleException("Nenhum APK release de $flavor. Rode assembleRelease antes.")
            val versao = Regex("versionCode='(\\d+)' versionName='([^']*)'")
                .find(rodar(aapt2, "dump", "badging", apk.path))?.destructured
            val certificado = rodar(apksigner, "verify", "--print-certs", apk.path)
            val dono = Regex("certificate DN: (.+)").find(certificado)?.groupValues?.get(1)?.trim()
            val sha256 = Regex("certificate SHA-256 digest: (\\w+)").find(certificado)?.groupValues?.get(1)

            logger.lifecycle("")
            logger.lifecycle("== $flavor: ${apk.name}")
            logger.lifecycle("   versão:     " + (versao?.let { (codigo, nome) -> "$nome (versionCode $codigo)" } ?: "não deu para ler"))
            logger.lifecycle("   assinatura: " + (dono?.let { "$it, SHA-256 $sha256" } ?: "SEM ASSINATURA (falta assinatura/assinatura.properties)"))
        }
    }

    private fun rodar(programa: File, vararg argumentos: String): String {
        val saida = ByteArrayOutputStream()
        exec.exec {
            commandLine(listOf(programa.path) + argumentos)
            standardOutput = saida
            errorOutput = saida
            isIgnoreExitValue = true
        }
        return saida.toString(Charsets.UTF_8)
    }
}

// ./gradlew gerarReleases: gera o release de todos os flavors e mostra versão e assinatura de cada APK.
tasks.register<ConferirReleases>("gerarReleases") {
    group = "lotus"
    description = "Gera o APK release de cada flavor e confere versão e assinatura."
    dependsOn("assembleRelease")
    pastaApks.set(layout.buildDirectory.dir("outputs/apk"))
    flavors.set(arquivosEnv.map { it.nameWithoutExtension })
    buildTools.set(androidComponents.sdkComponents.sdkDirectory.map { it.dir("build-tools/${android.buildToolsVersion}") })
    outputs.upToDateWhen { false }
}