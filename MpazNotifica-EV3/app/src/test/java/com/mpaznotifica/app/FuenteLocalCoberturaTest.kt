package com.mpaznotifica.app

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.data.remote.ApiResult
import com.mpaznotifica.app.data.remote.CausaError
import com.mpaznotifica.app.data.repository.AlmacenLocal
import com.mpaznotifica.app.data.repository.FuenteLocal
import com.mpaznotifica.app.domain.ReglasComunicado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Perfil

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * EV3: pruebas de casos límite del modo local.
 *
 * Comprueba errores de almacenamiento,
 * permisos y cancelación de operaciones.
 */
class FuenteLocalCoberturaTest {

    private lateinit var almacen: AlmacenLocal
    private lateinit var fuente: FuenteLocal

    private val inspector = MockData.usuarios.first {
        it.perfil == Perfil.INSPECTORIA_GENERAL
    }

    @BeforeEach
    fun prepararFuente() {

        almacen = mockk(relaxed = true)

        every { almacen.usuarios } returns
                flowOf(MockData.usuarios)

        every { almacen.estudiantes } returns
                flowOf(MockData.estudiantes)

        every { almacen.talleres } returns
                flowOf(MockData.talleres)

        every { almacen.apoderados } returns
                flowOf(MockData.apoderados)

        every { almacen.comunicaciones } returns
                flowOf(MockData.comunicados)

        every { almacen.envios } returns
                flowOf(MockData.envios)

        fuente = FuenteLocal(almacen)
    }

    // Confirma que la fuente corresponde al modo local.
    @Test
    fun identificaModoLocal() {

        assertTrue(fuente.esLocal)
    }

    // Comprueba los datos observables del almacenamiento.
    @Test
    fun entregaDatosDelAlmacen() = runTest {

        assertEquals(
            MockData.usuarios,
            fuente.usuarios.first()
        )

        assertEquals(
            MockData.estudiantes,
            fuente.estudiantes.first()
        )

        assertEquals(
            MockData.talleres,
            fuente.talleres.first()
        )

        assertEquals(
            MockData.apoderados,
            fuente.apoderados.first()
        )

        assertEquals(
            MockData.comunicados,
            fuente.comunicaciones.first()
        )

        assertEquals(
            MockData.envios,
            fuente.envios.first()
        )
    }

    @Test
    fun prepararDatosDelegaEnAlmacen() = runTest {

        fuente.asegurarDatosIniciales()

        coVerify(exactly = 1) {
            almacen.asegurarDatosIniciales()
        }
    }

    @Test
    fun buscarUsuarioDelegaEnAlmacen() = runTest {

        coEvery {
            almacen.buscarUsuario("usuario@prueba.cl")
        } returns inspector

        assertEquals(
            inspector,
            fuente.buscarUsuario("usuario@prueba.cl")
        )
    }

    // Un usuario sin clave local no puede autenticarse.
    @Test
    fun loginRechazaUsuarioSinClaveHash() = runTest {

        val sinClave = inspector.copy(
            claveHash = ""
        )

        coEvery {
            almacen.buscarUsuario(any())
        } returns sinClave

        val resultado = fuente.iniciarSesion(
            "usuario@prueba.cl",
            "clave-prueba"
        )

        assertTrue(resultado is ApiResult.Error)

        val error = resultado as ApiResult.Error

        assertEquals(
            CausaError.NO_AUTORIZADO,
            error.causa
        )
    }

    // Cerrar una sesión local no necesita servidor.
    @Test
    fun cerrarSesionLocalNoLanzaErrores() = runTest {

        assertEquals(
            Unit,
            fuente.cerrarSesion(FuenteLocal.TOKEN_LOCAL)
        )
    }

    // El comunicado debe pertenecer a un usuario existente.
    @Test
    fun rechazaComunicadoConAutorInexistente() = runTest {

        val comunicado = MockData.comunicados.first().copy(
            id = "AUTOR-INEXISTENTE",
            autorId = "USUARIO-NO-EXISTE"
        )

        val resultado = fuente.registrarEnvio(
            comunicado,
            null,
            emptyList()
        )

        assertTrue(resultado is ApiResult.Error)

        val error = resultado as ApiResult.Error

        assertEquals(
            CausaError.NO_AUTORIZADO,
            error.causa
        )

        coVerify(exactly = 0) {
            almacen.guardarRegistro(any(), any())
        }
    }

    // Los envíos deben pertenecer al mismo comunicado.
    @Test
    fun rechazaIdentificadorDeEnvioIncorrecto() = runTest {

        val comunicado = MockData.comunicados.first()

        val envio = MockData.envios.first().copy(
            comunicacionId = "OTRO-COMUNICADO"
        )

        try {

            fuente.registrarEnvio(
                comunicado,
                null,
                listOf(envio)
            )

            fail("Se esperaba un error de identificador")

        } catch (e: IllegalArgumentException) {

            assertEquals(
                "Los envíos deben pertenecer al comunicado",
                e.message
            )
        }
    }

    // El apoderado podría desaparecer antes de guardar.
    @Test
    fun actualizarCanalDevuelveNoEncontradoSiNoGuarda() =
        runTest {

            coEvery {
                almacen.guardarCanal(
                    "AP1",
                    CanalDespacho.EMAIL
                )
            } returns false

            val resultado =
                fuente.actualizarCanalPreferente(
                    inspector,
                    "AP1",
                    CanalDespacho.EMAIL
                )

            assertTrue(resultado is ApiResult.Error)

            val error = resultado as ApiResult.Error

            assertEquals(
                CausaError.NO_ENCONTRADO,
                error.causa
            )
        }

    // Comprueba el manejo de errores del almacenamiento.
    @Test
    fun actualizarCanalInformaErrorLocal() = runTest {

        coEvery {
            almacen.guardarCanal(
                "AP1",
                CanalDespacho.EMAIL
            )
        } throws IllegalStateException(
            "Error simulado de Room"
        )

        val resultado =
            fuente.actualizarCanalPreferente(
                inspector,
                "AP1",
                CanalDespacho.EMAIL
            )

        assertTrue(resultado is ApiResult.Error)

        val error = resultado as ApiResult.Error

        assertEquals(
            CausaError.LOCAL,
            error.causa
        )
    }

    // Las cancelaciones deben propagarse.
    @Test
    fun actualizarCanalPropagaCancelacion() = runTest {

        coEvery {
            almacen.guardarCanal(
                "AP1",
                CanalDespacho.EMAIL
            )
        } throws CancellationException(
            "Cancelación de prueba"
        )

        try {

            fuente.actualizarCanalPreferente(
                inspector,
                "AP1",
                CanalDespacho.EMAIL
            )

            fail("Se esperaba una cancelación")

        } catch (e: CancellationException) {

            assertEquals(
                "Cancelación de prueba",
                e.message
            )
        }
    }

    @Test
    fun restablecerDevuelveExito() = runTest {

        val resultado = fuente.restablecer()

        assertTrue(resultado is ApiResult.Success)

        coVerify(exactly = 1) {
            almacen.restablecer()
        }
    }

    @Test
    fun restablecerInformaErrorDeRoom() = runTest {

        coEvery {
            almacen.restablecer()
        } throws IllegalStateException(
            "Error simulado de almacenamiento"
        )

        val resultado = fuente.restablecer()

        assertTrue(resultado is ApiResult.Error)

        val error = resultado as ApiResult.Error

        assertEquals(
            CausaError.LOCAL,
            error.causa
        )
    }

    @Test
    fun restablecerPropagaCancelacion() = runTest {

        coEvery {
            almacen.restablecer()
        } throws CancellationException(
            "Operación cancelada"
        )

        try {

            fuente.restablecer()

            fail("Se esperaba una cancelación")

        } catch (e: CancellationException) {

            assertEquals(
                "Operación cancelada",
                e.message
            )
        }
    }
}
