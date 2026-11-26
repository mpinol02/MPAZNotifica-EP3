package com.mpaznotifica.app.nativo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.mpaznotifica.app.domain.ReglasComunicado
import com.mpaznotifica.app.model.AdjuntoPdf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * RECURSO NATIVO: selección, almacenamiento y apertura de PDF.
 *
 * Utiliza el selector de archivos de Android.
 *
 * EV3:
 * - Conserva el permiso de lectura cuando está disponible.
 * - Guarda una copia interna del documento.
 * - Permite recuperar y abrir el PDF desde el detalle.
 */
object ArchivosPdf {

    // Resultado de la validación del archivo seleccionado.
    data class Inspeccion(
        val adjunto: AdjuntoPdf?,
        val error: String?
    )

    // Obtiene y valida los datos del PDF seleccionado.
    fun inspeccionar(
        context: Context,
        uri: Uri
    ): Inspeccion {

        val tipo = context.contentResolver.getType(uri)

        try {
            // Conserva el permiso de lectura del documento.
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Si el proveedor no lo permite, usa el permiso temporal.
        }

        var nombre = "documento.pdf"
        var tamano: Long? = null

        try {

            // Consulta el nombre y tamaño del archivo.
            context.contentResolver.query(
                uri,
                null,
                null,
                null,
                null
            )?.use { cursor ->

                if (cursor.moveToFirst()) {

                    val columnaNombre = cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                    if (columnaNombre >= 0) {
                        cursor.getString(columnaNombre)?.let {
                            nombre = it
                        }
                    }

                    val columnaTamano = cursor.getColumnIndex(
                        OpenableColumns.SIZE
                    )

                    if (
                        columnaTamano >= 0 &&
                        !cursor.isNull(columnaTamano)
                    ) {
                        tamano = cursor.getLong(columnaTamano)
                    }
                }
            }

        } catch (e: Exception) {

            return Inspeccion(
                null,
                "No se pudo leer el archivo seleccionado"
            )
        }

        // Valida el tipo y tamaño mediante las reglas del dominio.
        val error = ReglasComunicado.errorAdjunto(tipo, tamano)

        return if (error != null) {

            Inspeccion(null, error)

        } else {

            Inspeccion(
                AdjuntoPdf(
                    nombre,
                    tamano ?: 0L,
                    uri.toString()
                ),
                null
            )
        }
    }

    // EV3: obtiene la ubicación interna del PDF.
    private fun archivoInterno(
        context: Context,
        comunicacionId: String,
        nombre: String
    ): File {

        val carpeta = File(context.filesDir, "adjuntos")

        // Evita utilizar rutas externas como nombres de archivo.
        val nombreSeguro = File(nombre).name
            .ifBlank { "documento.pdf" }

        val archivo = File(
            carpeta,
            "${comunicacionId}_$nombreSeguro"
        ).canonicalFile

        // Comprueba que el archivo pertenezca a la carpeta autorizada.
        require(archivo.parentFile == carpeta.canonicalFile)

        return archivo
    }

    // Guarda una copia del PDF en el almacenamiento interno.
    suspend fun guardarCopia(
        context: Context,
        adjunto: AdjuntoPdf,
        comunicacionId: String
    ): String? = withContext(Dispatchers.IO) {

        try {

            val destino = archivoInterno(
                context,
                comunicacionId,
                adjunto.nombre
            )

            destino.parentFile?.mkdirs()

            // Abre el archivo seleccionado por el usuario.
            val entrada = context.contentResolver.openInputStream(
                Uri.parse(adjunto.uri)
            )

            if (entrada == null) {

                null

            } else {

                try {

                    // Copia el documento al almacenamiento privado.
                    entrada.use { origen ->
                        destino.outputStream().use { salida ->
                            origen.copyTo(salida)
                        }
                    }

                    // Conserva el nombre visible en el comunicado.
                    adjunto.nombre

                } catch (e: Exception) {

                    // Elimina una copia incompleta si ocurre un error.
                    destino.delete()
                    throw e
                }
            }

        } catch (e: CancellationException) {

            throw e

        } catch (e: Exception) {

            null
        }
    }

    // EV3: comprueba si el PDF sigue guardado en el dispositivo.
    fun existeCopia(
        context: Context,
        comunicacionId: String,
        nombre: String
    ): Boolean {

        return try {

            archivoInterno(
                context,
                comunicacionId,
                nombre
            ).isFile

        } catch (e: Exception) {

            false
        }
    }

    // EV3: recupera y abre el PDF asociado al comunicado.
    // Devuelve null si funciona o un mensaje si ocurre un error.
    fun abrirPdf(
        context: Context,
        comunicacionId: String,
        nombre: String
    ): String? {

        return try {

            val archivo = archivoInterno(
                context,
                comunicacionId,
                nombre
            )

            // Comprueba que el documento exista.
            if (!archivo.isFile) {
                return "El PDF no está disponible en este dispositivo."
            }

            // Genera una URI segura mediante FileProvider.
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                archivo
            )

            // Solicita abrir el documento con un lector PDF.
            val intencion = Intent(Intent.ACTION_VIEW).apply {

                setDataAndType(uri, "application/pdf")

                // Concede acceso temporal de solo lectura.
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }

            context.startActivity(intencion)

            null

        } catch (e: android.content.ActivityNotFoundException) {

            "No hay una aplicación instalada para abrir PDF."

        } catch (e: SecurityException) {

            "No se pudo obtener permiso para abrir el PDF."

        } catch (e: Exception) {

            "No se pudo abrir el documento PDF."
        }
    }
}
