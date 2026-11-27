package com.mpaznotifica.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Lista desplegable reutilizable de Material 3.
 *
 * Permite seleccionar tipos, alcances,
 * destinatarios y canales de comunicación.
 *
 * @param opciones Valores disponibles.
 * @param seleccion Valor seleccionado actualmente.
 * @param textoDe Texto que representa cada opción.
 * @param error Mensaje de validación.
 * @param ayuda Texto de apoyo del selector.
 * @param leadingIcon Icono inicial del campo.
 * @param enabled Indica si el campo está habilitado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectorDesplegable(
    etiqueta: String,
    opciones: List<T>,
    seleccion: T?,
    textoDe: (T) -> String,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier,
    textoVacio: String = "Seleccionar…",
    error: String? = null,
    ayuda: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {

    // Controla la apertura y cierre de la lista.
    var expandido by remember {
        mutableStateOf(false)
    }

    // El mensaje de error tiene prioridad sobre la ayuda.
    val textoApoyo = error ?: ayuda

    // Cierra la lista si el selector queda deshabilitado.
    LaunchedEffect(enabled) {

        if (!enabled) {
            expandido = false
        }
    }

    ExposedDropdownMenuBox(
        expanded = expandido && enabled,

        onExpandedChange = {

            if (enabled) {
                expandido = it
            }
        },

        modifier = modifier
    ) {

        OutlinedTextField(

            // Muestra la opción seleccionada.
            value = if (seleccion != null) {
                textoDe(seleccion)
            } else {
                textoVacio
            },

            onValueChange = {},

            // Solo permite seleccionar opciones de la lista.
            readOnly = true,

            enabled = enabled,

            label = {
                Text(etiqueta)
            },

            leadingIcon = leadingIcon,

            trailingIcon = {

                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expandido && enabled
                )
            },

            // Destaca el campo cuando tiene errores.
            isError = error != null,

            supportingText = apoyo(textoApoyo),

            modifier = Modifier
                .fillMaxWidth()

                // EV3: reemplaza menuAnchor() obsoleto.
                // El campo es de solo lectura.
                .menuAnchor(
                    type = MenuAnchorType.PrimaryNotEditable,
                    enabled = enabled
                )
        )

        ExposedDropdownMenu(

            expanded = expandido && enabled,

            onDismissRequest = {
                expandido = false
            }
        ) {

            opciones.forEach { opcion ->

                DropdownMenuItem(

                    text = {
                        Text(textoDe(opcion))
                    },

                    enabled = enabled,

                    onClick = {

                        // Entrega la opción seleccionada.
                        onSeleccion(opcion)

                        // Cierra la lista después de elegir.
                        expandido = false
                    }
                )
            }
        }
    }
}
