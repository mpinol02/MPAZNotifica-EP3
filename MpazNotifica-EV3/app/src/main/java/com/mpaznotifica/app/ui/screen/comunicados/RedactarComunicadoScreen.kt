package com.mpaznotifica.app.ui.screen.comunicados

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mpaznotifica.app.domain.ReglasComunicado
import com.mpaznotifica.app.model.AdjuntoPdf
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.ErroresComunicado
import com.mpaznotifica.app.model.EstadoEnvioUi
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.model.SolicitudComunicado
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.nativo.ArchivosPdf
import com.mpaznotifica.app.nativo.Notificador
import com.mpaznotifica.app.nativo.Vibracion
import com.mpaznotifica.app.ui.components.AvisoCard
import com.mpaznotifica.app.ui.components.BarraSuperior
import com.mpaznotifica.app.ui.components.SelectorDesplegable
import com.mpaznotifica.app.ui.components.apoyo
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel
import kotlinx.coroutines.launch

/**
 * Redacción y envío de comunicados.
 *
 * RF-02 a RF-08: formulario, permisos y despacho.
 * RF-12: autorización con firma física.
 *
 * EV3:
 * - Validaciones por campo.
 * - Animaciones del formulario.
 * - Progreso del despacho y almacenamiento.
 * - Confirmación y resultado del envío.
 */
@Composable
fun RedactarComunicadoScreen(
    navController: NavHostController,
    viewModel: MpazNotificaViewModel
) {

    // Obtiene el contexto y los estados del ViewModel.
    val context = LocalContext.current
    val usuario by viewModel.usuarioActivo.collectAsState()
    val datos by viewModel.datos.collectAsState()
    val estadoEnvio by viewModel.envioUi.collectAsState()
    val modoLocal by viewModel.modoLocal.collectAsState()

    // Estados de los campos del formulario.
    var asunto by rememberSaveable { mutableStateOf("") }
    var cuerpo by rememberSaveable { mutableStateOf("") }

    var tipo by rememberSaveable {
        mutableStateOf<TipoComunicacion?>(null)
    }

    var alcance by rememberSaveable {
        mutableStateOf<AlcanceEnvio?>(null)
    }

    var destino by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var requiereFirma by rememberSaveable {
        mutableStateOf(false)
    }

    var intentoEnvio by rememberSaveable {
        mutableStateOf(false)
    }

    var confirmando by rememberSaveable {
        mutableStateOf(false)
    }

    var permisoSolicitado by rememberSaveable {
        mutableStateOf(false)
    }

    // Controla el documento adjunto y sus errores.
    var adjunto by remember {
        mutableStateOf<AdjuntoPdf?>(null)
    }

    var errorAdjunto by remember {
        mutableStateOf<String?>(null)
    }

    val scroll = rememberScrollState()
    val alcanceScope = rememberCoroutineScope()

    // Evita mostrar el resultado de un envío anterior.
    LaunchedEffect(Unit) {
        viewModel.reiniciarEnvio()
    }

    // Recurso nativo: vibración al finalizar el envío.
    LaunchedEffect(estadoEnvio) {

        val actual = estadoEnvio

        if (actual is EstadoEnvioUi.Listo) {
            Vibracion.confirmar(
                context,
                exito = actual.resumen.fallidos == 0
            )
        }
    }

    // Abre el selector nativo de documentos PDF.
    val selectorPdf = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->

        if (uri != null) {

            val resultado =
                ArchivosPdf.inspeccionar(context, uri)

            adjunto = resultado.adjunto
            errorAdjunto = resultado.error
        }
    }

    // Solicita el permiso de notificaciones de Android.
    val solicitudPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        confirmando = true
    }

    // Valida el formulario después del primer intento de envío.
    val errores: ErroresComunicado = if (intentoEnvio) {

        viewModel.validarComunicado(
            asunto,
            cuerpo,
            tipo,
            alcance,
            destino
        )

    } else {

        ErroresComunicado()
    }

    // Obtiene las opciones autorizadas según el perfil.
    val tiposDisponibles =
        viewModel.tiposPermitidos(usuario)

    val alcancesDisponibles =
        viewModel.alcancesPermitidos(usuario)

    val destinosDisponibles =
        remember(alcance, usuario, datos) {
            viewModel.destinosPara(alcance, usuario)
        }

    val cantidadDestinatarios =
        viewModel.contarDestinatarios(alcance, destino)

    // Impide modificar el formulario durante el envío.
    val enviando =
        estadoEnvio is EstadoEnvioUi.Enviando

    Scaffold(
        topBar = {
            BarraSuperior(
                "Redactar comunicado",
                navController
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scroll)
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // RF-06: informa las restricciones del Profesor Jefe.
            if (usuario?.perfil == Perfil.PROFESOR_JEFE) {

                AvisoCard(
                    texto = "Como Profesor/a Jefe solo puedes enviar comunicados a tu curso" +
                            (usuario?.cursoAsignado?.let {
                                " ($it)"
                            } ?: "") + ".",

                    icono = Icons.Filled.Info
                )
            }

            // =============================================
            // ASUNTO
            // =============================================

            OutlinedTextField(
                value = asunto,

                onValueChange = {
                    asunto = it
                },

                label = {
                    Text("Asunto")
                },

                leadingIcon = {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null
                    )
                },

                trailingIcon = if (errores.asunto != null) {
                    {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = "Campo con error",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    null
                },

                // Muestra el error o contador de caracteres.
                isError = errores.asunto != null,

                supportingText = apoyo(
                    errores.asunto ?:
                    "${asunto.trim().length}/${ReglasComunicado.MAX_ASUNTO}"
                ),

                singleLine = true,

                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),

                enabled = !enviando,
                modifier = Modifier.fillMaxWidth()
            )

            // =============================================
            // CUERPO DEL MENSAJE
            // =============================================

            OutlinedTextField(
                value = cuerpo,

                onValueChange = {
                    cuerpo = it
                },

                label = {
                    Text("Cuerpo del mensaje")
                },

                leadingIcon = {
                    Icon(
                        Icons.Filled.Description,
                        contentDescription = null
                    )
                },

                trailingIcon = if (errores.cuerpo != null) {
                    {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = "Campo con error",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    null
                },

                isError = errores.cuerpo != null,

                supportingText = apoyo(
                    errores.cuerpo ?:
                    "${cuerpo.trim().length}/${ReglasComunicado.MAX_CUERPO}"
                ),

                minLines = 4,
                maxLines = 8,

                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),

                enabled = !enviando,
                modifier = Modifier.fillMaxWidth()
            )

            // =============================================
            // TIPO DE COMUNICACIÓN
            // =============================================

            SelectorDesplegable(
                etiqueta = "Tipo de comunicación",
                opciones = tiposDisponibles,
                seleccion = tipo,
                textoDe = { it.etiqueta },

                onSeleccion = {
                    tipo = it
                },

                error = errores.tipo,

                leadingIcon = {
                    Icon(
                        Icons.Filled.Category,
                        contentDescription = null
                    )
                },

                enabled = !enviando
            )

            // =============================================
            // ALCANCE Y DESTINO
            // =============================================

            SelectorDesplegable(
                etiqueta = "Alcance (destinatarios)",
                opciones = alcancesDisponibles,
                seleccion = alcance,
                textoDe = { it.etiqueta },

                onSeleccion = {

                    // Limpia el destino cuando cambia el alcance.
                    alcance = it
                    destino = null
                },

                error = errores.alcance,

                leadingIcon = {
                    Icon(
                        Icons.Filled.Groups,
                        contentDescription = null
                    )
                },

                enabled = !enviando
            )

            // Anima la aparición del selector de destino.
            AnimatedVisibility(
                visible = alcance != null &&
                        alcance != AlcanceEnvio.COLEGIO_COMPLETO,

                enter = fadeIn(tween(250)) +
                        expandVertically(tween(250)),

                exit = fadeOut(tween(200)) +
                        shrinkVertically(tween(200))
            ) {

                SelectorDesplegable(
                    etiqueta = "Destino",
                    opciones = destinosDisponibles,
                    seleccion = destino,
                    textoDe = { it },

                    onSeleccion = {
                        destino = it
                    },

                    error = errores.destino,

                    leadingIcon = {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null
                        )
                    },

                    enabled = !enviando
                )
            }

            // Muestra la cantidad de destinatarios disponibles.
            AnimatedVisibility(
                visible = alcance != null &&
                        cantidadDestinatarios > 0,

                enter = fadeIn(tween(250)),
                exit = fadeOut(tween(200))
            ) {

                Text(
                    text = "Destinatarios: $cantidadDestinatarios apoderado(s). " +
                            if (requiereFirma) {
                                "Se informará por correo a todos."
                            } else {
                                "Se utilizará el canal preferente."
                            },

                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // =============================================
            // DOCUMENTO PDF
            // =============================================

            OutlinedButton(
                enabled = !enviando,

                onClick = {
                    selectorPdf.launch(
                        arrayOf("application/pdf")
                    )
                }
            ) {

                Icon(
                    Icons.Filled.AttachFile,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text("Adjuntar PDF (opcional)")
            }

            // Muestra el PDF seleccionado con una animación.
            AnimatedVisibility(
                visible = adjunto != null,

                enter = fadeIn(tween(250)) +
                        expandVertically(tween(250)),

                exit = fadeOut(tween(200)) +
                        shrinkVertically(tween(200))
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        Icons.Filled.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text(
                        text = adjunto?.let {
                            "${it.nombre} (${tamanoLegible(it.tamanoBytes)})"
                        }.orEmpty(),

                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    // Permite retirar el PDF antes del envío.
                    IconButton(
                        enabled = !enviando,

                        onClick = {
                            adjunto = null
                            errorAdjunto = null
                        }
                    ) {

                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Quitar PDF"
                        )
                    }
                }
            }

            // Informa los errores del documento seleccionado.
            AnimatedVisibility(
                visible = errorAdjunto != null
            ) {

                Column {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Text(
                            text = errorAdjunto.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // Permite continuar sin un adjunto incorrecto.
                    TextButton(
                        enabled = !enviando,

                        onClick = {
                            adjunto = null
                            errorAdjunto = null
                        }
                    ) {
                        Text("Continuar sin PDF")
                    }
                }
            }

            // =============================================
            // AUTORIZACIÓN CON FIRMA FÍSICA
            // =============================================

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Checkbox(
                    checked = requiereFirma,

                    onCheckedChange = {
                        requiereFirma = it
                    },

                    enabled = !enviando
                )

                Text(
                    "Es una autorización que requiere firma física del apoderado"
                )
            }

            // Anima el aviso cuando se solicita firma física.
            AnimatedVisibility(
                visible = requiereFirma,

                enter = fadeIn(tween(300)) +
                        expandVertically(tween(300)),

                exit = fadeOut(tween(250)) +
                        shrinkVertically(tween(250))
            ) {

                AvisoCard(
                    texto = "Rol solo informativo: la app notifica por correo y avisa que la autorización " +
                            "debe entregarse físicamente con firma manuscrita. No se capturan firmas en la app.",

                    icono = Icons.Filled.Warning
                )
            }

            // =============================================
            // BOTÓN ENVIAR
            // =============================================

            Button(
                enabled = !enviando,

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),

                onClick = {

                    intentoEnvio = true

                    // Valida los campos antes de confirmar.
                    val hayErrores =
                        viewModel.validarComunicado(
                            asunto,
                            cuerpo,
                            tipo,
                            alcance,
                            destino
                        ).hayErrores

                    if (hayErrores || errorAdjunto != null) {

                        // Regresa al inicio del formulario.
                        alcanceScope.launch {
                            scroll.animateScrollTo(0)
                        }

                    } else if (
                        viewModel.necesitaPermisoNotificaciones() &&
                        !permisoSolicitado
                    ) {

                        permisoSolicitado = true

                        solicitudPermiso.launch(
                            Notificador.PERMISO
                        )

                    } else {

                        confirmando = true
                    }
                }
            ) {

                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text("Enviar")
            }
        }
    }

    // =====================================================
    // CONFIRMACIÓN DEL ENVÍO
    // =====================================================

    val tipoElegido = tipo
    val alcanceElegido = alcance

    if (
        confirmando &&
        tipoElegido != null &&
        alcanceElegido != null
    ) {

        AlertDialog(
            onDismissRequest = {
                confirmando = false
            },

            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null
                )
            },

            title = {
                Text("¿Enviar comunicado?")
            },

            text = {

                // EV3: diferencia el modo local del remoto.
                val modo = if (modoLocal) {

                    "despacho simulado local, sin envío real"

                } else {

                    "despacho mediante servidor"
                }

                Text(
                    "Se enviará a $cantidadDestinatarios apoderado(s) ($modo). " +
                            "Una vez enviado, el comunicado queda registrado en el historial y no se puede editar."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        confirmando = false

                        // Entrega el formulario al ViewModel.
                        viewModel.enviarComunicado(
                            SolicitudComunicado(
                                asunto = asunto,
                                cuerpo = cuerpo,
                                tipo = tipoElegido,
                                alcance = alcanceElegido,
                                destino = destino,
                                adjunto = adjunto,
                                requiereFirmaFisica = requiereFirma
                            )
                        )
                    }
                ) {
                    Text("Enviar")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        confirmando = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // =====================================================
    // PROGRESO Y RESULTADO DEL ENVÍO
    // =====================================================

    when (val estado = estadoEnvio) {

        is EstadoEnvioUi.Enviando -> {

            // Anima el avance del procesamiento.
            val avance by animateFloatAsState(
                targetValue = if (estado.total == 0) {

                    0f

                } else {

                    (estado.procesados.toFloat() / estado.total)
                        .coerceIn(0f, 1f)
                },

                animationSpec = tween(250),
                label = "avanceEnvio"
            )

            // EV3: diferencia el despacho del registro final.
            val registrando =
                estado.total > 0 &&
                        estado.procesados >= estado.total

            AlertDialog(
                onDismissRequest = {},

                title = {

                    Text(
                        if (registrando) {

                            "Guardando comunicado…"

                        } else {

                            "Enviando comunicado…"
                        }
                    )
                },

                text = {

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        if (registrando) {

                            // Progreso indeterminado durante el guardado.
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                if (modoLocal) {

                                    "Guardando en la base local…"

                                } else {

                                    "Confirmando el registro…"
                                }
                            )

                        } else {

                            // Progreso según destinatarios procesados.
                            LinearProgressIndicator(
                                progress = { avance },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                "${estado.procesados} de ${estado.total} destinatario(s) procesados"
                            )
                        }
                    }
                },

                confirmButton = {}
            )
        }

        // =============================================
        // ENVÍO FINALIZADO
        // =============================================

        is EstadoEnvioUi.Listo -> {

            val resumen = estado.resumen

            AlertDialog(
                onDismissRequest = {},

                icon = {

                    Icon(
                        imageVector = if (resumen.fallidos == 0) {

                            Icons.Filled.CheckCircle

                        } else {

                            Icons.Filled.Warning
                        },

                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },

                title = {
                    Text("Comunicado enviado")
                },

                text = {

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        // Muestra los despachos correctos.
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Text(
                                "${resumen.despachados} despachado(s)"
                            )
                        }

                        // Muestra los destinatarios fallidos.
                        if (resumen.fallidos > 0) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    Icons.Filled.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )

                                Spacer(
                                    modifier = Modifier.width(8.dp)
                                )

                                Text(
                                    "${resumen.fallidos} fallido(s): revisa el detalle en el historial"
                                )
                            }
                        }

                        // Aclara que no se realizan envíos reales en modo local.
                        if (modoLocal) {

                            Text(
                                "Despacho simulado. El comunicado quedó registrado localmente.",

                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (resumen.requiereFirmaFisica) {

                            Text(
                                "Recuerda: la autorización debe entregarse físicamente con firma manuscrita.",

                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },

                confirmButton = {

                    TextButton(
                        onClick = {

                            viewModel.reiniciarEnvio()

                            navController.popBackStack()
                        }
                    ) {
                        Text("Aceptar")
                    }
                }
            )
        }

        // =============================================
        // CONTROL DE ERRORES
        // =============================================

        is EstadoEnvioUi.Error -> {

            AlertDialog(
                onDismissRequest = {
                    viewModel.reiniciarEnvio()
                },

                icon = {

                    Icon(
                        Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },

                title = {

                    Text(
                        if (estado.puedeReintentar) {

                            "No se pudo registrar"

                        } else {

                            "No se pudo enviar"
                        }
                    )
                },

                text = {
                    Text(estado.mensaje)
                },

                confirmButton = {

                    if (estado.puedeReintentar) {

                        // Reintenta guardar sin duplicar los despachos.
                        TextButton(
                            onClick = {
                                viewModel.reintentarRegistro()
                            }
                        ) {
                            Text("Reintentar")
                        }

                    } else {

                        TextButton(
                            onClick = {
                                viewModel.reiniciarEnvio()
                            }
                        ) {
                            Text("Aceptar")
                        }
                    }
                },

                dismissButton = if (estado.puedeReintentar) {

                    {

                        TextButton(
                            onClick = {
                                viewModel.reiniciarEnvio()
                            }
                        ) {
                            Text("Descartar")
                        }
                    }

                } else {

                    null
                }
            )
        }

        EstadoEnvioUi.Inactivo -> Unit
    }
}

// Convierte el tamaño del PDF a un formato fácil de leer.
private fun tamanoLegible(bytes: Long): String = when {

    bytes >= 1024 * 1024 ->
        "%.1f MB".format(bytes / (1024.0 * 1024.0))

    bytes >= 1024 ->
        "${bytes / 1024} KB"

    else ->
        "$bytes B"
}
