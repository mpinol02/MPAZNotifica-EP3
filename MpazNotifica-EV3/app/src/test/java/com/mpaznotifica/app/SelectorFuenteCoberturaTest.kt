package com.mpaznotifica.app

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.data.remote.ApiResult
import com.mpaznotifica.app.data.repository.FuenteDatos
import com.mpaznotifica.app.data.repository.ModoEnMemoria
import com.mpaznotifica.app.data.repository.SelectorFuente
import com.mpaznotifica.app.model.CanalDespacho

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * EV3: pruebas adicionales del selector de fuentes.
 *
 * Verifica que las operaciones se deleguen
 * a la fuente activa de la aplicación.
 */
class SelectorFuenteCoberturaTest {

    private lateinit var local: FuenteDatos
    private lateinit var servidor: FuenteDatos
    private lateinit var modo: ModoEnMemoria
    private lateinit var selector: SelectorFuente

    @BeforeEach
    fun prepararFuentes() {

        local = mockk(relaxed = true)
        servidor = mockk(relaxed = true)

        modo = ModoEnMemoria(true)

        every { local.usuarios } returns
                flowOf(MockData.usuarios)

        every { servidor.usuarios } returns
                flowOf(emptyList())

        every { local.estudiantes } returns
                flowOf(MockData.estudiantes)

        every { servidor.estudiantes } returns
                flowOf(emptyList())

        every { local.talleres } returns
                flowOf(MockData.talleres)

        every { servidor.talleres } returns
                flowOf(emptyList())

        every { local.apoderados } returns
                flowOf(MockData.apoderados)

        every { servidor.apoderados } returns
                flowOf(emptyList())

        every { local.comunicaciones } returns
                flowOf(MockData.comunicados)

        every { servidor.comunicaciones } returns
                flowOf(emptyList())

        every { local.envios } returns
                flowOf(MockData.envios)

        every { servidor.envios } returns
                flowOf(emptyList())

        selector = SelectorFuente(
            modo,
            local,
            servidor
        )
    }

    // Comprueba el modo activo.
    @Test
    fun selectorInformaModoLocal() {

        assertTrue(selector.esLocal)
        assertSame(local, selector.actual())
    }

    @Test
    fun selectorInformaModoServidor() {

        modo.cambiar(false)

        assertFalse(selector.esLocal)
        assertSame(servidor, selector.actual())
    }

    // Comprueba los datos observables.
    @Test
    fun consultaUsuariosLocales() = runTest {

        assertEquals(
            MockData.usuarios,
            selector.usuarios.first()
        )
    }

    @Test
    fun consultaUsuariosDelServidor() = runTest {

        modo.cambiar(false)

        assertTrue(
            selector.usuarios.first().isEmpty()
        )
    }

    @Test
    fun consultaEstudiantesLocales() = runTest {

        assertEquals(
            MockData.estudiantes,
            selector.estudiantes.first()
        )
    }

    @Test
    fun consultaEstudiantesDelServidor() = runTest {

        modo.cambiar(false)

        assertTrue(
            selector.estudiantes.first().isEmpty()
        )
    }

    @Test
    fun consultaTalleresLocales() = runTest {

        assertEquals(
            MockData.talleres,
            selector.talleres.first()
        )
    }

    @Test
    fun consultaTalleresDelServidor() = runTest {

        modo.cambiar(false)

        assertTrue(
            selector.talleres.first().isEmpty()
        )
    }

    @Test
    fun consultaApoderadosLocales() = runTest {

        assertEquals(
            MockData.apoderados,
            selector.apoderados.first()
        )
    }

    @Test
    fun consultaApoderadosDelServidor() = runTest {

        modo.cambiar(false)

        assertTrue(
            selector.apoderados.first().isEmpty()
        )
    }

    @Test
    fun consultaComunicacionesLocales() = runTest {

        assertEquals(
            MockData.comunicados,
            selector.comunicaciones.first()
        )
    }

    @Test
    fun consultaComunicacionesDelServidor() = runTest {

        modo.cambiar(false)

        assertTrue(
            selector.comunicaciones.first().isEmpty()
        )
    }

    @Test
    fun consultaEnviosLocales() = runTest {

        assertEquals(
            MockData.envios,
            selector.envios.first()
        )
    }

    @Test
    fun consultaEnviosDelServidor() = runTest {

        modo.cambiar(false)

        assertTrue(
            selector.envios.first().isEmpty()
        )
    }

    // Comprueba la búsqueda en la fuente activa.
    @Test
    fun buscarUsuarioEnModoLocal() = runTest {

        val usuario = MockData.usuarios.first()

        coEvery {
            local.buscarUsuario("usuario@prueba.cl")
        } returns usuario

        assertEquals(
            usuario,
            selector.buscarUsuario("usuario@prueba.cl")
        )

        coVerify(exactly = 1) {
            local.buscarUsuario("usuario@prueba.cl")
        }

        coVerify(exactly = 0) {
            servidor.buscarUsuario(any())
        }
    }

    @Test
    fun buscarUsuarioEnModoServidor() = runTest {

        modo.cambiar(false)

        coEvery {
            servidor.buscarUsuario(any())
        } returns null

        assertNull(
            selector.buscarUsuario("usuario@prueba.cl")
        )

        coVerify(exactly = 1) {
            servidor.buscarUsuario("usuario@prueba.cl")
        }

        coVerify(exactly = 0) {
            local.buscarUsuario(any())
        }
    }

    // Comprueba el cierre de sesión.
    @Test
    fun cerrarSesionLocal() = runTest {

        selector.cerrarSesion("token-local")

        coVerify(exactly = 1) {
            local.cerrarSesion("token-local")
        }

        coVerify(exactly = 0) {
            servidor.cerrarSesion(any())
        }
    }

    @Test
    fun cerrarSesionServidor() = runTest {

        modo.cambiar(false)

        selector.cerrarSesion("token-prueba")

        coVerify(exactly = 1) {
            servidor.cerrarSesion("token-prueba")
        }

        coVerify(exactly = 0) {
            local.cerrarSesion(any())
        }
    }

    // Comprueba la delegación de sincronización.
    @Test
    fun sincronizarModoLocal() = runTest {

        coEvery {
            local.sincronizar()
        } returns ApiResult.Success(Unit)

        val resultado = selector.sincronizar()

        assertTrue(resultado is ApiResult.Success)

        coVerify(exactly = 1) {
            local.sincronizar()
        }
    }

    @Test
    fun sincronizarModoServidor() = runTest {

        modo.cambiar(false)

        coEvery {
            servidor.sincronizar()
        } returns ApiResult.Success(Unit)

        val resultado = selector.sincronizar()

        assertTrue(resultado is ApiResult.Success)

        coVerify(exactly = 1) {
            servidor.sincronizar()
        }
    }

    // Comprueba el registro de comunicados.
    @Test
    fun registrarEnvioDelegaEnLocal() = runTest {

        val comunicado = MockData.comunicados.first()

        val envios = MockData.envios.filter {
            it.comunicacionId == comunicado.id
        }

        selector.registrarEnvio(
            comunicado,
            null,
            envios
        )

        coVerify(exactly = 1) {
            local.registrarEnvio(
                comunicado,
                null,
                envios
            )
        }

        coVerify(exactly = 0) {
            servidor.registrarEnvio(any(), any(), any())
        }
    }

    @Test
    fun registrarEnvioDelegaEnServidor() = runTest {

        modo.cambiar(false)

        val comunicado = MockData.comunicados.first()

        val envios = MockData.envios.filter {
            it.comunicacionId == comunicado.id
        }

        selector.registrarEnvio(
            comunicado,
            null,
            envios
        )

        coVerify(exactly = 1) {
            servidor.registrarEnvio(
                comunicado,
                null,
                envios
            )
        }

        coVerify(exactly = 0) {
            local.registrarEnvio(any(), any(), any())
        }
    }

    // Comprueba la actualización de canales.
    @Test
    fun actualizarCanalDelegaEnLocal() = runTest {

        val actor = MockData.usuarios.first()

        selector.actualizarCanalPreferente(
            actor,
            "AP1",
            CanalDespacho.EMAIL
        )

        coVerify(exactly = 1) {
            local.actualizarCanalPreferente(
                actor,
                "AP1",
                CanalDespacho.EMAIL
            )
        }

        coVerify(exactly = 0) {
            servidor.actualizarCanalPreferente(
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun actualizarCanalDelegaEnServidor() = runTest {

        modo.cambiar(false)

        val actor = MockData.usuarios.first()

        selector.actualizarCanalPreferente(
            actor,
            "AP1",
            CanalDespacho.EMAIL
        )

        coVerify(exactly = 1) {
            servidor.actualizarCanalPreferente(
                actor,
                "AP1",
                CanalDespacho.EMAIL
            )
        }

        coVerify(exactly = 0) {
            local.actualizarCanalPreferente(
                any(),
                any(),
                any()
            )
        }
    }

    // Comprueba que el restablecimiento afecte
    // únicamente a la fuente activa.
    @Test
    fun restablecerSoloFuenteLocal() = runTest {

        selector.restablecer()

        coVerify(exactly = 1) {
            local.restablecer()
        }

        coVerify(exactly = 0) {
            servidor.restablecer()
        }
    }

    @Test
    fun restablecerSoloFuenteServidor() = runTest {

        modo.cambiar(false)

        selector.restablecer()

        coVerify(exactly = 1) {
            servidor.restablecer()
        }

        coVerify(exactly = 0) {
            local.restablecer()
        }
    }
}
