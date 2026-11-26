package com.mpaznotifica.app

import com.mpaznotifica.app.domain.Fechas
import com.mpaznotifica.app.domain.ReglasHistorial

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * EV3: pruebas JUnit 5 del manejo de fechas.
 */
@DisplayName("Validaciones de fechas e historial")
class FechasJUnit5Test {

    // Comprueba distintas fechas válidas.
    @ParameterizedTest
    @ValueSource(
        strings = [
            "2026-11-26",
            "2024-02-29",
            "2000-02-29",
            "2026-01-01",
            "2026-12-31"
        ]
    )
    fun aceptaFechasValidas(fecha: String) {

        assertTrue(
            ReglasHistorial.fechaValida(fecha)
        )
    }

    // Comprueba distintos casos de fechas incorrectas.
    @ParameterizedTest
    @ValueSource(
        strings = [
            "2026-02-29",
            "1900-02-29",
            "2026-02-30",
            "2026-04-31",
            "2026-13-01",
            "2026-00-01",
            "2026-01-00",
            "0000-01-01",
            "26-11-2026",
            "2026-1-1",
            "2026-11-26 texto",
            ""
        ]
    )
    fun rechazaFechasInvalidas(fecha: String) {

        assertFalse(
            ReglasHistorial.fechaValida(fecha)
        )
    }

    // Comprueba que un rango cronológico sea aceptado.
    @Test
    fun aceptaRangoDeFechasCorrecto() {

        val resultado =
            ReglasHistorial.errorFechas(
                "2026-11-25",
                "2026-11-26"
            )

        assertNull(resultado)
    }

    // No permite que Desde sea posterior a Hasta.
    @Test
    fun rechazaRangoDeFechasInvertido() {

        val resultado =
            ReglasHistorial.errorFechas(
                "2026-11-27",
                "2026-11-25"
            )

        assertEquals(
            ReglasHistorial.MSG_FECHAS_INVERTIDAS,
            resultado
        )
    }

    // Permite buscar comunicados de un mismo día.
    @Test
    fun aceptaLaMismaFechaEnAmbosCampos() {

        assertNull(
            ReglasHistorial.errorFechas(
                "2026-11-26",
                "2026-11-26"
            )
        )
    }

    // Permite utilizar solo la fecha inicial.
    @Test
    fun aceptaFiltroSoloDesde() {

        assertNull(
            ReglasHistorial.errorFechas(
                "2026-11-26",
                null
            )
        )
    }

    // Permite utilizar solo la fecha final.
    @Test
    fun aceptaFiltroSoloHasta() {

        assertNull(
            ReglasHistorial.errorFechas(
                null,
                "2026-11-26"
            )
        )
    }

    // Permite consultar sin filtros de fecha.
    @Test
    fun aceptaAmbasFechasVacias() {

        assertNull(
            ReglasHistorial.errorFechas(
                null,
                null
            )
        )
    }

    // Una fecha imposible debe generar un error.
    @Test
    fun rechazaFechaInexistenteEnRango() {

        assertEquals(
            ReglasHistorial.MSG_FORMATO_FECHA,

            ReglasHistorial.errorFechas(
                "2026-02-30",
                "2026-03-01"
            )
        )
    }

    // Comprueba la conversión de fecha a milisegundos.
    @Test
    fun convierteFechaAMilisegundos() {

        val resultado =
            Fechas.aMillisUtc("2026-11-26")

        assertNotNull(resultado)
    }

    // Impide convertir una fecha incorrecta.
    @Test
    fun noConvierteFechaInvalida() {

        assertNull(
            Fechas.aMillisUtc("2026-02-30")
        )
    }

    // Impide aceptar caracteres adicionales.
    @Test
    fun noConvierteFechaConTextoAdicional() {

        assertNull(
            Fechas.aMillisUtc("2026-11-26 incorrecto")
        )
    }

    // Comprueba que ambas conversiones sean coherentes.
    @Test
    fun conversionDeFechaCompletaEsCorrecta() {

        val fechaOriginal = "2026-11-26"

        val millis =
            Fechas.aMillisUtc(fechaOriginal)

        assertNotNull(millis)

        val fechaConvertida =
            Fechas.deMillisUtc(millis!!)

        assertEquals(
            fechaOriginal,
            fechaConvertida
        )
    }

    // Comprueba el formato visible en la interfaz.
    @Test
    fun muestraFechaEnFormatoLegible() {

        assertEquals(
            "26-11-2026",

            Fechas.fechaLegible("2026-11-26")
        )
    }

    // Comprueba el formato del historial.
    @Test
    fun muestraFechaYHoraLegibles() {

        assertEquals(
            "26-11-2026 14:30",

            Fechas.legible(
                "2026-11-26 14:30:00"
            )
        )
    }
}