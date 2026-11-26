package com.mpaznotifica.app.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mpaznotifica.app.ui.components.FranjaModo
import com.mpaznotifica.app.ui.screen.LoginScreen
import com.mpaznotifica.app.ui.screen.PanelScreen
import com.mpaznotifica.app.ui.screen.apoderados.ApoderadosScreen
import com.mpaznotifica.app.ui.screen.comunicados.DetalleComunicadoScreen
import com.mpaznotifica.app.ui.screen.comunicados.HistorialScreen
import com.mpaznotifica.app.ui.screen.comunicados.RedactarComunicadoScreen
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel

// Define las rutas disponibles en la aplicación.
object Rutas {

    const val LOGIN = "login"
    const val PANEL = "panel"
    const val REDACTAR = "redactar"
    const val HISTORIAL = "historial"
    const val DETALLE = "detalle/{id}"
    const val APODERADOS = "apoderados"

    const val ARG_ID = "id"

    // Genera la ruta del detalle utilizando el ID del comunicado.
    fun detalle(id: String) = "detalle/${Uri.encode(id)}"
}

/**
 * Controla la navegación entre las pantallas.
 *
 * Utiliza un NavHost y un ViewModel compartido.
 *
 * EV3:
 * - Mantiene las animaciones entre pantallas.
 * - Protege las rutas mediante la sesión del usuario.
 * - Permite abrir el detalle desde una notificación.
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: MpazNotificaViewModel = viewModel(),

    // EV3: recibe el ID enviado desde MainActivity.
    comunicadoPendiente: String? = null,

    // Limpia la notificación después de atenderla.
    onNotificacionAtendida: () -> Unit = {}
) {

    // Obtiene el usuario que tiene una sesión activa.
    val usuario by viewModel.usuarioActivo.collectAsState()

    // Observa los comunicados almacenados.
    val comunicados by viewModel.comunicados.collectAsState()

    // Obtiene la pantalla actual de la navegación.
    val rutaActual =
        navController.currentBackStackEntryAsState()
            .value?.destination?.route

    // =====================================================
    // CONTROL DE SESIÓN
    // =====================================================

    // Protege las pantallas cuando no existe una sesión activa.
    LaunchedEffect(usuario, rutaActual) {

        if (
            usuario == null &&
            rutaActual != null &&
            rutaActual != Rutas.LOGIN
        ) {

            navController.navigate(Rutas.LOGIN) {

                // Limpia la pila para impedir volver sin sesión.
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
            }
        }
    }

    // =====================================================
    // EV3: NAVEGACIÓN DESDE NOTIFICACIONES
    // =====================================================

    LaunchedEffect(
        usuario,
        comunicadoPendiente,
        rutaActual,
        comunicados
    ) {

        val id = comunicadoPendiente

        // Espera que exista una sesión y que el comunicado
        // esté disponible en los datos de la aplicación.
        if (
            usuario != null &&
            id != null &&
            rutaActual != null &&
            rutaActual != Rutas.LOGIN &&
            comunicados.any { it.id == id }
        ) {

            // Abre el detalle del comunicado correspondiente.
            navController.navigate(Rutas.detalle(id)) {

                // Mantiene el panel como pantalla de regreso.
                popUpTo(Rutas.PANEL) {
                    inclusive = false
                }

                // Evita duplicar el detalle en la navegación.
                launchSingleTop = true
            }

            // Limpia el ID para evitar repetir la navegación.
            onNotificacionAtendida()
        }
    }

    // =====================================================
    // ESTRUCTURA PRINCIPAL
    // =====================================================

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // Muestra si la aplicación funciona en modo local
        // o conectada a un servidor.
        FranjaModo(viewModel)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .consumeWindowInsets(WindowInsets.statusBars)
        ) {

            NavHost(
                navController = navController,
                startDestination = Rutas.LOGIN,

                // Animación de entrada entre pantallas.
                enterTransition = {
                    slideInHorizontally(
                        animationSpec = tween(280)
                    ) { ancho ->
                        ancho / 5
                    } + fadeIn(tween(280))
                },

                // Animación de salida.
                exitTransition = {
                    fadeOut(tween(200))
                },

                // Animación al regresar a una pantalla.
                popEnterTransition = {
                    fadeIn(tween(250))
                },

                // Animación de salida al presionar atrás.
                popExitTransition = {
                    slideOutHorizontally(
                        animationSpec = tween(250)
                    ) { ancho ->
                        ancho / 5
                    } + fadeOut(tween(250))
                }

            ) {

                // =========================================
                // LOGIN
                // =========================================

                composable(Rutas.LOGIN) {

                    LoginScreen(
                        viewModel = viewModel,

                        onIngresar = {

                            // Ingresa al panel después del login.
                            navController.navigate(Rutas.PANEL) {

                                // Elimina el login de la pila.
                                popUpTo(Rutas.LOGIN) {
                                    inclusive = true
                                }
                            }
                        }
                    )
                }

                // =========================================
                // PANEL PRINCIPAL
                // =========================================

                composable(Rutas.PANEL) {

                    PanelScreen(
                        navController,
                        viewModel,

                        onCerrarSesion = {

                            // Cierra la sesión del usuario.
                            viewModel.logout()
                        }
                    )
                }

                // =========================================
                // REDACTAR COMUNICADO
                // =========================================

                composable(Rutas.REDACTAR) {

                    RedactarComunicadoScreen(
                        navController,
                        viewModel
                    )
                }

                // =========================================
                // HISTORIAL
                // =========================================

                composable(Rutas.HISTORIAL) {

                    HistorialScreen(
                        navController,
                        viewModel
                    )
                }

                // =========================================
                // DETALLE DEL COMUNICADO
                // =========================================

                composable(
                    route = Rutas.DETALLE,

                    // Recibe el identificador del comunicado.
                    arguments = listOf(
                        navArgument(Rutas.ARG_ID) {
                            type = NavType.StringType
                        }
                    )

                ) { entrada ->

                    DetalleComunicadoScreen(
                        navController = navController,
                        viewModel = viewModel,

                        // Obtiene el ID desde la ruta.
                        comunicadoId = entrada.arguments
                            ?.getString(Rutas.ARG_ID)
                    )
                }

                // =========================================
                // APODERADOS Y CANAL PREFERENTE
                // =========================================

                composable(Rutas.APODERADOS) {

                    ApoderadosScreen(
                        navController,
                        viewModel
                    )
                }
            }
        }
    }
}
