package com.mpaznotifica.app

import android.database.sqlite.SQLiteConstraintException

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.data.local.AppDatabase
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
 * EV3: pruebas de los datos de referencia en Room.
 *
 * Comprueba inserciones, actualizaciones,
 * relaciones y protección de las claves locales.
 */
@RunWith(AndroidJUnit4::class)
class RoomReferenciaTest {

    private lateinit var db: AppDatabase
    private lateinit var cache: RoomCache

    @Before
    fun prepararBase() {

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

    @After
    fun cerrarBase() {

        db.close()
    }

    // Construye los datos de referencia de ejemplo.
    private fun referenciaInicial(): ReferenciaRemota {

        val estudiantes = MockData.estudiantes.map {
                estudiante ->

            val apoderado = MockData.apoderados.first {
                estudiante.id in it.pupilosIds
            }

            EstudianteRemoto(
                estudiante = estudiante,
                apoderadoId = apoderado.id
            )
        }

        return ReferenciaRemota(
            usuarios = MockData.usuarios,
            talleres = MockData.talleres,
            estudiantes = estudiantes,
            apoderados = MockData.apoderados
        )
    }

    // Comprueba que actualizar los datos
    // no genere registros duplicados.
    @Test
    fun guardarReferenciaNoDuplicaDatos() = runBlocking {

        val referencia = referenciaInicial()

        cache.guardarReferencia(referencia)
        cache.guardarReferencia(referencia)

        assertEquals(
            MockData.usuarios.size,
            cache.usuarios.first().size
        )

        assertEquals(
            MockData.apoderados.size,
            cache.apoderados.first().size
        )

        assertEquals(
            MockData.estudiantes.size,
            cache.estudiantes.first().size
        )

        assertEquals(
            MockData.talleres.size,
            cache.talleres.first().size
        )
    }

    // Comprueba la actualización de usuarios existentes.
    @Test
    fun actualizarDatosPublicosDeUsuario() = runBlocking {

        val referencia = referenciaInicial()

        val original = referencia.usuarios.first()

        val modificado = original.copy(
            nombre = "Usuario actualizado"
        )

        cache.guardarReferencia(
            referencia.copy(
                usuarios = listOf(modificado)
            )
        )

        val guardado = cache.usuarios.first().first {
            it.id == original.id
        }

        assertEquals(
            "Usuario actualizado",
            guardado.nombre
        )
    }

    // Comprueba que la actualización de referencia
    // conserve la clave local existente.
    @Test
    fun actualizarUsuarioConservaClaveLocal() = runBlocking {

        val referencia = referenciaInicial()

        val original = referencia.usuarios.first()

        val modificado = original.copy(
            nombre = "Nombre actualizado",
            claveHash = ""
        )

        cache.guardarReferencia(
            referencia.copy(
                usuarios = listOf(modificado)
            )
        )

        val guardado = cache.usuarios.first().first {
            it.id == original.id
        }

        assertEquals(
            original.claveHash,
            guardado.claveHash
        )
    }

    // Comprueba la inserción de un usuario nuevo.
    @Test
    fun guardarReferenciaInsertaUsuarioNuevo() = runBlocking {

        val referencia = referenciaInicial()

        val nuevo = referencia.usuarios.first().copy(
            id = "U-REFERENCIA-NUEVA",
            nombre = "Usuario de prueba",
            usuario = "referencia@prueba.cl"
        )

        cache.guardarReferencia(
            referencia.copy(
                usuarios = listOf(nuevo)
            )
        )

        assertTrue(
            cache.usuarios.first().any {
                it.id == nuevo.id
            }
        )
    }

    // Comprueba la actualización del apoderado.
    @Test
    fun guardarReferenciaActualizaApoderado() = runBlocking {

        val referencia = referenciaInicial()

        val original = referencia.apoderados.first()

        val actualizado = original.copy(
            canalPreferente = CanalDespacho.WHATSAPP
        )

        cache.guardarReferencia(
            referencia.copy(
                apoderados = listOf(actualizado)
            )
        )

        val guardado = cache.apoderados.first().first {
            it.id == original.id
        }

        assertEquals(
            CanalDespacho.WHATSAPP,
            guardado.canalPreferente
        )
    }

    // Comprueba la modificación de talleres.
    @Test
    fun guardarReferenciaActualizaTaller() = runBlocking {

        val referencia = referenciaInicial()

        val original = referencia.talleres.first()

        val actualizado = original.copy(
            nombre = "Taller actualizado"
        )

        cache.guardarReferencia(
            referencia.copy(
                talleres = listOf(actualizado)
            )
        )

        val guardado = cache.talleres.first().first {
            it.id == original.id
        }

        assertEquals(
            "Taller actualizado",
            guardado.nombre
        )
    }

    // Comprueba que se puedan actualizar
    // las inscripciones de un estudiante.
    @Test
    fun guardarReferenciaActualizaInscripciones() = runBlocking {

        val referencia = referenciaInicial()

        val original = referencia.estudiantes.first {
            it.estudiante.talleresIds.isNotEmpty()
        }

        val actualizado = original.copy(
            estudiante = original.estudiante.copy(
                talleresIds = emptyList()
            )
        )

        cache.guardarReferencia(
            referencia.copy(
                estudiantes = listOf(actualizado)
            )
        )

        val guardado = cache.estudiantes.first().first {
            it.id == original.estudiante.id
        }

        assertTrue(
            guardado.talleresIds.isEmpty()
        )
    }

    // Comprueba que una referencia vacía
    // no elimine los datos existentes.
    @Test
    fun guardarReferenciaVaciaConservaDatos() = runBlocking {

        cache.guardarReferencia(
            ReferenciaRemota(
                usuarios = emptyList(),
                talleres = emptyList(),
                estudiantes = emptyList(),
                apoderados = emptyList()
            )
        )

        assertEquals(
            MockData.usuarios.size,
            cache.usuarios.first().size
        )

        assertEquals(
            MockData.estudiantes.size,
            cache.estudiantes.first().size
        )
    }

    // Una referencia con una clave foránea incorrecta
    // debe revertir la transacción.
    @Test
    fun guardarReferenciaFallidaRevierteCambios() = runBlocking {

        val referencia = referenciaInicial()

        val usuario = referencia.usuarios.first()

        val modificado = usuario.copy(
            nombre = "Cambio que debe revertirse"
        )

        val estudianteIncorrecto =
            referencia.estudiantes.first().copy(
                apoderadoId = "AP-INEXISTENTE"
            )

        try {

            cache.guardarReferencia(
                referencia.copy(
                    usuarios = listOf(modificado),
                    estudiantes = listOf(estudianteIncorrecto)
                )
            )

            fail("Se esperaba un error de clave foránea")

        } catch (e: SQLiteConstraintException) {

            val guardado = cache.usuarios.first().first {
                it.id == usuario.id
            }

            assertEquals(
                usuario.nombre,
                guardado.nombre
            )
        }
    }
}
