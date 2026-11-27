package com.mpaznotifica.app

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.mpaznotifica.app.data.repository.PreferenciasModo

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * EV3: pruebas instrumentadas de las preferencias.
 *
 * Comprueba el cambio entre modo local y servidor,
 * la persistencia y la actualización del StateFlow.
 *
 * Utiliza preferencias independientes de la aplicación.
 */
@RunWith(AndroidJUnit4::class)
class PreferenciasModoTest {

    private lateinit var context: Context
    private lateinit var preferencias: SharedPreferences

    // Nombre independiente para las pruebas.
    private val archivoPruebas = "mpaz_servidor_test"

    @Before
    fun prepararPreferencias() {

        // Obtiene un contexto válido de Android.
        val contextoBase = InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .applicationContext

        // Redirige las preferencias a un archivo de pruebas.
        context = object : ContextWrapper(contextoBase) {

            override fun getApplicationContext(): Context {
                return this
            }

            override fun getSharedPreferences(
                name: String,
                mode: Int
            ): SharedPreferences {

                return contextoBase.getSharedPreferences(
                    archivoPruebas,
                    mode
                )
            }
        }

        preferencias = context.getSharedPreferences(
            archivoPruebas,
            Context.MODE_PRIVATE
        )

        // Limpia únicamente las preferencias de pruebas.
        preferencias.edit().clear().commit()
    }

    @After
    fun limpiarPreferencias() {

        // No modifica las preferencias reales de la app.
        preferencias.edit().clear().commit()
    }

    // Comprueba el modo local predeterminado.
    @Test
    fun modoInicialLocalSinPreferencias() {

        val modo = PreferenciasModo(
            context,
            inicialLocal = true
        )

        assertTrue(modo.esLocal())
        assertTrue(modo.modoLocal.value)
    }

    // Comprueba el modo servidor predeterminado.
    @Test
    fun modoInicialServidorSinPreferencias() {

        val modo = PreferenciasModo(
            context,
            inicialLocal = false
        )

        assertFalse(modo.esLocal())
        assertFalse(modo.modoLocal.value)
    }

    // Comprueba el cambio de modo local a servidor.
    @Test
    fun cambiarDeLocalAServidor() {

        val modo = PreferenciasModo(context, true)

        modo.cambiar(false)

        assertFalse(modo.esLocal())
        assertFalse(modo.modoLocal.value)
    }

    // Comprueba el cambio de servidor a local.
    @Test
    fun cambiarDeServidorALocal() {

        val modo = PreferenciasModo(context, false)

        modo.cambiar(true)

        assertTrue(modo.esLocal())
        assertTrue(modo.modoLocal.value)
    }

    // Comprueba que el modo servidor quede almacenado.
    @Test
    fun conservaModoServidorEnNuevaInstancia() {

        val primera = PreferenciasModo(context, true)

        primera.cambiar(false)

        val segunda = PreferenciasModo(context, true)

        assertFalse(segunda.esLocal())
        assertFalse(segunda.modoLocal.value)
    }

    // Comprueba que el modo local quede almacenado.
    @Test
    fun conservaModoLocalEnNuevaInstancia() {

        val primera = PreferenciasModo(context, false)

        primera.cambiar(true)

        val segunda = PreferenciasModo(context, false)

        assertTrue(segunda.esLocal())
        assertTrue(segunda.modoLocal.value)
    }

    // Un cambio de configuración inicial invalida
    // la elección guardada anteriormente.
    @Test
    fun cambioDeConfiguracionInicialInvalidaModoAnterior() {

        val anterior = PreferenciasModo(context, true)

        anterior.cambiar(true)

        val nuevo = PreferenciasModo(context, false)

        assertFalse(nuevo.esLocal())
        assertFalse(nuevo.modoLocal.value)
    }

    // Comprueba el cambio de configuración a modo local.
    @Test
    fun cambioDeConfiguracionInicialPermiteModoLocal() {

        val anterior = PreferenciasModo(context, false)

        anterior.cambiar(false)

        val nuevo = PreferenciasModo(context, true)

        assertTrue(nuevo.esLocal())
        assertTrue(nuevo.modoLocal.value)
    }

    // Comprueba que ambas preferencias se almacenen.
    @Test
    fun guardaModoElegidoYConfiguracionInicial() {

        val modo = PreferenciasModo(context, true)

        modo.cambiar(false)

        assertTrue(
            preferencias.contains("modo_local")
        )

        assertTrue(
            preferencias.contains("modo_local_inicial")
        )

        assertFalse(
            preferencias.getBoolean(
                "modo_local",
                true
            )
        )

        assertTrue(
            preferencias.getBoolean(
                "modo_local_inicial",
                false
            )
        )
    }

    // Comprueba varios cambios consecutivos.
    @Test
    fun permiteCambiarModoVariasVeces() {

        val modo = PreferenciasModo(context, true)

        modo.cambiar(false)

        assertFalse(modo.modoLocal.value)

        modo.cambiar(true)

        assertTrue(modo.modoLocal.value)

        modo.cambiar(false)

        assertFalse(modo.modoLocal.value)
        assertFalse(modo.esLocal())
    }

    // Comprueba que se consulte el valor almacenado.
    @Test
    fun esLocalConsultaElValorAlmacenado() {

        val modo = PreferenciasModo(context, true)

        preferencias.edit()
            .putBoolean("modo_local", false)
            .putBoolean("modo_local_inicial", true)
            .commit()

        assertFalse(modo.esLocal())
    }
}
