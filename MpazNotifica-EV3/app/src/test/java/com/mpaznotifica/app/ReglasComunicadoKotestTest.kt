package com.mpaznotifica.app

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.domain.DatosReferencia
import com.mpaznotifica.app.domain.ReglasComunicado
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.model.TipoComunicacion

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * EV3: pruebas Kotest de las reglas de negocio.
 *
 * Comprueba destinatarios, permisos y validaciones.
 */
class ReglasComunicadoKotestTest : FunSpec({

    // Usuarios utilizados durante las pruebas.
    val inspectoria = MockData.usuarios.first {
        it.perfil == Perfil.INSPECTORIA_GENERAL
    }

    val profesor = MockData.usuarios.first {
        it.id == "U2"
    }

    val datos = DatosReferencia(
        estudiantes = MockData.estudiantes,
        apoderados = MockData.apoderados,
        talleres = MockData.talleres
    )

    test("Inspectoría dispone de todos los tipos de comunicación") {

        ReglasComunicado.tiposPermitidos(inspectoria)
            .toSet() shouldBe TipoComunicacion.entries.toSet()
    }

    test("Inspectoría dispone de los cuatro alcances") {

        ReglasComunicado.alcancesPermitidos(inspectoria)
            .toSet() shouldBe AlcanceEnvio.entries.toSet()
    }

    test("El profesor solo dispone de los alcances de su curso") {

        ReglasComunicado.alcancesPermitidos(profesor)
            .toSet() shouldBe setOf(
            AlcanceEnvio.CURSO,
            AlcanceEnvio.APODERADO_INDIVIDUAL
        )
    }

    test("El profesor no puede seleccionar otro curso") {

        ReglasComunicado.destinosPara(
            AlcanceEnvio.CURSO,
            profesor,
            datos
        ) shouldBe listOf("4° Básico A")
    }

    test("El curso 4 A resuelve tres apoderados") {

        val resultado = ReglasComunicado.resolverApoderados(
            AlcanceEnvio.CURSO,
            "4° Básico A",
            datos
        )

        resultado.map { it.id }.toSet() shouldBe
                setOf("AP1", "AP2", "AP4")
    }

    test("El curso 2 A resuelve solamente su apoderado") {

        val resultado = ReglasComunicado.resolverApoderados(
            AlcanceEnvio.CURSO,
            "2° Básico A",
            datos
        )

        resultado.map { it.id } shouldBe listOf("AP3")
    }

    test("Colegio completo resuelve todos los apoderados") {

        val resultado = ReglasComunicado.resolverApoderados(
            AlcanceEnvio.COLEGIO_COMPLETO,
            null,
            datos
        )

        resultado.size shouldBe MockData.apoderados.size
    }

    test("Un destino individual inexistente no tiene destinatarios") {

        val resultado = ReglasComunicado.resolverApoderados(
            AlcanceEnvio.APODERADO_INDIVIDUAL,
            "Apoderado inexistente",
            datos
        )

        resultado.isEmpty() shouldBe true
    }

    test("Un formulario vacío contiene errores") {

        val errores = ReglasComunicado.validar(
            asunto = "",
            cuerpo = "",
            tipo = null,
            alcance = null,
            destino = null,
            usuario = inspectoria,
            datos = datos
        )

        errores.hayErrores shouldBe true
        (errores.asunto != null) shouldBe true
        (errores.cuerpo != null) shouldBe true
        (errores.tipo != null) shouldBe true
        (errores.alcance != null) shouldBe true
    }

    test("Un comunicado institucional válido no contiene errores") {

        val errores = ReglasComunicado.validar(
            asunto = "Comunicado de prueba",
            cuerpo = "Información importante para todos los apoderados.",
            tipo = TipoComunicacion.INSTITUCIONAL,
            alcance = AlcanceEnvio.COLEGIO_COMPLETO,
            destino = null,
            usuario = inspectoria,
            datos = datos
        )

        errores.hayErrores shouldBe false
    }

    test("Un profesor no puede enviar a otro curso") {

        val errores = ReglasComunicado.validar(
            asunto = "Reunión de apoderados",
            cuerpo = "Se informa una reunión de apoderados.",
            tipo = TipoComunicacion.CITACION,
            alcance = AlcanceEnvio.CURSO,
            destino = "2° Básico A",
            usuario = profesor,
            datos = datos
        )

        errores.destino shouldBe
                ReglasComunicado.MSG_SOLO_CURSO
    }

    test("Un profesor no puede enviar a todo el colegio") {

        val errores = ReglasComunicado.validar(
            asunto = "Reunión de apoderados",
            cuerpo = "Se informa una reunión de apoderados.",
            tipo = TipoComunicacion.CITACION,
            alcance = AlcanceEnvio.COLEGIO_COMPLETO,
            destino = null,
            usuario = profesor,
            datos = datos
        )

        errores.alcance shouldBe
                ReglasComunicado.MSG_SOLO_CURSO
    }

    test("El asunto no puede superar la longitud máxima") {

        val errores = ReglasComunicado.validar(
            asunto = "A".repeat(
                ReglasComunicado.MAX_ASUNTO + 1
            ),
            cuerpo = "Contenido válido del comunicado.",
            tipo = TipoComunicacion.INSTITUCIONAL,
            alcance = AlcanceEnvio.COLEGIO_COMPLETO,
            destino = null,
            usuario = inspectoria,
            datos = datos
        )

        (errores.asunto != null) shouldBe true
    }

    test("El cuerpo no puede superar la longitud máxima") {

        val errores = ReglasComunicado.validar(
            asunto = "Comunicado de prueba",
            cuerpo = "A".repeat(
                ReglasComunicado.MAX_CUERPO + 1
            ),
            tipo = TipoComunicacion.INSTITUCIONAL,
            alcance = AlcanceEnvio.COLEGIO_COMPLETO,
            destino = null,
            usuario = inspectoria,
            datos = datos
        )

        (errores.cuerpo != null) shouldBe true
    }

    test("El envío individual rechaza nombres ambiguos") {

        // Simula dos apoderados con el mismo nombre.
        val original = MockData.apoderados.first()

        val duplicado = original.copy(
            id = "AP-DUPLICADO"
        )

        val datosDuplicados = datos.copy(
            apoderados = datos.apoderados + duplicado
        )

        val resultado = ReglasComunicado.resolverApoderados(
            AlcanceEnvio.APODERADO_INDIVIDUAL,
            original.nombreCompleto,
            datosDuplicados
        )

        resultado.isEmpty() shouldBe true
    }

    test("Solo Inspectoría puede modificar canales preferentes") {

        ReglasComunicado.puedeConfigurarCanal(
            inspectoria
        ) shouldBe true

        ReglasComunicado.puedeConfigurarCanal(
            profesor
        ) shouldBe false

        ReglasComunicado.puedeConfigurarCanal(
            null
        ) shouldBe false
    }
})
