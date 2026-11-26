package com.mpaznotifica.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mpaznotifica.app.nativo.Notificador
import com.mpaznotifica.app.navigation.AppNavigation
import com.mpaznotifica.app.ui.theme.MpazNotificaTheme

/**
 * Actividad principal de MPAZ Notifica.
 *
 * Inicia la interfaz y controla el acceso a la navegación.
 *
 * EV3: recibe el ID del comunicado al presionar
 * una notificación y permite abrir su detalle.
 */
class MainActivity : ComponentActivity() {

    // Almacena el ID del comunicado pendiente de abrir.
    private var comunicadoPendiente by mutableStateOf<String?>(null)

    // Se ejecuta cuando se inicia la actividad.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recupera el ID guardado o recibido por la notificación.
        comunicadoPendiente = if (savedInstanceState != null) {
            savedInstanceState.getString("comunicado_pendiente")
        } else {
            obtenerComunicado(intent)
        }

        // Permite utilizar toda la superficie de la pantalla.
        enableEdgeToEdge()

        // Inicia la interfaz mediante Jetpack Compose.
        setContent {
            MpazNotificaTheme {

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {

                    // Entrega el comunicado pendiente a la navegación.
                    AppNavigation(
                        comunicadoPendiente = comunicadoPendiente,

                        // Limpia el ID después de abrir el detalle.
                        onNotificacionAtendida = {
                            comunicadoPendiente = null
                        }
                    )
                }
            }
        }
    }

    // EV3: recibe una notificación cuando la actividad ya existe.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Actualiza el Intent de la actividad.
        setIntent(intent)

        // Obtiene el ID del nuevo comunicado.
        comunicadoPendiente = obtenerComunicado(intent)
    }

    // Conserva el ID pendiente si Android recrea la actividad.
    override fun onSaveInstanceState(outState: Bundle) {

        outState.putString(
            "comunicado_pendiente",
            comunicadoPendiente
        )

        super.onSaveInstanceState(outState)
    }

    // Extrae el identificador enviado desde Notificador.kt.
    private fun obtenerComunicado(intent: Intent?): String? {

        // Comprueba que el Intent corresponda a una notificación.
        if (intent?.action != Notificador.ACCION_DETALLE) {
            return null
        }

        // Devuelve el ID únicamente si contiene información.
        return intent.getStringExtra(
            Notificador.EXTRA_COMUNICADO_ID
        )?.takeIf {
            it.isNotBlank()
        }
    }
}
