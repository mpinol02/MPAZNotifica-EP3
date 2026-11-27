package com.mpaznotifica.app

import android.content.Context
import android.database.sqlite.SQLiteConstraintException

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.data.local.AppDatabase
import com.mpaznotifica.app.data.local.aEntidad
import com.mpaznotifica.app.data.repository.RoomCache
import com.mpaznotifica.app.data.repository.ReferenciaRemota
import com.mpaznotifica.app.data.repository.EstudianteRemoto
import com.mpaznotifica.app.model.CanalDespacho

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * EV3: pruebas instrumentadas de persistencia Room.
 *
 * Utiliza una base independiente para no modificar
 * los datos reales de la aplicación.
 */
@RunWith(AndroidJUnit4::class)
class RoomPersistenciaTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var cache: RoomCache

    // Crea una base independiente antes de cada prueba.
    @Before
    fun prepararBase() {

        context = InstrumentationRegistry
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

                        // Activa la protección de la bitácora.
                        AppDatabase.crearProteccion(db)
                    }
                }
            )
            .build()

        cache = RoomCache(db)

        runBlocking {
            cache.asegurarDatosIniciales()
        }
    }

    // Libera la base al finalizar cada prueba.
    @After
    fun cerrarBase() {

        db.close()
    }

    // READ: comprueba los usuarios almacenados.
    @Test
    fun consultarUsuariosGuardados() = runBlocking {

        assertEquals(
            MockData.usuarios.size,
            cache.usuarios.first().size
        )
    }

    // READ: comprueba los apoderados almacenados.
    @Test
    fun consultarApoderadosGuardados() = runBlocking {

        assertEquals(
            MockData.apoderados.size,
            cache.apoderados.first().size
        )
    }

    // Comprueba la carga de estudiantes y sus talleres.
    @Test
    fun consultarEstudiantesGuardados() = runBlocking {

        val estudiantes = cache.estudiantes.first()

        assertEquals(
            MockData.estudiantes.size,
            estudiantes.size
        )

        MockData.estudiantes.forEach { original ->

            val guardado = estudiantes.first {
                it.id == original.id
            }

            assertEquals(
                original.curso,
                guardado.curso
            )

            assertEquals(
                original.talleresIds.toSet(),
                guardado.talleresIds.toSet()
            )
        }
    }

    // Comprueba la carga de los talleres.
    @Test
    fun consultarTalleresGuardados() = runBlocking {

        assertEquals(
            MockData.talleres.map { it.id }.toSet(),
            cache.talleres.first().map { it.id }.toSet()
        )
    }

    // Comprueba las relaciones entre apoderados y estudiantes.
    @Test
    fun conservaPupilosDeCadaApoderado() = runBlocking {

        val apoderados = cache.apoderados.first()

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

    // Comprueba el historial inicial.
    @Test
    fun consultarComunicadosIniciales() = runBlocking {

        assertEquals(
            MockData.comunicados.map { it.id }.toSet(),
            cache.comunicaciones.first().map { it.id }.toSet()
        )
    }

    // Comprueba los despachos iniciales.
    @Test
    fun consultarEnviosIniciales() = runBlocking {

        assertEquals(
            MockData.envios.size,
            cache.envios.first().size
        )
    }

    // Comprueba que la siembra no duplique datos.
    @Test
    fun inicializarDosVecesNoDuplicaRegistros() = runBlocking {

        cache.asegurarDatosIniciales()
        cache.asegurarDatosIniciales()

        assertEquals(
            MockData.comunicados.size,
            cache.comunicaciones.first().size
        )

        assertEquals(
            MockData.usuarios.size,
            cache.usuarios.first().size
        )
    }

    // Comprueba una nueva instancia sobre una base existente.
    @Test
    fun nuevaCacheNoDuplicaDatosExistentes() = runBlocking {

        val segundaCache = RoomCache(db)

        segundaCache.asegurarDatosIniciales()

        assertEquals(
            MockData.comunicados.size,
            segundaCache.comunicaciones.first().size
        )
    }

    // Busca un usuario existente.
    @Test
    fun buscarUsuarioExistente() = runBlocking {

        assertNotNull(
            cache.buscarUsuario(
                "inspectoria@mpaznotifica.cl"
            )
        )
    }

    // Busca un usuario inexistente.
    @Test
    fun buscarUsuarioInexistente() = runBlocking {

        assertNull(
            cache.buscarUsuario(
                "inexistente@mpaznotifica.cl"
            )
        )
    }

    // Comprueba la normalización del correo.
    @Test
    fun buscarUsuarioConMayusculasYEspacios() = runBlocking {

        val usuario = cache.buscarUsuario(
            "  INSPECTORIA@MPAZNOTIFICA.CL  "
        )

        assertNotNull(usuario)
    }

    // CREATE: registra un comunicado y su despacho.
    @Test
    fun guardarComunicadoYEnvio() = runBlocking {

        val comunicado = MockData.comunicados.first().copy(
            id = "TEST-CREATE-01",
            asunto = "Prueba de persistencia Room"
        )

        val envio = MockData.envios.first().copy(
            id = "TEST-ENVIO-01",
            comunicacionId = comunicado.id
        )

        cache.guardarRegistro(
            comunicado,
            listOf(envio)
        )

        assertTrue(
            cache.comunicaciones.first().any {
                it.id == comunicado.id
            }
        )

        assertTrue(
            cache.envios.first().any {
                it.comunicacionId == comunicado.id
            }
        )
    }

    // Comprueba que guardar el mismo registro no lo duplique.
    @Test
    fun guardarRegistroRepetidoNoDuplicaDatos() = runBlocking {

        val comunicado = MockData.comunicados.first().copy(
            id = "TEST-DUPLICADO"
        )

        val envio = MockData.envios.first().copy(
            id = "TEST-DUPLICADO-ENVIO",
            comunicacionId = comunicado.id
        )

        repeat(2) {

            cache.guardarRegistro(
                comunicado,
                listOf(envio)
            )
        }

        assertEquals(
            1,
            cache.comunicaciones.first().count {
                it.id == comunicado.id
            }
        )

        assertEquals(
            1,
            cache.envios.first().count {
                it.id == envio.id
            }
        )
    }

    // UPDATE: cambia el canal preferente.
    @Test
    fun actualizarCanalPreferente() = runBlocking {

        assertTrue(
            cache.guardarCanal(
                "AP1",
                CanalDespacho.WHATSAPP
            )
        )

        val apoderado = cache.apoderados.first().first {
            it.id == "AP1"
        }

        assertEquals(
            CanalDespacho.WHATSAPP,
            apoderado.canalPreferente
        )
    }

    // Comprueba que un cambio de canal no altere la bitácora.
    @Test
    fun actualizarCanalNoModificaEnviosAnteriores() = runBlocking {

        val enviosAnteriores =
            cache.envios.first().associateBy { it.id }

        cache.guardarCanal(
            "AP1",
            CanalDespacho.WHATSAPP
        )

        val enviosPosteriores =
            cache.envios.first().associateBy { it.id }

        assertEquals(
            enviosAnteriores,
            enviosPosteriores
        )
    }

    // No permite actualizar un apoderado inexistente.
    @Test
    fun actualizarApoderadoInexistente() = runBlocking {

        assertFalse(
            cache.guardarCanal(
                "AP-INEXISTENTE",
                CanalDespacho.EMAIL
            )
        )
    }

    // RN-02: impide modificar comunicados.
    @Test
    fun comunicadoEnviadoNoSePuedeEditar() {

        val base = db.openHelper.writableDatabase
        val id = MockData.comunicados.first().id

        try {

            base.execSQL(
                "UPDATE comunicaciones SET asunto = ? WHERE id = ?",
                arrayOf("Asunto modificado", id)
            )

            fail("Se permitió editar un comunicado enviado")

        } catch (e: SQLiteConstraintException) {

            assertTrue(
                e.message.orEmpty().contains("RN-02")
            )
        }
    }

    // RN-02: impide eliminar comunicados.
    @Test
    fun comunicadoEnviadoNoSePuedeEliminar() {

        val base = db.openHelper.writableDatabase
        val id = MockData.comunicados.first().id

        try {

            base.execSQL(
                "DELETE FROM comunicaciones WHERE id = ?",
                arrayOf(id)
            )

            fail("Se permitió eliminar un comunicado enviado")

        } catch (e: SQLiteConstraintException) {

            assertTrue(
                e.message.orEmpty().contains("RN-02")
            )
        }
    }

    // RN-02: impide modificar despachos.
    @Test
    fun envioRegistradoNoSePuedeEditar() {

        val base = db.openHelper.writableDatabase
        val id = MockData.envios.first().id

        try {

            base.execSQL(
                "UPDATE envios SET detalle = ? WHERE id = ?",
                arrayOf("Detalle modificado", id)
            )

            fail("Se permitió editar un despacho")

        } catch (e: SQLiteConstraintException) {

            assertTrue(
                e.message.orEmpty().contains("RN-02")
            )
        }
    }

    // RN-02: impide eliminar despachos.
    @Test
    fun envioRegistradoNoSePuedeEliminar() {

        val base = db.openHelper.writableDatabase
        val id = MockData.envios.first().id

        try {

            base.execSQL(
                "DELETE FROM envios WHERE id = ?",
                arrayOf(id)
            )

            fail("Se permitió eliminar un despacho")

        } catch (e: SQLiteConstraintException) {

            assertTrue(
                e.message.orEmpty().contains("RN-02")
            )
        }
    }

    // Comprueba la integridad referencial.
    @Test
    fun rechazaEnvioSinComunicadoExistente() = runBlocking {

        val envio = MockData.envios.first().copy(
            id = "TEST-FK-01",
            comunicacionId = "COMUNICADO-INEXISTENTE"
        )

        try {

            db.comunicacionDao().insertarEnvios(
                listOf(envio.aEntidad())
            )

            fail("Se permitió registrar un envío sin comunicado")

        } catch (e: SQLiteConstraintException) {

            assertNotNull(e)
        }
    }

    // Comprueba la reversión de transacciones fallidas.
    @Test
    fun transaccionFallidaRevierteLosCambios() = runBlocking {

        val comunicado = MockData.comunicados.first().copy(
            id = "TEST-TRANSACCION"
        )

        val envioIncorrecto = MockData.envios.first().copy(
            id = "TEST-TRANSACCION-ENVIO",
            comunicacionId = comunicado.id,
            apoderadoId = "AP-INEXISTENTE"
        )

        try {

            db.withTransaction {

                db.comunicacionDao().insertar(
                    comunicado.aEntidad()
                )

                db.comunicacionDao().insertarEnvios(
                    listOf(envioIncorrecto.aEntidad())
                )
            }

            fail("Se esperaba un error de clave foránea")

        } catch (e: SQLiteConstraintException) {

            assertFalse(
                cache.comunicaciones.first().any {
                    it.id == comunicado.id
                }
            )
        }
    }

    // Comprueba el restablecimiento de datos locales.
    @Test
    fun restablecerDatosEliminaRegistrosDePrueba() = runBlocking {

        val comunicado = MockData.comunicados.first().copy(
            id = "TEST-DELETE"
        )

        val envio = MockData.envios.first().copy(
            id = "TEST-DELETE-ENVIO",
            comunicacionId = comunicado.id
        )

        cache.guardarRegistro(
            comunicado,
            listOf(envio)
        )

        cache.guardarCanal(
            "AP1",
            CanalDespacho.WHATSAPP
        )

        cache.restablecer()

        assertFalse(
            cache.comunicaciones.first().any {
                it.id == comunicado.id
            }
        )

        assertEquals(
            MockData.comunicados.size,
            cache.comunicaciones.first().size
        )

        assertEquals(
            MockData.envios.size,
            cache.envios.first().size
        )

        val canalOriginal = MockData.apoderados.first {
            it.id == "AP1"
        }.canalPreferente

        val canalRestaurado = cache.apoderados.first().first {
            it.id == "AP1"
        }.canalPreferente

        assertEquals(
            canalOriginal,
            canalRestaurado
        )
    }

    // Comprueba que el restablecimiento restaure los disparadores.
    @Test
    fun restablecerMantieneProteccionDeBitacora() = runBlocking {

        cache.restablecer()

        try {

            db.openHelper.writableDatabase.execSQL(
                "DELETE FROM comunicaciones"
            )

            fail("La bitácora perdió su protección")

        } catch (e: SQLiteConstraintException) {

            assertTrue(
                e.message.orEmpty().contains("RN-02")
            )
        }
    }

    // Comprueba el reemplazo controlado de la caché.
    @Test
    fun reemplazarBitacoraConservaLosNuevosRegistros() = runBlocking {

        val comunicado = MockData.comunicados.first().copy(
            id = "CACHE-NUEVA"
        )

        val envio = MockData.envios.first().copy(
            id = "CACHE-ENVIO",
            comunicacionId = comunicado.id
        )

        cache.reemplazarBitacora(
            listOf(comunicado),
            listOf(envio)
        )

        assertEquals(
            listOf(comunicado.id),
            cache.comunicaciones.first().map { it.id }
        )

        assertEquals(
            listOf(envio.id),
            cache.envios.first().map { it.id }
        )
    }

    // Comprueba el reemplazo por una bitácora vacía.
    @Test
    fun reemplazarBitacoraVaciaEliminaRegistros() = runBlocking {

        cache.reemplazarBitacora(
            emptyList(),
            emptyList()
        )

        assertTrue(
            cache.comunicaciones.first().isEmpty()
        )

        assertTrue(
            cache.envios.first().isEmpty()
        )
    }

    // Una sincronización incorrecta no debe eliminar datos existentes.
    @Test
    fun reemplazarBitacoraFallidaConservaDatosOriginales() = runBlocking {

        val anteriores =
            cache.comunicaciones.first().map { it.id }.toSet()

        val envioIncorrecto = MockData.envios.first().copy(
            id = "CACHE-ERROR",
            comunicacionId = "NO-EXISTE"
        )

        try {

            cache.reemplazarBitacora(
                emptyList(),
                listOf(envioIncorrecto)
            )

            fail("Se esperaba un error de clave foránea")

        } catch (e: SQLiteConstraintException) {

            assertEquals(
                anteriores,
                cache.comunicaciones.first()
                    .map { it.id }.toSet()
            )
        }
    }
}
