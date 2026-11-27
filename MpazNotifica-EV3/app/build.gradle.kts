import java.util.Properties
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // KSP genera el código necesario de Room.
    alias(libs.plugins.ksp)

    // EV3: herramienta de cobertura de código.
    jacoco
}

jacoco {
    toolVersion = "0.8.12"
}

// =====================================================
// CONFIGURACIÓN DE FIRMA DEL APK
// =====================================================

// Las claves de firma se mantienen fuera del repositorio.
val archivoKeystore = rootProject.file("keystore.properties")

val propiedadesKeystore = Properties().apply {
    if (archivoKeystore.exists()) {
        archivoKeystore.inputStream().use {
            load(it)
        }
    }
}

// Busca las propiedades de firma.
fun propiedadDeFirma(nombre: String): String? =
    propiedadesKeystore.getProperty(nombre)
        ?.takeIf { it.isNotBlank() }
        ?: (project.findProperty(nombre) as String?)
            ?.takeIf { it.isNotBlank() }
        ?: System.getenv(nombre)
            ?.takeIf { it.isNotBlank() }

val firmaArchivo = propiedadDeFirma("MPAZ_STORE_FILE")
val firmaClaveAlmacen = propiedadDeFirma("MPAZ_STORE_PASSWORD")
val firmaAlias = propiedadDeFirma("MPAZ_KEY_ALIAS")
val firmaClaveAlias = propiedadDeFirma("MPAZ_KEY_PASSWORD")

// Comprueba que estén disponibles todos los datos de firma.
val firmaCompleta = listOf(
    firmaArchivo,
    firmaClaveAlmacen,
    firmaAlias,
    firmaClaveAlias
).all { it != null }

// =====================================================
// CONFIGURACIÓN DEL MODO LOCAL Y SERVIDOR
// =====================================================

// Dirección base del servidor.
val apiBaseUrl: String =
    (project.findProperty("API_BASE_URL") as String?)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: "http://10.0.2.2:3000/"

// Determina el modo inicial de funcionamiento.
val usarApi: Boolean =
    (project.findProperty("USAR_API") as String?)
        ?.trim()
        ?.lowercase() == "true"

// =====================================================
// CONFIGURACIÓN DE ANDROID
// =====================================================

android {

    namespace = "com.mpaznotifica.app"
    compileSdk = 35

    defaultConfig {

        applicationId = "com.mpaznotifica.app"

        minSdk = 24
        targetSdk = 35

        versionCode = 1
        versionName = "1.0.0"

        // Ejecutor de pruebas instrumentadas.
        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"$apiBaseUrl\""
        )

        buildConfigField(
            "boolean",
            "USAR_API",
            usarApi.toString()
        )
    }

    signingConfigs {

        if (firmaCompleta) {

            create("release") {

                storeFile = file(firmaArchivo!!)
                storePassword = firmaClaveAlmacen!!
                keyAlias = firmaAlias!!
                keyPassword = firmaClaveAlias!!
            }
        }
    }

    buildTypes {

        debug {

            // Cobertura de las pruebas unitarias JVM.
            enableUnitTestCoverage = true

            // EV3: cobertura de las pruebas del emulador.
            enableAndroidTestCoverage = true
        }

        release {

            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )

            // Aplica la firma cuando está configurada.
            if (firmaCompleta) {
                signingConfig =
                    signingConfigs.getByName("release")
            }
        }
    }

    // Utiliza la misma versión de JaCoCo en Android.
    testCoverage {
        jacocoVersion = "0.8.12"
    }

    compileOptions {

        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {

        jvmTarget = "11"
    }

    buildFeatures {

        compose = true
        buildConfig = true
    }

    // =================================================
    // EMPAQUETADO DE RECURSOS
    // =================================================

    packaging {

        resources {

            // Combina las licencias duplicadas.
            merges += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/NOTICE.md"
            )
        }
    }

    // =================================================
    // CONFIGURACIÓN DE PRUEBAS UNITARIAS
    // =================================================

    testOptions {

        unitTests.all {

            // Ejecuta JUnit 4, JUnit 5 y Kotest.
            it.useJUnitPlatform()

            // Muestra los resultados en la terminal.
            it.testLogging {

                events(
                    "passed",
                    "failed",
                    "skipped"
                )

                showExceptions = true
                showCauses = true
                showStackTraces = true
            }
        }
    }
}

// =====================================================
// DEPENDENCIAS DEL PROYECTO
// =====================================================

dependencies {

    // Navegación e iconos.
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)

    // =================================================
    // PERSISTENCIA ROOM
    // =================================================

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    ksp(libs.androidx.room.compiler)

    // =================================================
    // API REST
    // =================================================

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)

    // Registro HTTP únicamente en debug.
    debugImplementation(libs.okhttp.logging)

    implementation(libs.kotlinx.coroutines.android)

    // =================================================
    // JETPACK COMPOSE
    // =================================================

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // =================================================
    // PRUEBAS JUNIT 4 Y JUNIT 5
    // =================================================

    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    // =================================================
    // PRUEBAS KOTEST
    // =================================================

    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)

    // Simulaciones y pruebas de corrutinas.
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.mockk)

    // =================================================
    // PRUEBAS INSTRUMENTADAS
    // =================================================

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )

    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// =====================================================
// EV3: INFORME COMBINADO DE COBERTURA JACOCO
// =====================================================

tasks.register<JacocoReport>("jacocoTestReport") {

    group = "verification"

    description =
        "Genera la cobertura de pruebas unitarias e instrumentadas"

    // Ejecuta las pruebas unitarias antes del informe.
    dependsOn("testDebugUnitTest")

    reports {

        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true)

        html.outputLocation.set(
            layout.buildDirectory.dir(
                "reports/jacoco/html"
            )
        )

        xml.outputLocation.set(
            layout.buildDirectory.file(
                "reports/jacoco/cobertura.xml"
            )
        )

        csv.outputLocation.set(
            layout.buildDirectory.file(
                "reports/jacoco/cobertura.csv"
            )
        )
    }

    // Excluye clases generadas automáticamente.
    val exclusiones = listOf(
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*_Impl.class",
        "**/*_Impl\$*.class",
        "**/*ComposableSingletons*.*"
    )

    // Clases originales compiladas de Kotlin.
    val clasesKotlin = fileTree(
        layout.buildDirectory.dir(
            "tmp/kotlin-classes/debug"
        )
    ) {
        exclude(exclusiones)
    }

    // Clases originales compiladas de Java.
    val clasesJava = fileTree(
        layout.buildDirectory.dir(
            "intermediates/javac/debug/classes"
        )
    ) {
        exclude(exclusiones)
    }

    classDirectories.setFrom(
        files(
            clasesKotlin,
            clasesJava
        )
    )

    // Código fuente utilizado por el informe.
    sourceDirectories.setFrom(
        files(
            "src/main/java",
            "src/main/kotlin"
        )
    )

    // Resultados de las pruebas unitarias JVM.
    val coberturaUnitaria = fileTree(
        layout.buildDirectory
    ) {

        include(
            "jacoco/testDebugUnitTest.exec",
            "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
        )
    }

    // Resultados de las pruebas instrumentadas Android.
    val coberturaInstrumentada = fileTree(
        layout.buildDirectory
    ) {

        include(
            "outputs/code_coverage/debugAndroidTest/**/*.ec",
            "outputs/code_coverage/connected/**/*.ec",
            "outputs/connected_android_test_code_coverage/debugAndroidTest/**/*.ec"
        )
    }

    // Combina ambos tipos de cobertura.
    executionData.setFrom(
        coberturaUnitaria,
        coberturaInstrumentada
    )

    // Comprueba que existan resultados de ambos tipos.
    doFirst {

        check(coberturaUnitaria.files.isNotEmpty()) {
            "No se encontraron resultados de cobertura unitaria."
        }

        check(coberturaInstrumentada.files.isNotEmpty()) {
            "Ejecuta primero connectedDebugAndroidTest para generar la cobertura instrumentada."
        }

        println("Archivos de cobertura unitaria:")

        coberturaUnitaria.files.forEach {
            println(it.absolutePath)
        }

        println("Archivos de cobertura instrumentada:")

        coberturaInstrumentada.files.forEach {
            println(it.absolutePath)
        }
    }
}
