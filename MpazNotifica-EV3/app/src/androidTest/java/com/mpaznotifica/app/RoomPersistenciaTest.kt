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
import com.mpaznotifica.app.data.local.EnvioEntity
import com.mpaznotifica.app.data.local.aEntidad
import com.mpaznotifica.app.data.repository.RoomCache
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

    // Crea una base de pruebas antes de cada caso.
    @Before
    fun prepararBase() {

        context =
            InstrumentationRegistry
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

                        // Aplica la protección real RN-02.
                        AppDatabase.crearProteccion(db)
                    }
                }
            )
            .build()

        cache = RoomCache(db)

        // Carga los datos de ejemplo.
        runBlocking {
            cache.asegurarDatosIniciales()
        }
    }

    // Cierra la base al terminar cada prueba.
    @After
    fun cerrarBase() {

        db.close()
    }

    // READ: comprueba la carga de usuarios.
    @Test
    fun consultarUsuariosGuardados() = runBlocking {

        val usuarios =
            cache.usuarios.first()

        assertEquals(
            MockData.usuarios.size,
            usuarios.size
        )
    }

    // READ: comprueba la carga de apoderados.
    @Test
    fun consultarApoderadosGuardados() = runBlocking {

        val apoderados =
            cache.apoderados.first()

        assertEquals(
            MockData.apoderados.size,
            apoderados.size
        )
    }

    // Comprueba la búsqueda de usuarios mediante el DAO.
    @Test
    fun buscarUsuarioExistente() = runBlocking {

        val usuario =
            cache.buscarUsuario(
                "inspectoria@mpaznotifica.cl"
            )

        assertNotNull(usuario)
    }

    // Comprueba una búsqueda sin resultados.
    @Test
    fun buscarUsuarioInexistente() = runBlocking {

        val usuario =
            cache.buscarUsuario(
                "inexistente@mpaznotifica.cl"
            )

        assertNull(usuario)
    }

    // CREATE: registra un comunicado y su despacho.
    @Test
    fun guardarComunicadoYEnvio() = runBlocking {

        val comunicado =
            MockData.comunicados.first().copy(
                id = "TEST-CREATE-01",
                asunto = "Prueba de persistencia Room"
            )

        val envio =
            MockData.envios.first().copy(
                id = "TEST-ENVIO-01",
                comunicacionId = comunicado.id
            )

        cache.guardarRegistro(
            comunicado,
            listOf(envio)
        )

        // Comprueba que ambos registros existan.
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

    // UPDATE: modifica el canal preferente.
    @Test
    fun actualizarCanalPreferente() = runBlocking {

        val resultado =
            cache.guardarCanal(
                "AP1",
                CanalDespacho.WHATSAPP
            )

        assertTrue(resultado)

        val apoderado =
            cache.apoderados.first().first {
                it.id == "AP1"
            }

        assertEquals(
            CanalDespacho.WHATSAPP,
            apoderado.canalPreferente
        )
    }

    // No permite actualizar un apoderado inexistente.
    @Test
    fun actualizarApoderadoInexistente() = runBlocking {

        val resultado =
            cache.guardarCanal(
                "AP-INEXISTENTE",
                CanalDespacho.EMAIL
            )

        assertFalse(resultado)
    }

    // RN-02: impide modificar comunicados enviados.
    @Test
    fun comunicadoEnviadoNoSePuedeEditar() {

        val base =
            db.openHelper.writableDatabase

        val id =
            MockData.comunicados.first().id

        try {

            base.execSQL(
                "UPDATE comunicaciones SET asunto = ? WHERE id = ?",
                arrayOf("Asunto modificado", id)
            )

            fail("Room permitió editar un comunicado enviado")

        } catch (e: SQLiteConstraintException) {

            assertTrue(
                e.message.orEmpty().contains("RN-02")
            )
        }
    }

    // RN-02: impide eliminar comunicados enviados.
    @Test
    fun comunicadoEnviadoNoSePuedeEliminar() {

        val base =
            db.openHelper.writableDatabase

        val id =
            MockData.comunicados.first().id

        try {

            base.execSQL(
                "DELETE FROM comunicaciones WHERE id = ?",
                arrayOf(id)
            )

            fail("Room permitió eliminar un comunicado enviado")

        } catch (e: SQLiteConstraintException) {

            assertTrue(
                e.message.orEmpty().contains("RN-02")
            )
        }
    }

    // RN-02: impide modificar registros de despacho.
    @Test
    fun envioRegistradoNoSePuedeEditar() {

        val base =
            db.openHelper.writableDatabase

        val id =
            MockData.envios.first().id

        try {

            base.execSQL(
                "UPDATE envios SET detalle = ? WHERE id = ?",
                arrayOf("Detalle modificado", id)
            )

            fail("Room permitió editar un despacho")

        } catch (e: SQLiteConstraintException) {

            assertTrue(
                e.message.orEmpty().contains("RN-02")
            )
        }
    }

    // RN-02: impide eliminar registros de despacho.
    @Test
    fun envioRegistradoNoSePuedeEliminar() {

        val base =
            db.openHelper.writableDatabase

        val id =
            MockData.envios.first().id

        try {

            base.execSQL(
                "DELETE FROM envios WHERE id = ?",
                arrayOf(id)
            )

            fail("Room permitió eliminar un despacho")

        } catch (e: SQLiteConstraintException) {

            assertTrue(
                e.message.orEmpty().contains("RN-02")
            )
        }
    }

    // Comprueba la integridad de las claves foráneas.
    @Test
    fun rechazaEnvioSinComunicadoExistente() = runBlocking {

        val envio =
            MockData.envios.first().copy(
                id = "TEST-FK-01",
                comunicacionId = "COMUNICADO-INEXISTENTE"
            )

        try {

            db.comunicacionDao().insertarEnvios(
                listOf(envio.aEntidad())
            )

            fail("Se permitió registrar un envío sin comunicado")

        } catch (e: SQLiteConstraintException) {

            // La relación entre tablas está protegida.
            assertNotNull(e)
        }
    }

    // Comprueba que una transacción fallida no guarde datos parciales.
    @Test
    fun transaccionFallidaRevierteLosCambios() = runBlocking {

        val comunicado =
            MockData.comunicados.first().copy(
                id = "TEST-TRANSACCION-01"
            )

        val envioIncorrecto: EnvioEntity =
            MockData.envios.first().copy(
                id = "TEST-TRANSACCION-ENVIO",
                comunicacionId = comunicado.id,
                apoderadoId = "AP-INEXISTENTE"
            ).aEntidad()

        try {

            db.withTransaction {

                db.comunicacionDao().insertar(
                    comunicado.aEntidad()
                )

                db.comunicacionDao().insertarEnvios(
                    listOf(envioIncorrecto)
                )
            }

            fail("Se esperaba un error de clave foránea")

        } catch (e: SQLiteConstraintException) {

            // Verifica que tampoco se guardó el comunicado.
            assertFalse(
                cache.comunicaciones.first().any {
                    it.id == comunicado.id
                }
            )
        }
    }

    // DELETE controlado: restablece únicamente la base de prueba.
    @Test
    fun restablecerDatosEliminaRegistrosDePrueba() = runBlocking {

        val comunicado =
            MockData.comunicados.first().copy(
                id = "TEST-DELETE-01"
            )

        val envio =
            MockData.envios.first().copy(
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

        // Restablece los datos de ejemplo.
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

        // Comprueba que se restauró el canal original.
        val canalOriginal =
            MockData.apoderados.first {
                it.id == "AP1"
            }.canalPreferente

        val canalRestaurado =
            cache.apoderados.first().first {
                it.id == "AP1"
            }.canalPreferente

        assertEquals(
            canalOriginal,
            canalRestaurado
        )
    }
}