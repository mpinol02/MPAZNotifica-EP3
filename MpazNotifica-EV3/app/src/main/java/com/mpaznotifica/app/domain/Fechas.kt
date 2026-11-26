package com.mpaznotifica.app.domain

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utilidades de fecha compatibles con Android API 24.
 *
 * EV3: valida el formato completo antes de convertir fechas.
 */
object Fechas {

    private const val PATRON_FECHA_HORA =
        "yyyy-MM-dd HH:mm:ss"

    private const val PATRON_FECHA =
        "yyyy-MM-dd"

    // Obtiene la fecha y hora actual del dispositivo.
    fun ahora(): String =
        SimpleDateFormat(
            PATRON_FECHA_HORA,
            Locale.ROOT
        ).format(Date())

    // Convierte los milisegundos del DatePicker a fecha.
    fun deMillisUtc(millis: Long): String {

        val formato = SimpleDateFormat(
            PATRON_FECHA,
            Locale.ROOT
        )

        formato.timeZone =
            TimeZone.getTimeZone("UTC")

        return formato.format(Date(millis))
    }

    // Convierte una fecha válida a milisegundos UTC.
    fun aMillisUtc(fecha: String): Long? {

        // EV3: valida la fecha antes de convertirla.
        if (!ReglasHistorial.fechaValida(fecha)) {
            return null
        }

        val formato = SimpleDateFormat(
            PATRON_FECHA,
            Locale.ROOT
        )

        formato.timeZone =
            TimeZone.getTimeZone("UTC")

        formato.isLenient = false

        return try {

            formato.parse(fecha)?.time

        } catch (e: ParseException) {

            null
        }
    }

    // Convierte la fecha y hora a un formato legible.
    fun legible(fechaHora: String): String {

        val coincidencia = Regex(
            """^(\d{4})-(\d{2})-(\d{2}) (\d{2}:\d{2})"""
        ).find(fechaHora) ?: return fechaHora

        val (anio, mes, dia, hora) =
            coincidencia.destructured

        return "$dia-$mes-$anio $hora"
    }

    // Muestra una fecha en formato DD-MM-AAAA.
    fun fechaLegible(fecha: String): String {

        val partes = fecha.split("-")

        return if (partes.size == 3) {

            "${partes[2]}-${partes[1]}-${partes[0]}"

        } else {

            fecha
        }
    }
}
