package com.mpaznotifica.app.nativo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mpaznotifica.app.MainActivity
import com.mpaznotifica.app.R
import com.mpaznotifica.app.model.ResumenEnvio

/**
 * RECURSO NATIVO: notificación local con el resultado del envío.
 *
 * EV3: permite acceder al detalle del comunicado
 * al presionar la notificación.
 *
 * Mantiene la validación de permisos de Android.
 * Si el usuario rechaza el permiso, la aplicación
 * continúa funcionando normalmente.
 */
class Notificador(private val context: Context) {

    // Constantes utilizadas para identificar el canal,
    // el permiso y el comunicado.
    companion object {
        const val CANAL_ID = "envios_mpaz"
        const val PERMISO = Manifest.permission.POST_NOTIFICATIONS
        const val ACCION_DETALLE = "com.mpaznotifica.app.ABRIR_DETALLE"
        const val EXTRA_COMUNICADO_ID = "comunicado_id"
    }

    // Comprueba si la versión de Android requiere permiso.
    fun requierePermiso(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    // Verifica si el usuario concedió el permiso.
    fun tienePermiso(): Boolean =
        !requierePermiso() ||
                ContextCompat.checkSelfPermission(
                    context,
                    PERMISO
                ) == PackageManager.PERMISSION_GRANTED

    // Crea el canal de notificaciones de la aplicación.
    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                "Estado de envíos",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            canal.description =
                "Resultado del despacho de comunicados"

            context.getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(canal)
        }
    }

    // Genera la notificación con el resultado del envío.
    fun notificarEnvio(
        comunicadoId: String,
        asunto: String,
        resumen: ResumenEnvio
    ) {

        // Si no tiene permiso, no muestra la notificación.
        if (!tienePermiso()) return

        crearCanal()

        // Construye el resumen de destinatarios
        // despachados, fallidos y firma física.
        val texto = buildString {
            append("${resumen.despachados} despachado(s)")

            if (resumen.fallidos > 0) {
                append(" · ${resumen.fallidos} fallido(s)")
            }

            if (resumen.requiereFirmaFisica) {
                append(" · entregar la firma en papel")
            }
        }

        // EV3: prepara el Intent que recibe MainActivity.
        val intencion = Intent(
            context,
            MainActivity::class.java
        ).apply {

            action = ACCION_DETALLE

            // Identifica de forma única cada comunicado.
            data = Uri.Builder()
                .scheme("mpaznotifica")
                .authority("comunicado")
                .appendPath(comunicadoId)
                .build()

            // Envía el ID del comunicado a MainActivity.
            putExtra(EXTRA_COMUNICADO_ID, comunicadoId)

            // Evita abrir actividades duplicadas.
            addFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }

        // Permite ejecutar el Intent al tocar la notificación.
        val pendiente = PendingIntent.getActivity(
            context,
            comunicadoId.hashCode(),
            intencion,
            PendingIntent.FLAG_IMMUTABLE or
                    PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Configura el icono, título, contenido y acción.
        val notificacion =
            NotificationCompat.Builder(context, CANAL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Comunicado enviado: $asunto")
                .setContentText(texto)
                .setContentIntent(pendiente)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

        // Muestra una notificación identificada
        // por el ID del comunicado.
        try {
            NotificationManagerCompat.from(context).notify(
                comunicadoId,
                0,
                notificacion
            )
        } catch (e: SecurityException) {
            // Evita errores si el usuario revoca el permiso.
        }
    }
}
