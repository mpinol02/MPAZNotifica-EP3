package com.mpaznotifica.app.domain

import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.TipoComunicacion

/**
 * Filtros del historial (RF-10).
 * null significa que el filtro no está activo.
 */
data class FiltrosHistorial(
    val tipo: TipoComunicacion? = null,
    val emisorId: String? = null,
    val desde: String? = null,
    val hasta: String? = null
) {

    // Cuenta la cantidad de filtros activos.
    val cantidadActivos: Int
        get() = listOf(tipo, emisorId, desde, hasta)
            .count { it != null }
}

/**
 * Reglas del historial independientes de Android.
 *
 * EV3: valida fechas reales y rangos cronológicos.
 */
object ReglasHistorial {

    const val MSG_FECHAS_INVERTIDAS =
        "La fecha Desde no puede ser posterior a la fecha Hasta"

    const val MSG_FORMATO_FECHA =
        "Usa el formato AAAA-MM-DD"

    private val regexFecha =
        Regex("""^\d{4}-\d{2}-\d{2}$""")

    // Comprueba que la fecha exista en el calendario.
    fun fechaValida(texto: String): Boolean {

        if (!regexFecha.matches(texto)) {
            return false
        }

        val anio = texto.substring(0, 4).toInt()
        val mes = texto.substring(5, 7).toInt()
        val dia = texto.substring(8, 10).toInt()

        // Rechaza años y meses incorrectos.
        if (anio == 0 || mes !in 1..12) {
            return false
        }

        // Comprueba si el año es bisiesto.
        val bisiesto =
            (anio % 4 == 0 && anio % 100 != 0) ||
                    anio % 400 == 0

        val diasDelMes = when (mes) {

            2 -> if (bisiesto) 29 else 28

            4, 6, 9, 11 -> 30

            else -> 31
        }

        return dia in 1..diasDelMes
    }

    // Comprueba el formato y orden de las fechas.
    fun errorFechas(
        desde: String?,
        hasta: String?
    ): String? {

        val d = desde?.trim().orEmpty()
        val h = hasta?.trim().orEmpty()

        if (
            (d.isNotEmpty() && !fechaValida(d)) ||
            (h.isNotEmpty() && !fechaValida(h))
        ) {
            return MSG_FORMATO_FECHA
        }

        // Impide que la fecha inicial supere a la final.
        if (
            d.isNotEmpty() &&
            h.isNotEmpty() &&
            d > h
        ) {
            return MSG_FECHAS_INVERTIDAS
        }

        return null
    }

    // Aplica los filtros y ordena los comunicados.
    fun aplicar(
        todos: List<Comunicacion>,
        filtros: FiltrosHistorial
    ): List<Comunicacion> {

        // Si hay un error, no aplica el rango de fechas.
        val usarFechas =
            errorFechas(
                filtros.desde,
                filtros.hasta
            ) == null

        val desde = filtros.desde?.trim()?.takeIf {
            usarFechas && it.isNotEmpty()
        }

        val hasta = filtros.hasta?.trim()?.takeIf {
            usarFechas && it.isNotEmpty()
        }

        return todos

            // Filtra por tipo de comunicado.
            .filter {
                filtros.tipo == null ||
                        it.tipo == filtros.tipo
            }

            // Filtra por usuario emisor.
            .filter {
                filtros.emisorId == null ||
                        it.autorId == filtros.emisorId
            }

            // Aplica la fecha inicial.
            .filter {
                desde == null ||
                        it.fechaHora.take(10) >= desde
            }

            // Aplica la fecha final.
            .filter {
                hasta == null ||
                        it.fechaHora.take(10) <= hasta
            }

            // Ordena del más reciente al más antiguo.
            .sortedByDescending {
                it.fechaHora
            }
    }
}
