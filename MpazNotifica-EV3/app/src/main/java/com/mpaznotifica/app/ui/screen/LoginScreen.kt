package com.mpaznotifica.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mpaznotifica.app.R
import com.mpaznotifica.app.ui.components.AvisoCard
import com.mpaznotifica.app.ui.components.DialogoServidor
import com.mpaznotifica.app.ui.components.InterruptorModoLocal
import com.mpaznotifica.app.ui.components.apoyo
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel

/**
 * RF-01: autenticación diferenciada por perfil.
 *
 * Modo local: valida las credenciales almacenadas en Room.
 * Modo servidor: utiliza la autenticación remota.
 *
 * EV3: errores por campo, iconos y animaciones.
 */
@Composable
fun LoginScreen(
    viewModel: MpazNotificaViewModel,
    onIngresar: () -> Unit
) {

    // Estado de los campos del formulario.
    var usuario by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }

    // Controla la visibilidad de la contraseña.
    var claveVisible by remember { mutableStateOf(false) }

    // Controla la animación de entrada.
    var contenidoVisible by remember { mutableStateOf(false) }

    var mostrarServidor by rememberSaveable {
        mutableStateOf(false)
    }

    // Obtiene los estados del ViewModel.
    val errores by viewModel.erroresLogin.collectAsState()
    val errorCredenciales by viewModel.errorCredenciales.collectAsState()
    val ingresando by viewModel.ingresando.collectAsState()
    val urlServidor by viewModel.urlServidor.collectAsState()
    val modoLocal by viewModel.modoLocal.collectAsState()

    val foco = LocalFocusManager.current

    val errorUsuario = errores.usuario
    val errorClave = errores.clave

    // Activa la animación al mostrar la pantalla.
    LaunchedEffect(Unit) {
        contenidoVisible = true
    }

    // Envía las credenciales al ViewModel.
    fun ingresar() {
        foco.clearFocus()
        viewModel.iniciarSesion(
            usuario,
            clave,
            onIngresar
        )
    }

    // Cierra el diálogo del servidor al cambiar a modo local.
    LaunchedEffect(modoLocal) {
        if (modoLocal) {
            mostrarServidor = false
        }
    }

    if (!modoLocal && mostrarServidor) {

        DialogoServidor(
            viewModel = viewModel,
            onCerrar = {
                mostrarServidor = false
            }
        )
    }

    Scaffold { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Animación de entrada del formulario.
            AnimatedVisibility(
                visible = contenidoVisible,
                enter = fadeIn(
                    animationSpec = tween(500)
                ) + slideInVertically(
                    animationSpec = tween(500)
                ) { alto ->
                    alto / 6
                }
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Logotipo de la aplicación.
                    Image(
                        painter = painterResource(
                            id = R.drawable.logo_app
                        ),
                        contentDescription = "Logo de MPAZ Notifica",
                        modifier = Modifier.size(110.dp)
                    )

                    Text(
                        text = "MPAZ Notifica",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        text = "Ingreso de personal docente y directivo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Campo de correo institucional.
                    OutlinedTextField(
                        value = usuario,

                        onValueChange = {
                            usuario = it

                            if (
                                errorUsuario != null ||
                                errorCredenciales != null
                            ) {
                                viewModel.limpiarErroresLogin()
                            }
                        },

                        label = {
                            Text("Correo institucional")
                        },

                        leadingIcon = {
                            Icon(
                                Icons.Filled.Email,
                                contentDescription = null
                            )
                        },

                        // Muestra el icono cuando existe un error.
                        trailingIcon = if (errorUsuario != null) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = "Campo con error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            null
                        },

                        isError = errorUsuario != null,
                        supportingText = apoyo(errorUsuario),
                        singleLine = true,

                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),

                        keyboardActions = KeyboardActions(
                            onNext = {
                                foco.moveFocus(FocusDirection.Down)
                            }
                        ),

                        modifier = Modifier.fillMaxWidth()
                    )

                    // Campo de contraseña.
                    OutlinedTextField(
                        value = clave,

                        onValueChange = {
                            clave = it

                            if (
                                errorClave != null ||
                                errorCredenciales != null
                            ) {
                                viewModel.limpiarErroresLogin()
                            }
                        },

                        label = {
                            Text("Clave de acceso")
                        },

                        leadingIcon = {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = null
                            )
                        },

                        trailingIcon = {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                // EV3: muestra el error sin quitar el botón del ojo.
                                if (errorClave != null) {

                                    Icon(
                                        imageVector = Icons.Filled.ErrorOutline,
                                        contentDescription = "Error en la clave",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }

                                // Permite mostrar u ocultar la contraseña.
                                IconButton(
                                    onClick = {
                                        claveVisible = !claveVisible
                                    }
                                ) {

                                    Icon(
                                        imageVector = if (claveVisible) {
                                            Icons.Filled.VisibilityOff
                                        } else {
                                            Icons.Filled.Visibility
                                        },

                                        contentDescription = if (claveVisible) {
                                            "Ocultar clave"
                                        } else {
                                            "Mostrar clave"
                                        }
                                    )
                                }
                            }
                        },

                        // Destaca el campo y muestra su mensaje.
                        isError = errorClave != null,
                        supportingText = apoyo(errorClave),
                        singleLine = true,

                        visualTransformation = if (claveVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },

                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),

                        keyboardActions = KeyboardActions(
                            onDone = {
                                ingresar()
                            }
                        ),

                        modifier = Modifier.fillMaxWidth()
                    )

                    // Anima el mensaje de credenciales incorrectas.
                    AnimatedVisibility(
                        visible = errorCredenciales != null,
                        enter = fadeIn(tween(250)) +
                                expandVertically(tween(250)),
                        exit = fadeOut(tween(200)) +
                                shrinkVertically(tween(200))
                    ) {

                        AvisoCard(
                            texto = errorCredenciales.orEmpty(),
                            icono = Icons.Filled.ErrorOutline,
                            esError = true
                        )
                    }

                    // Impide realizar dos inicios de sesión simultáneos.
                    Button(
                        onClick = {
                            ingresar()
                        },
                        enabled = !ingresando,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        // Muestra un indicador mientras valida las credenciales.
                        if (ingresando) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )

                        } else {

                            Text("Ingresar")
                        }
                    }

                    // Configuración del servidor, solo en modo remoto.
                    if (!modoLocal) {

                        TextButton(
                            onClick = {
                                mostrarServidor = true
                            }
                        ) {

                            Icon(
                                Icons.Filled.Dns,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )

                            Text("  Servidor: $urlServidor")
                        }
                    }

                    // Permite seleccionar el modo de funcionamiento.
                    InterruptorModoLocal(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
