package com.mpaznotifica.app

import android.database.sqlite.SQLiteConstraintException

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.data.local.AppDatabase
import com.mpaznotifica.app.data.repository.MpazRepository
import com.mpaznotifica.app.model.CanalDespacho

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * EV3: pruebas del Repository utilizando Room real.
 *
 * Comprueba las operaciones locales sin depender
 * de un servidor externo.
 */
@RunWith(AndroidJUnit4::class)
class MpazRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: MpazRepository

    // Crea una base independiente para cada prueba.
    @Before
    fun prepararRepository() {

        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .addCallback(
                object : RoomDatabase.Callback() {

                    override fun onOpen(
                        db: SupportSQLiteDatabase
                    ) {

                        super.onOpen(db)

                        // Mantiene las reglas de inmutabilidad.
                        AppDatabase.crearProteccion(db)
                    }
                }
            )
            .build()

        repository = MpazRepository(db)

        // Carga los datos iniciales.
        runBlocking {
            repository.asegurarDatosIniciales()
        }
    }

    // Libera la base de datos.
    @After
    fun cerrarBase() {

        db.close()
    }

    // Comprueba la carga inicial de usuarios.
    @Test
    fun repositoryConsultaUsuarios() = runBlocking {

        assertEquals(
            MockData.usuarios.map { it.id }.toSet(),
            repository.usuarios.first().map { it.id }.toSet()
        )
    }

    // Comprueba la carga de estudiantes.
    @Test
    fun repositoryConsultaEstudiantes() = runBlocking {

        assertEquals(
            MockData.estudiantes.size,
            repository.estudiantes.first().size
        )
    }

    // Comprueba la carga de talleres.
    @Test
    fun repositoryConsultaTalleres() = runBlocking {

        assertEquals(
            MockData.talleres.map { it.id }.toSet(),
            repository.talleres.first().map { it.id }.toSet()
        )
    }

    // Comprueba la carga de apoderados.
    @Test
    fun repositoryConsultaApoderados() = runBlocking {

        assertEquals(
            MockData.apoderados.map { it.id }.toSet(),
            repository.apoderados.first().map { it.id }.toSet()
        )
    }

    // Comprueba las relaciones de los estudiantes con talleres.
    @Test
    fun repositoryConservaTalleresDeEstudiantes() = runBlocking {

        val estudiantes =
            repository.estudiantes.first()

        MockData.estudiantes.forEach { original ->

            val guardado = estudiantes.first {
                it.id == original.id
            }

            assertEquals(
                original.talleresIds.toSet(),
                guardado.talleresIds.toSet()
            )
        }
    }

    // Comprueba las relaciones apoderado-estudiante.
    @Test
    fun repositoryConservaPupilos() = runBlocking {

        val apoderados =
            repository.apoderados.first()

        MockData.apoderados.forEach { original ->

            val guardado = apoderados.first {
                it.id == original.id
            }

            assertEquals(
                original.pupilosIds.toSet(),
                guardado.pupilosIds.toSet()
            )
        }
    }

    // Comprueba los comunicados iniciales.
    @Test
    fun repositoryConsultaComunicados() = runBlocking {

        assertEquals(
            MockData.comunicados.size,
            repository.comunicaciones.first().size
        )
    }

    // Comprueba los envíos iniciales.
    @Test
    fun repositoryConsultaEnvios() = runBlocking {

        assertEquals(
            MockData.envios.size,
            repository.envios.first().size
        )
    }

    // Comprueba que el historial se ordene por fecha.
    @Test
    fun repositoryOrdenaHistorial() = runBlocking {

        val fechas = repository.comunicaciones.first()
            .map { it.fechaHora }

        assertEquals(
            fechas.sortedDescending(),
            fechas
        )
    }

    // Busca un usuario válido.
    @Test
    fun repositoryBuscaUsuarioExistente() = runBlocking {

        val usuario = repository.buscarUsuario(
            "inspectoria@mpaznotifica.cl"
        )

        assertNotNull(usuario)
    }

    // Busca un usuario inexistente.
    @Test
    fun repositoryBuscaUsuarioInexistente() = runBlocking {

        assertNull(
            repository.buscarUsuario(
                "inexistente@mpaznotifica.cl"
            )
        )
    }

    // Comprueba la normalización del correo.
    @Test
    fun repositoryNormalizaCorreo() = runBlocking {

        val usuario = repository.buscarUsuario(
            "  INSPECTORIA@MPAZNOTIFICA.CL  "
        )

        assertNotNull(usuario)
    }

    // Comprueba que la siembra sea segura al repetirla.
    @Test
    fun repositoryNoDuplicaDatosIniciales() = runBlocking {

        repository.asegurarDatosIniciales()
        repository.asegurarDatosIniciales()

        assertEquals(
            MockData.comunicados.size,
            repository.comunicaciones.first().size
        )
    }

    // Comprueba una nueva instancia sobre la misma base.
    @Test
    fun repositoryReconoceBaseExistente() = runBlocking {

        val nuevoRepository = MpazRepository(db)

        nuevoRepository.asegurarDatosIniciales()

        assertEquals(
            MockData.usuarios.size,
            nuevoRepository.usuarios.first().size
        )

        assertEquals(
            MockData.comunicados.size,
            nuevoRepository.comunicaciones.first().size
        )
    }

    // CREATE: registra comunicado y despacho.
    @Test
    fun repositoryRegistraComunicado() = runBlocking {

        val comunicado = MockData.comunicados.first().copy(
            id = "REPO-NUEVO",
            asunto = "Prueba Repository"
        )

        val envio = MockData.envios.first().copy(
            id = "REPO-NUEVO-ENVIO",
            comunicacionId = comunicado.id
        )

        repository.registrarEnvio(
            comunicado,
            listOf(envio)
        )

        val resultado = repository.comunicaciones.first()
            .first { it.id == comunicado.id }

        assertEquals(
            comunicado.asunto,
            resultado.asunto
        )

        assertEquals(
            1,
            repository.envios.first().count {
                it.comunicacionId == comunicado.id
            }
        )
    }

    // Comprueba la validación de identificadores.
    @Test
    fun repositoryRechazaEnviosDeOtroComunicado() = runBlocking {

        val comunicado = MockData.comunicados.first().copy(
            id = "REPO-VALIDACION"
        )

        val envioIncorrecto = MockData.envios.first().copy(
            id = "REPO-VALIDACION-ENVIO",
            comunicacionId = "OTRO-COMUNICADO"
        )

        try {

            repository.registrarEnvio(
                comunicado,
                listOf(envioIncorrecto)
            )

            fail("Se esperaba una validación de identificadores")

        } catch (e: IllegalArgumentException) {

            assertEquals(
                "Los envíos deben pertenecer al comunicado",
                e.message
            )
        }

        assertFalse(
            repository.comunicaciones.first().any {
                it.id == comunicado.id
            }
        )
    }

    // Impide registrar un identificador repetido.
    @Test
    fun repositoryRechazaComunicadoDuplicado() = runBlocking {

        val comunicado = MockData.comunicados.first()

        try {

            repository.registrarEnvio(
                comunicado,
                emptyList()
            )

            fail("Se esperaba un error por ID duplicado")

        } catch (e: SQLiteConstraintException) {

            assertNotNull(e)
        }

        assertEquals(
            1,
            repository.comunicaciones.first().count {
                it.id == comunicado.id
            }
        )
    }

    // Comprueba que una transacción incorrecta se revierta.
    @Test
    fun repositoryRevierteRegistroIncompleto() = runBlocking {

        val comunicado = MockData.comunicados.first().copy(
            id = "REPO-ROLLBACK"
        )

        val envioIncorrecto = MockData.envios.first().copy(
            id = "REPO-ROLLBACK-ENVIO",
            comunicacionId = comunicado.id,
            apoderadoId = "AP-NO-EXISTE"
        )

        try {

            repository.registrarEnvio(
                comunicado,
                listOf(envioIncorrecto)
            )

            fail("Se esperaba un error de clave foránea")

        } catch (e: SQLiteConstraintException) {

            assertNotNull(e)
        }

        assertFalse(
            repository.comunicaciones.first().any {
                it.id == comunicado.id
            }
        )
    }

    // UPDATE: cambia el canal preferente.
    @Test
    fun repositoryActualizaCanalPreferente() = runBlocking {

        val resultado = repository.actualizarCanalPreferente(
            "AP1",
            CanalDespacho.WHATSAPP
        )

        assertTrue(resultado)

        val apoderado = repository.apoderados.first().first {
            it.id == "AP1"
        }

        assertEquals(
            CanalDespacho.WHATSAPP,
            apoderado.canalPreferente
        )
    }

    // Comprueba un apoderado inexistente.
    @Test
    fun repositoryRechazaActualizacionInexistente() = runBlocking {

        assertFalse(
            repository.actualizarCanalPreferente(
                "AP-NO-EXISTE",
                CanalDespacho.EMAIL
            )
        )
    }
}
