package com.mpaznotifica.app

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.domain.ReglasComunicado
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.model.TipoComunicacion

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * EV3: pruebas unitarias con JUnit 5.
 *
 * Comprueba permisos, validaciones y reglas de negocio.
 */
@DisplayName("Reglas de negocio de comunicados")
class ReglasComunicadoJUnit5Test {

    // Usuarios de prueba.
    private val inspectoria =
        MockData.usuarios.first {
            it.perfil == Perfil.INSPECTORIA_GENERAL
        }

    private val profesor =
        MockData.usuarios.first {
            it.perfil == Perfil.PROFESOR_JEFE
        }

    // Inspectoría puede utilizar todos los tipos.
    @Test
    fun inspectoriaPuedeUtilizarTodosLosTipos() {

        val resultado =
            ReglasComunicado.tiposPermitidos(inspectoria)

        assertEquals(
            TipoComunicacion.entries.toSet(),
            resultado.toSet()
        )
    }

    // El Profesor Jefe no puede emitir mensajes institucionales.
    @Test
    fun profesorNoPuedeEnviarTipoInstitucional() {

        val resultado =
            ReglasComunicado.tiposPermitidos(profesor)

        assertFalse(
            TipoComunicacion.INSTITUCIONAL in resultado
        )
    }

    // Inspectoría puede enviar a todo el colegio.
    @Test
    fun inspectoriaPuedeEnviarAColegioCompleto() {

        val resultado =
            ReglasComunicado.alcancesPermitidos(inspectoria)

        assertTrue(
            AlcanceEnvio.COLEGIO_COMPLETO in resultado
        )
    }

    // RF-06: el profesor no tiene acceso al colegio completo.
    @Test
    fun profesorNoPuedeEnviarAColegioCompleto() {

        val resultado =
            ReglasComunicado.alcancesPermitidos(profesor)

        assertFalse(
            AlcanceEnvio.COLEGIO_COMPLETO in resultado
        )
    }

    // RF-06: el profesor no puede emitir a talleres.
    @Test
    fun profesorNoPuedeEnviarATalleres() {

        val resultado =
            ReglasComunicado.alcancesPermitidos(profesor)

        assertFalse(
            AlcanceEnvio.TALLER in resultado
        )
    }

    // Un usuario sin sesión no tiene tipos disponibles.
    @Test
    fun usuarioSinSesionNoTieneTiposPermitidos() {

        assertTrue(
            ReglasComunicado.tiposPermitidos(null).isEmpty()
        )
    }

    // Un usuario sin sesión no tiene alcances disponibles.
    @Test
    fun usuarioSinSesionNoTieneAlcancesPermitidos() {

        assertTrue(
            ReglasComunicado.alcancesPermitidos(null).isEmpty()
        )
    }

    // Comprueba que un PDF válido sea aceptado.
    @Test
    fun aceptaDocumentoPdfValido() {

        val resultado =
            ReglasComunicado.errorAdjunto(
                "application/pdf",
                1024L
            )

        assertNull(resultado)
    }

    // Impide seleccionar archivos de otro formato.
    @Test
    fun rechazaArchivoQueNoEsPdf() {

        val resultado =
            ReglasComunicado.errorAdjunto(
                "image/png",
                1024L
            )

        assertNotNull(resultado)
    }

    // Comprueba el límite de tamaño permitido.
    @Test
    fun rechazaPdfQueSuperaElLimite() {

        val resultado =
            ReglasComunicado.errorAdjunto(
                "application/pdf",
                ReglasComunicado.MAX_ADJUNTO_BYTES + 1
            )

        assertNotNull(resultado)
    }

    // Acepta un documento exactamente en el límite.
    @Test
    fun aceptaPdfEnElLimitePermitido() {

        val resultado =
            ReglasComunicado.errorAdjunto(
                "application/pdf",
                ReglasComunicado.MAX_ADJUNTO_BYTES
            )

        assertNull(resultado)
    }

    // RF-12: una autorización debe informarse por correo.
    @Test
    fun firmaFisicaObligaAUtilizarCorreo() {

        val apoderado = MockData.apoderados.first()

        val resultado =
            ReglasComunicado.canalDe(
                apoderado,
                true
            )

        assertEquals(
            CanalDespacho.EMAIL,
            resultado
        )
    }

    // Sin firma física se utiliza el canal preferente.
    @Test
    fun comunicadoNormalRespetaCanalPreferente() {

        val apoderado = MockData.apoderados.first()

        val resultado =
            ReglasComunicado.canalDe(
                apoderado,
                false
            )

        assertEquals(
            apoderado.canalPreferente,
            resultado
        )
    }

    // Comprueba que se agregue el aviso de firma física.
    @Test
    fun agregaAvisoDeFirmaFisica() {

        val resultado =
            ReglasComunicado.cuerpoFinal(
                "Autorización para salida pedagógica",
                true
            )

        assertTrue(
            resultado.endsWith(
                ReglasComunicado.AVISO_FIRMA_FISICA
            )
        )
    }

    // Un comunicado normal no debe incluir el aviso.
    @Test
    fun comunicadoNormalNoIncluyeAvisoDeFirma() {

        val cuerpo = "Reunión de apoderados"

        val resultado =
            ReglasComunicado.cuerpoFinal(
                cuerpo,
                false
            )

        assertEquals(cuerpo, resultado)
    }

    // Solo Inspectoría puede modificar canales.
    @Test
    fun inspectoriaPuedeConfigurarCanales() {

        assertTrue(
            ReglasComunicado.puedeConfigurarCanal(
                inspectoria
            )
        )
    }

    // El Profesor Jefe solo puede consultar los canales.
    @Test
    fun profesorNoPuedeConfigurarCanales() {

        assertFalse(
            ReglasComunicado.puedeConfigurarCanal(
                profesor
            )
        )
    }

    // Un usuario sin sesión no puede modificar canales.
    @Test
    fun usuarioSinSesionNoPuedeConfigurarCanales() {

        assertFalse(
            ReglasComunicado.puedeConfigurarCanal(null)
        )
    }
}