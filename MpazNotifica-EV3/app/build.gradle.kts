import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // KSP genera el código necesario de Room.
    alias(libs.plugins.ksp)
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

        release {

            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )

            // Aplica la firma únicamente cuando está configurada.
            if (firmaCompleta) {
                signingConfig =
                    signingConfigs.getByName("release")
            }
        }
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
    // EV3: EMPAQUETADO DE RECURSOS
    // =================================================

    packaging {

        resources {

            // Combina las licencias duplicadas de las dependencias.
            // Evita conflictos al generar el APK de pruebas.
            merges += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/NOTICE.md"
            )
        }
    }

    // =================================================
    // EV3: CONFIGURACIÓN DE JUNIT 5
    // =================================================

    testOptions {

        unitTests.all {

            // Ejecuta JUnit 5 y las pruebas antiguas con Vintage.
            it.useJUnitPlatform()

            // Muestra los resultados de las pruebas en la terminal.
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

    // Navegación entre pantallas.
    implementation(libs.androidx.navigation.compose)

    // Íconos de Material Design.
    implementation(libs.androidx.material.icons.extended)

    // =================================================
    // PERSISTENCIA ROOM
    // =================================================

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // Genera las implementaciones de DAO y Room.
    ksp(libs.androidx.room.compiler)

    // =================================================
    // API REST
    // =================================================

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)

    // Registro HTTP disponible únicamente en debug.
    debugImplementation(libs.okhttp.logging)

    // Corrutinas para operaciones asíncronas.
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
    // PRUEBAS UNITARIAS JVM
    // =================================================

    // Conserva las pruebas originales de JUnit 4.
    testImplementation(libs.junit)

    // EV3: nuevas pruebas con JUnit 5.
    testImplementation(libs.junit.jupiter)

    // Permite ejecutar JUnit 4 dentro de JUnit Platform.
    testRuntimeOnly(libs.junit.vintage.engine)

    // Ejecutor de la plataforma de pruebas.
    testRuntimeOnly(libs.junit.platform.launcher)

    // Corrutinas, simulaciones HTTP y objetos de prueba.
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.mockk)

    // =================================================
    // PRUEBAS INSTRUMENTADAS EN ANDROID
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
