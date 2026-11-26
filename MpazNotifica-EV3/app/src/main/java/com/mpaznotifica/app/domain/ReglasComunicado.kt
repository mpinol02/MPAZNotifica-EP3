package com.mpaznotifica.app.domain

import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.ErroresComunicado
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario

/**
 * Reglas de negocio de los comunicados.
 *
 * RF-02: opciones según perfil.
 * RF-03: validación del formulario.
 * RF-04: resolución de destinatarios.
 * RF-05: permisos de Inspectoría.
 * RF-06: restricciones del Profesor Jefe.
 * RF-12: autorización con firma física.
 * RF-13: canal preferente.
 *
 * EV3: el envío individual a apoderados del propio curso
 * se mantiene pendiente de confirmación docente.
 */
object ReglasComunicado {

    const val MIN_ASUNTO = 3
    const val MAX_ASUNTO = 100
    const val MIN_CUERPO = 10
    const val MAX_CUERPO = 1000

    // Tamaño máximo del documento adjunto.
    const val MAX_ADJUNTO_BYTES = 5L * 1024 * 1024
    const val TIPO_MIME_PDF = "application/pdf"

    const val MSG_SOLO_CURSO = "Solo puedes enviar comunicados a tu curso"
    const val MSG_TIPO_NO_DISPONIBLE = "Este tipo de comunicado no está disponible para tu perfil"
    const val MSG_SIN_SESION = "Debes iniciar sesión para enviar comunicados"
    const val MSG_SIN_CURSO = "Tu usuario no tiene un curso asignado"
    const val MSG_SIN_DESTINATARIOS = "No hay apoderados para este destino"

    const val AVISO_FIRMA_FISICA =
        "IMPORTANTE: esta autorización requiere firma manuscrita del apoderado. " +
                "Debe firmarse en papel y entregarse en el establecimiento."

    // Define los tipos de comunicación permitidos por perfil.
    fun tiposPermitidos(usuario: Usuario?): List<TipoComunicacion> =
        when (usuario?.perfil) {

            Perfil.INSPECTORIA_GENERAL ->
                TipoComunicacion.entries.toList()

            Perfil.PROFESOR_JEFE -> listOf(
                TipoComunicacion.PEDAGOGICA,
                TipoComunicacion.CITACION,
                TipoComunicacion.SALIDA_PEDAGOGICA
            )

            null -> emptyList()
        }

    // Define los alcances permitidos según el usuario.
    fun alcancesPermitidos(usuario: Usuario?): List<AlcanceEnvio> =
        when (usuario?.perfil) {

            Perfil.INSPECTORIA_GENERAL ->
                AlcanceEnvio.entries.toList()

            Perfil.PROFESOR_JEFE -> listOf(
                AlcanceEnvio.CURSO,
                AlcanceEnvio.APODERADO_INDIVIDUAL
            )

            null -> emptyList()
        }

    // Obtiene únicamente los apoderados del curso indicado.
    fun apoderadosDelCurso(
        curso: String?,
        datos: DatosReferencia
    ): List<Apoderado> {

        if (curso == null) return emptyList()

        val idsEstudiantes = datos.estudiantes
            .filter { it.curso == curso }
            .map { it.id }
            .toSet()

        return datos.apoderados.filter { apoderado ->
            apoderado.pupilosIds.any {
                it in idsEstudiantes
            }
        }
    }

    // EV3: evita seleccionar apoderados con nombres ambiguos.
    private fun nombresIndividuales(
        apoderados: List<Apoderado>,
        datos: DatosReferencia
    ): List<String> =
        apoderados
            .map { it.nombreCompleto }
            .distinct()
            .filter { nombre ->
                resolverApoderados(
                    AlcanceEnvio.APODERADO_INDIVIDUAL,
                    nombre,
                    datos
                ).size == 1
            }

    // RF-06: determina los destinos disponibles según el perfil.
    fun destinosPara(
        alcance: AlcanceEnvio?,
        usuario: Usuario?,
        datos: DatosReferencia
    ): List<String> {

        if (
            usuario == null ||
            alcance == null ||
            alcance !in alcancesPermitidos(usuario)
        ) {
            return emptyList()
        }

        return if (usuario.perfil == Perfil.PROFESOR_JEFE) {

            when (alcance) {

                AlcanceEnvio.CURSO ->
                    listOfNotNull(usuario.cursoAsignado)

                AlcanceEnvio.APODERADO_INDIVIDUAL ->
                    nombresIndividuales(
                        apoderadosDelCurso(
                            usuario.cursoAsignado,
                            datos
                        ),
                        datos
                    )

                AlcanceEnvio.TALLER,
                AlcanceEnvio.COLEGIO_COMPLETO ->
                    emptyList()
            }

        } else {

            when (alcance) {

                AlcanceEnvio.CURSO ->
                    datos.cursos

                AlcanceEnvio.TALLER ->
                    datos.talleres.map { it.nombre }

                AlcanceEnvio.APODERADO_INDIVIDUAL ->
                    nombresIndividuales(
                        datos.apoderados,
                        datos
                    )

                AlcanceEnvio.COLEGIO_COMPLETO ->
                    emptyList()
            }
        }
    }

    // Obtiene los destinatarios reales sin duplicarlos.
    fun resolverApoderados(
        alcance: AlcanceEnvio,
        destino: String?,
        datos: DatosReferencia
    ): List<Apoderado> =

        when (alcance) {

            AlcanceEnvio.COLEGIO_COMPLETO ->
                datos.apoderados

            AlcanceEnvio.CURSO ->
                apoderadosDelCurso(destino, datos)

            AlcanceEnvio.TALLER -> {

                val tallerId = datos.talleres
                    .find { it.nombre == destino }
                    ?.id

                val idsEstudiantes = datos.estudiantes
                    .filter {
                        tallerId != null &&
                                tallerId in it.talleresIds
                    }
                    .map { it.id }
                    .toSet()

                datos.apoderados.filter { apoderado ->
                    apoderado.pupilosIds.any {
                        it in idsEstudiantes
                    }
                }
            }

            AlcanceEnvio.APODERADO_INDIVIDUAL -> {

                // Un envío individual debe tener un solo destinatario.
                val coincidencias = datos.apoderados
                    .filter { it.nombreCompleto == destino }
                    .distinctBy { it.id }

                if (coincidencias.size == 1) {
                    coincidencias
                } else {
                    emptyList()
                }
            }

        }.distinctBy { it.id }

    // Valida los campos y las reglas de negocio del comunicado.
    fun validar(
        asunto: String,
        cuerpo: String,
        tipo: TipoComunicacion?,
        alcance: AlcanceEnvio?,
        destino: String?,
        usuario: Usuario?,
        datos: DatosReferencia
    ): ErroresComunicado {

        // Valida el asunto.
        val errorAsunto = when {

            asunto.isBlank() ->
                "El asunto es obligatorio"

            asunto.trim().length < MIN_ASUNTO ->
                "El asunto debe tener al menos $MIN_ASUNTO caracteres"

            asunto.trim().length > MAX_ASUNTO ->
                "El asunto no puede superar los $MAX_ASUNTO caracteres"

            else -> null
        }

        // Valida el contenido del mensaje.
        val errorCuerpo = when {

            cuerpo.isBlank() ->
                "El cuerpo del mensaje es obligatorio"

            cuerpo.trim().length < MIN_CUERPO ->
                "El mensaje debe tener al menos $MIN_CUERPO caracteres"

            cuerpo.trim().length > MAX_CUERPO ->
                "El mensaje no puede superar los $MAX_CUERPO caracteres"

            else -> null
        }

        // Impide enviar comunicados sin sesión.
        if (usuario == null) {

            return ErroresComunicado(
                asunto = errorAsunto,
                cuerpo = errorCuerpo,
                alcance = MSG_SIN_SESION
            )
        }

        // Comprueba que el tipo esté permitido.
        val errorTipo = when {

            tipo == null ->
                "Selecciona el tipo de comunicación"

            tipo !in tiposPermitidos(usuario) ->
                MSG_TIPO_NO_DISPONIBLE

            else -> null
        }

        val esProfesor =
            usuario.perfil == Perfil.PROFESOR_JEFE

        // RF-06: comprueba las restricciones del alcance.
        val errorAlcance = when {

            alcance == null ->
                "Selecciona el alcance"

            esProfesor && usuario.cursoAsignado.isNullOrBlank() ->
                MSG_SIN_CURSO

            alcance !in alcancesPermitidos(usuario) ->

                if (esProfesor) {
                    MSG_SOLO_CURSO
                } else {
                    "Tu perfil no puede emitir a ${alcance.etiqueta}"
                }

            alcance == AlcanceEnvio.COLEGIO_COMPLETO &&
                    datos.apoderados.isEmpty() ->
                MSG_SIN_DESTINATARIOS

            else -> null
        }

        // Valida el destino antes de registrar el envío.
        val errorDestino = when {

            alcance == null || errorAlcance != null ->
                null

            alcance == AlcanceEnvio.COLEGIO_COMPLETO ->
                null

            destino.isNullOrBlank() ->
                "Selecciona el destino"

            // EV3: impide envíos individuales ambiguos.
            alcance == AlcanceEnvio.APODERADO_INDIVIDUAL &&
                    datos.apoderados
                        .filter { it.nombreCompleto == destino }
                        .distinctBy { it.id }
                        .size > 1 ->
                "El nombre del apoderado es ambiguo; selecciona otro destino"

            destino !in destinosPara(alcance, usuario, datos) ->

                if (esProfesor) {
                    MSG_SOLO_CURSO
                } else {
                    "El destino seleccionado no es válido"
                }

            resolverApoderados(
                alcance,
                destino,
                datos
            ).isEmpty() ->
                MSG_SIN_DESTINATARIOS

            else -> null
        }

        // Devuelve los errores de cada campo.
        return ErroresComunicado(
            asunto = errorAsunto,
            cuerpo = errorCuerpo,
            tipo = errorTipo,
            alcance = errorAlcance,
            destino = errorDestino
        )
    }

    // Comprueba el formato y tamaño del PDF.
    fun errorAdjunto(
        tipoMime: String?,
        tamanoBytes: Long?
    ): String? = when {

        tipoMime != TIPO_MIME_PDF ->
            "El archivo adjunto debe ser un PDF"

        tamanoBytes != null &&
                tamanoBytes > MAX_ADJUNTO_BYTES ->
            "El PDF supera el máximo de 5 MB"

        else -> null
    }

    // RF-12: las autorizaciones se informan por correo.
    fun canalDe(
        apoderado: Apoderado,
        requiereFirmaFisica: Boolean
    ): CanalDespacho =

        if (requiereFirmaFisica) {
            CanalDespacho.EMAIL
        } else {
            apoderado.canalPreferente
        }

    // Retira el aviso de firma física cuando ya existe.
    fun sinAviso(cuerpo: String): String {

        val texto = cuerpo.trim()

        return if (texto.endsWith(AVISO_FIRMA_FISICA)) {

            texto.removeSuffix(AVISO_FIRMA_FISICA).trim()

        } else {

            texto
        }
    }

    // Agrega el aviso de entrega física al comunicado.
    fun cuerpoFinal(
        cuerpo: String,
        requiereFirmaFisica: Boolean
    ): String =

        if (requiereFirmaFisica) {

            "${cuerpo.trim()}\n\n$AVISO_FIRMA_FISICA"

        } else {

            cuerpo.trim()
        }

    // RF-13: solo Inspectoría modifica el canal preferente.
    fun puedeConfigurarCanal(usuario: Usuario?): Boolean =
        usuario?.perfil == Perfil.INSPECTORIA_GENERAL

    // Determina los apoderados visibles según el perfil.
    fun apoderadosVisibles(
        usuario: Usuario?,
        datos: DatosReferencia
    ): List<Apoderado> =

        when (usuario?.perfil) {

            Perfil.INSPECTORIA_GENERAL ->
                datos.apoderados

            Perfil.PROFESOR_JEFE ->
                apoderadosDelCurso(
                    usuario.cursoAsignado,
                    datos
                )

            null -> emptyList()
        }
}
