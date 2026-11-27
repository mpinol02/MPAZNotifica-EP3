package com.mpaznotifica.app

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.domain.FiltrosHistorial
import com.mpaznotifica.app.domain.ReglasHistorial
import com.mpaznotifica.app.model.TipoComunicacion

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * EV3: pruebas Kotest del historial.
 *
 * Comprueba filtros, orden y rangos de fechas.
 */
class ReglasHistorialKotestTest : FunSpec({

    // Datos de demostración utilizados por la aplicación.
    val comunicados = MockData.comunicados

    test("Sin filtros aparecen todos los comunicados") {

        val resultado = ReglasHistorial.aplicar(
            comunicados,
            FiltrosHistorial()
        )

        resultado.size shouldBe comunicados.size
    }

    test("El historial se ordena del más reciente al más antiguo") {

        val resultado = ReglasHistorial.aplicar(
            comunicados,
            FiltrosHistorial()
        )

        resultado.map { it.fechaHora } shouldBe
                comunicados.map { it.fechaHora }
                    .sortedDescending()
    }

    test("El filtro institucional muestra únicamente ese tipo") {

        val resultado = ReglasHistorial.aplicar(
            comunicados,
            FiltrosHistorial(
                tipo = TipoComunicacion.INSTITUCIONAL
            )
        )

        resultado.isNotEmpty() shouldBe true

        resultado.all {
            it.tipo == TipoComunicacion.INSTITUCIONAL
        } shouldBe true
    }

    test("El filtro de emisor muestra únicamente sus comunicados") {

        val autorId = comunicados.first().autorId

        val resultado = ReglasHistorial.aplicar(
            comunicados,
            FiltrosHistorial(
                emisorId = autorId
            )
        )

        resultado.isNotEmpty() shouldBe true

        resultado.all {
            it.autorId == autorId
        } shouldBe true
    }

    test("Un emisor inexistente genera una lista vacía") {

        val resultado = ReglasHistorial.aplicar(
            comunicados,
            FiltrosHistorial(
                emisorId = "USUARIO-INEXISTENTE"
            )
        )

        resultado.isEmpty() shouldBe true
    }

    test("El filtro Desde incluye la fecha seleccionada") {

        val fecha = comunicados.first()
            .fechaHora.take(10)

        val resultado = ReglasHistorial.aplicar(
            comunicados,
            FiltrosHistorial(
                desde = fecha
            )
        )

        resultado.any {
            it.fechaHora.take(10) == fecha
        } shouldBe true

        resultado.all {
            it.fechaHora.take(10) >= fecha
        } shouldBe true
    }

    test("El filtro Hasta incluye la fecha seleccionada") {

        val fecha = comunicados.first()
            .fechaHora.take(10)

        val resultado = ReglasHistorial.aplicar(
            comunicados,
            FiltrosHistorial(
                hasta = fecha
            )
        )

        resultado.any {
            it.fechaHora.take(10) == fecha
        } shouldBe true

        resultado.all {
            it.fechaHora.take(10) <= fecha
        } shouldBe true
    }

    test("Permite consultar comunicados del mismo día") {

        val fecha = comunicados.first()
            .fechaHora.take(10)

        val resultado = ReglasHistorial.aplicar(
            comunicados,
            FiltrosHistorial(
                desde = fecha,
                hasta = fecha
            )
        )

        resultado.isNotEmpty() shouldBe true

        resultado.all {
            it.fechaHora.take(10) == fecha
        } shouldBe true
    }

    test("El rango invertido genera un mensaje de error") {

        ReglasHistorial.errorFechas(
            "2026-11-27",
            "2026-11-25"
        ) shouldBe ReglasHistorial.MSG_FECHAS_INVERTIDAS
    }

    test("Una fecha inexistente genera un mensaje de error") {

        ReglasHistorial.errorFechas(
            "2026-02-30",
            "2026-03-01"
        ) shouldBe ReglasHistorial.MSG_FORMATO_FECHA
    }

    test("Un rango inválido no aplica el filtro de fechas") {

        val resultado = ReglasHistorial.aplicar(
            comunicados,
            FiltrosHistorial(
                desde = "2026-11-27",
                hasta = "2026-11-25"
            )
        )

        resultado.size shouldBe comunicados.size
    }

    test("Los cuatro filtros se aplican simultáneamente") {

        val comunicado = comunicados.first()

        val fecha = comunicado.fechaHora.take(10)

        val filtros = FiltrosHistorial(
            tipo = comunicado.tipo,
            emisorId = comunicado.autorId,
            desde = fecha,
            hasta = fecha
        )

        val resultado = ReglasHistorial.aplicar(
            comunicados,
            filtros
        )

        resultado.isNotEmpty() shouldBe true

        resultado.all {

            it.tipo == comunicado.tipo &&
                    it.autorId == comunicado.autorId &&
                    it.fechaHora.take(10) == fecha

        } shouldBe true
    }

    test("El contador reconoce los cuatro filtros activos") {

        val filtros = FiltrosHistorial(
            tipo = TipoComunicacion.INSTITUCIONAL,
            emisorId = "U1",
            desde = "2026-01-01",
            hasta = "2026-12-31"
        )

        filtros.cantidadActivos shouldBe 4
    }

    test("Al limpiar los filtros se recupera el historial completo") {

        val filtros = FiltrosHistorial()

        filtros.cantidadActivos shouldBe 0

        ReglasHistorial.aplicar(
            comunicados,
            filtros
        ).size shouldBe comunicados.size
    }
})
