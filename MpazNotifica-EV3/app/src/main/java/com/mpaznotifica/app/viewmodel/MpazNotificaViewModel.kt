package com.mpaznotifica.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mpaznotifica.app.BuildConfig
import com.mpaznotifica.app.data.local.AppDatabase
import com.mpaznotifica.app.data.remote.ApiClient
import com.mpaznotifica.app.data.remote.ApiResult
import com.mpaznotifica.app.data.remote.ConfiguracionServidor
import com.mpaznotifica.app.data.remote.CausaError
import com.mpaznotifica.app.data.remote.Despachador
import com.mpaznotifica.app.data.remote.FuenteRemota
import com.mpaznotifica.app.data.remote.PreferenciasUrl
import com.mpaznotifica.app.data.remote.SesionApi
import com.mpaznotifica.app.data.remote.UrlServidor
import com.mpaznotifica.app.data.repository.FuenteLocal
import com.mpaznotifica.app.data.repository.FuenteServidor
import com.mpaznotifica.app.data.repository.MpazRepository
import com.mpaznotifica.app.data.repository.PreferenciasModo
import com.mpaznotifica.app.data.repository.RoomCache
import com.mpaznotifica.app.data.repository.SelectorFuente
import com.mpaznotifica.app.domain.Credenciales
import com.mpaznotifica.app.domain.DatosReferencia
import com.mpaznotifica.app.domain.Fechas
import com.mpaznotifica.app.domain.FiltrosHistorial
import com.mpaznotifica.app.domain.ReglasComunicado
import com.mpaznotifica.app.domain.ReglasHistorial
import com.mpaznotifica.app.domain.ValidadorContacto
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.Destinatario
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.ErroresComunicado
import com.mpaznotifica.app.model.ErroresLogin
import com.mpaznotifica.app.model.EstadoDatos
import com.mpaznotifica.app.model.EstadoEnvioUi
import com.mpaznotifica.app.model.PruebaServidor
import com.mpaznotifica.app.model.ResumenEnvio
import com.mpaznotifica.app.model.SolicitudComunicado
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario
import com.mpaznotifica.app.nativo.ArchivosPdf
import com.mpaznotifica.app.nativo.EstadoRed
import com.mpaznotifica.app.nativo.Notificador
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel único, compartido por todas las pantallas.
 *
 * Arquitectura MVVM:
 * - Las pantallas muestran los datos.
 * - El ViewModel administra el estado y las operaciones.
 * - El dominio contiene las reglas de negocio.
 * - El repositorio administra la persistencia.
 *
 * Modo local: Room y despacho simulado.
 * Modo servidor: API REST y caché Room.
 *
 * EV3: se incorpora el ID del comunicado a la notificación
 * para permitir el acceso directo a su detalle.
 */
class MpazNotificaViewModel(
    application: Application
) : AndroidViewModel(application) {

    // =====================================================
    // CONFIGURACIÓN Y REPOSITORIO
    // =====================================================

    // Configuración de los componentes del modo servidor.
    private val configuracion = ConfiguracionServidor(
        PreferenciasUrl(application),
        BuildConfig.API_BASE_URL
    )

    private val sesionApi = SesionApi()

    private val cliente = ApiClient(
        configuracion,
        sesionApi
    )

    // Guarda la preferencia de funcionamiento local o remoto.
    private val preferenciaModo = PreferenciasModo(
        application,
        inicialLocal = !BuildConfig.USAR_API
    )

    /**
     * Selecciona la fuente de datos según el modo activo.
     *
     * Modo local: mpaz_local.db.
     * Modo servidor: mpaz_notifica.db como caché.
     *
     * Las bases son independientes.
     */
    private val repositorio = SelectorFuente(
        preferencia = preferenciaModo,
        local = FuenteLocal(
            RoomCache(
                AppDatabase.obtenerLocal(application)
            )
        ),
        servidor = FuenteServidor(
            MpazRepository(
                AppDatabase.obtener(application),
                FuenteRemota { cliente.api() }
            )
        ) { cliente.mensajeria() }
    )

    // Recurso nativo encargado de las notificaciones.
    private val notificador = Notificador(application)

    // Carga los datos iniciales al crear el ViewModel.
    init {
        viewModelScope.launch {
            repositorio.asegurarDatosIniciales()
        }
    }

    // Genera un mensaje según el estado de la conexión.
    private fun textoSinConexion(
        resultado: ApiResult.SinConexion
    ): String =
        if (!EstadoRed.disponible(getApplication())) {
            "El dispositivo no tiene conexión a internet."
        } else {
            resultado.mensaje
        }

    // Convierte un Flow en un StateFlow observable por Compose.
    private fun <T> Flow<T>.comoEstado(
        inicial: T
    ): StateFlow<T> =
        stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            inicial
        )

    // =====================================================
    // DATOS DE REFERENCIA Y BITÁCORA
    // =====================================================

    // Usuarios disponibles para filtrar por emisor.
    val emisores: StateFlow<List<Usuario>> =
        repositorio.usuarios.comoEstado(emptyList())

    // Combina los datos necesarios para aplicar las reglas.
    val datos: StateFlow<DatosReferencia> =
        combine(
            repositorio.talleres,
            repositorio.estudiantes,
            repositorio.apoderados
        ) { talleres, estudiantes, apoderados ->

            DatosReferencia(
                cursos = estudiantes
                    .map { it.curso }
                    .distinct()
                    .sorted(),
                talleres = talleres,
                estudiantes = estudiantes,
                apoderados = apoderados
            )

        }.comoEstado(DatosReferencia())

    // Lista observable de comunicados almacenados.
    val comunicados: StateFlow<List<Comunicacion>> =
        repositorio.comunicaciones.comoEstado(emptyList())

    // Lista observable del resultado de cada despacho.
    val envios: StateFlow<List<EnvioDestinatario>> =
        repositorio.envios.comoEstado(emptyList())

    // Agrupa los resultados de envío por comunicado.
    val resumenes: StateFlow<Map<String, ResumenEnvio>> =
        combine(comunicados, envios) { lista, todos ->

            val porComunicado =
                todos.groupBy { it.comunicacionId }

            lista.associate {
                it.id to ResumenEnvio.de(
                    porComunicado[it.id].orEmpty(),
                    it.requiereFirmaFisica
                )
            }

        }.comoEstado(emptyMap())

    // Mensajes breves que puede mostrar la interfaz.
    private val _mensajes =
        MutableSharedFlow<String>(extraBufferCapacity = 4)

    val mensajes: SharedFlow<String> =
        _mensajes.asSharedFlow()

    // =====================================================
    // MODO LOCAL Y MODO SERVIDOR
    // =====================================================

    // Indica si la aplicación funciona sin servidor.
    val modoLocal: StateFlow<Boolean> =
        preferenciaModo.modoLocal

    /**
     * Cambia la fuente de datos utilizada por la aplicación.
     * Si existe una sesión abierta, se cierra.
     */
    fun cambiarModo(local: Boolean) {

        if (preferenciaModo.esLocal() == local) return

        // Evita cambiar de modo durante un envío.
        if (_envioUi.value is EstadoEnvioUi.Enviando) {
            _mensajes.tryEmit(
                "Espera a que termine el envío para cambiar de modo"
            )
            return
        }

        if (_usuarioActivo.value != null) {
            logout()
        }

        preferenciaModo.cambiar(local)

        if (!local) {
            _urlServidor.value = direccionActual()
        }

        _estadoDatos.value = EstadoDatos.Inactivo
        _sinSesionRemota.value = false
        _pruebaServidor.value = PruebaServidor.Inactiva

        viewModelScope.launch {
            repositorio.asegurarDatosIniciales()
        }
    }

    // Restablece los datos de ejemplo únicamente en modo local.
    fun restablecerDatosLocales(
        alTerminar: (Boolean) -> Unit = {}
    ) {

        if (!repositorio.esLocal) {
            alTerminar(false)
            return
        }

        viewModelScope.launch {

            val exito = try {

                repositorio.restablecer() is ApiResult.Success

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                false
            }

            _mensajes.tryEmit(
                if (exito) {
                    "Datos locales restablecidos"
                } else {
                    "No se pudieron restablecer los datos locales"
                }
            )

            alTerminar(exito)
        }
    }

    // =====================================================
    // CONFIGURACIÓN DEL SERVIDOR
    // =====================================================

    // Obtiene la dirección configurada de la API.
    private fun direccionActual(): String =
        try {
            configuracion.urlBase()
        } catch (e: NotImplementedError) {
            BuildConfig.API_BASE_URL
        }

    private val _urlServidor = MutableStateFlow(
        if (preferenciaModo.esLocal()) {
            BuildConfig.API_BASE_URL
        } else {
            direccionActual()
        }
    )

    val urlServidor: StateFlow<String> =
        _urlServidor.asStateFlow()

    private val _pruebaServidor =
        MutableStateFlow<PruebaServidor>(
            PruebaServidor.Inactiva
        )

    val pruebaServidor: StateFlow<PruebaServidor> =
        _pruebaServidor.asStateFlow()

    // Comprueba que la dirección del servidor sea válida.
    fun validarServidor(texto: String): String? =
        UrlServidor.validar(texto)

    // Guarda la dirección del servidor si es válida.
    fun guardarServidor(texto: String): String? {

        val error = configuracion.guardar(texto)

        if (error == null) {
            _urlServidor.value = configuracion.urlBase()
            _pruebaServidor.value = PruebaServidor.Inactiva
        }

        return error
    }

    // Recupera la dirección original del servidor.
    fun restaurarServidor() {
        configuracion.restaurar()
        _urlServidor.value = configuracion.urlBase()
        _pruebaServidor.value = PruebaServidor.Inactiva
    }

    // Restablece el estado de la prueba de conexión.
    fun limpiarPruebaServidor() {
        _pruebaServidor.value = PruebaServidor.Inactiva
    }

    // Comprueba la disponibilidad del servidor.
    fun probarServidor(texto: String) {

        if (_pruebaServidor.value is PruebaServidor.Probando) {
            return
        }

        _pruebaServidor.value = PruebaServidor.Probando

        viewModelScope.launch {

            _pruebaServidor.value =
                when (val resultado = cliente.probarConexion(texto)) {

                    is ApiResult.Success ->
                        PruebaServidor.Correcta(
                            "Conexión correcta con el servidor de MPAZ Notifica"
                        )

                    is ApiResult.SinConexion ->
                        PruebaServidor.Fallida(
                            textoSinConexion(resultado)
                        )

                    is ApiResult.Error ->
                        PruebaServidor.Fallida(
                            resultado.mensaje
                        )
                }
        }
    }

    // =====================================================
    // SINCRONIZACIÓN DE DATOS
    // =====================================================

    private val _estadoDatos =
        MutableStateFlow<EstadoDatos>(EstadoDatos.Inactivo)

    val estadoDatos: StateFlow<EstadoDatos> =
        _estadoDatos.asStateFlow()

    // Indica si el usuario está en modo remoto sin conexión.
    private val _sinSesionRemota =
        MutableStateFlow(false)

    val sinSesionRemota: StateFlow<Boolean> =
        _sinSesionRemota.asStateFlow()

    private var sincronizando = false

    // Sincroniza los datos remotos con la caché Room.
    fun sincronizar() {

        if (sincronizando || _usuarioActivo.value == null) {
            return
        }

        if (repositorio.esLocal) {
            _estadoDatos.value = EstadoDatos.Inactivo
            return
        }

        if (!sesionApi.activa) {
            _estadoDatos.value =
                EstadoDatos.SinConexion(MSG_MODO_SIN_CONEXION)

            return
        }

        sincronizando = true
        _estadoDatos.value = EstadoDatos.Cargando

        viewModelScope.launch {

            try {

                when (val resultado = repositorio.sincronizar()) {

                    is ApiResult.Success -> {

                        _estadoDatos.value =
                            EstadoDatos.Actualizado(
                                Fechas.legible(Fechas.ahora())
                            )
                    }

                    is ApiResult.SinConexion -> {

                        _estadoDatos.value =
                            EstadoDatos.SinConexion(
                                textoSinConexion(resultado) +
                                        " Se muestran los últimos datos guardados."
                            )
                    }

                    is ApiResult.Error -> {

                        if (resultado.causa == CausaError.NO_AUTORIZADO) {
                            sesionExpirada()
                        } else {
                            _estadoDatos.value =
                                EstadoDatos.Error(resultado.mensaje)
                        }
                    }
                }

            } finally {

                sincronizando = false
            }
        }
    }

    // Cierra la sesión cuando el servidor rechaza el token.
    private fun sesionExpirada() {

        logout()

        _errorCredenciales.value =
            "Tu sesión expiró. Ingresa nuevamente."
    }

    // =====================================================
    // AUTENTICACIÓN Y PERFILES (RF-01)
    // =====================================================

    // Mantiene el usuario conectado a la aplicación.
    private val _usuarioActivo =
        MutableStateFlow<Usuario?>(null)

    val usuarioActivo: StateFlow<Usuario?> =
        _usuarioActivo.asStateFlow()

    // Errores individuales del formulario de login.
    private val _erroresLogin =
        MutableStateFlow(ErroresLogin())

    val erroresLogin: StateFlow<ErroresLogin> =
        _erroresLogin.asStateFlow()

    // Error general de credenciales incorrectas.
    private val _errorCredenciales =
        MutableStateFlow<String?>(null)

    val errorCredenciales: StateFlow<String?> =
        _errorCredenciales.asStateFlow()

    // Controla el indicador de progreso del login.
    private val _ingresando =
        MutableStateFlow(false)

    val ingresando: StateFlow<Boolean> =
        _ingresando.asStateFlow()

    // Limpia los errores del formulario.
    fun limpiarErroresLogin() {

        _erroresLogin.value = ErroresLogin()

        _errorCredenciales.value = null
    }

    /**
     * Valida los campos y comprueba las credenciales.
     *
     * Modo local: verifica los usuarios almacenados en Room.
     * Modo servidor: utiliza la autenticación remota.
     */
    fun iniciarSesion(
        correo: String,
        clave: String,
        onExito: () -> Unit
    ) {

        if (_ingresando.value) return

        // Valida los campos mediante las reglas del dominio.
        val errores = Credenciales.validar(correo, clave)

        _erroresLogin.value = errores
        _errorCredenciales.value = null

        if (errores.hayErrores) return

        _ingresando.value = true

        viewModelScope.launch {

            try {

                repositorio.asegurarDatosIniciales()

                val local = repositorio.esLocal

                when (
                    val resultado =
                        repositorio.iniciarSesion(correo, clave)
                ) {

                    is ApiResult.Success -> {

                        if (local) {
                            sesionApi.cerrar()
                        } else {
                            sesionApi.iniciar(
                                resultado.datos.token
                            )
                        }

                        _sinSesionRemota.value = false
                        _estadoDatos.value = EstadoDatos.Inactivo

                        // Guarda el usuario autenticado.
                        _usuarioActivo.value =
                            resultado.datos.usuario

                        // Permite navegar al panel principal.
                        onExito()

                        sincronizar()
                    }

                    is ApiResult.Error -> {

                        _errorCredenciales.value =
                            if (
                                resultado.causa ==
                                CausaError.NO_AUTORIZADO
                            ) {
                                "Correo o clave incorrectos"
                            } else {
                                resultado.mensaje
                            }
                    }

                    is ApiResult.SinConexion -> {

                        ingresarSinConexion(
                            correo,
                            clave,
                            textoSinConexion(resultado),
                            onExito
                        )
                    }
                }

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _errorCredenciales.value =
                    if (repositorio.esLocal) {
                        "No se pudo acceder a los datos locales. Intenta nuevamente."
                    } else {
                        "No se pudo iniciar sesión. Intenta nuevamente."
                    }

            } finally {

                _ingresando.value = false
            }
        }
    }

    // Permite consultar los datos guardados sin conexión.
    private suspend fun ingresarSinConexion(
        correo: String,
        clave: String,
        motivo: String,
        onExito: () -> Unit
    ) {

        val encontrado = repositorio.buscarUsuario(correo)

        if (
            encontrado != null &&
            encontrado.claveHash.isNotEmpty() &&
            Credenciales.coincide(
                clave,
                encontrado.claveHash
            )
        ) {

            sesionApi.cerrar()

            _sinSesionRemota.value = true

            _estadoDatos.value =
                EstadoDatos.SinConexion(MSG_MODO_SIN_CONEXION)

            _usuarioActivo.value = encontrado

            onExito()

        } else {

            _errorCredenciales.value = motivo
        }
    }

    // Cierra la sesión y limpia los estados del usuario.
    fun logout() {

        val token = sesionApi.token
        val fuente = repositorio.actual()

        sesionApi.cerrar()

        if (token != null) {

            viewModelScope.launch {
                fuente.cerrarSesion(token)
            }
        }

        _sinSesionRemota.value = false
        _estadoDatos.value = EstadoDatos.Inactivo

        pendienteRegistro = null

        _usuarioActivo.value = null

        limpiarErroresLogin()

        _envioUi.value = EstadoEnvioUi.Inactivo

        _filtros.value = FiltrosHistorial()
    }

    // =====================================================
    // REDACCIÓN Y ENVÍO DE COMUNICADOS
    // RF-02 a RF-08, RF-12, RN-01 y RN-02
    // =====================================================

    // Obtiene los tipos de comunicado permitidos por perfil.
    fun tiposPermitidos(
        usuario: Usuario?
    ): List<TipoComunicacion> =
        ReglasComunicado.tiposPermitidos(usuario)

    // Obtiene los alcances permitidos por perfil.
    fun alcancesPermitidos(
        usuario: Usuario?
    ): List<AlcanceEnvio> =
        ReglasComunicado.alcancesPermitidos(usuario)

    // RF-06: limita los destinos según el usuario y su curso.
    fun destinosPara(
        alcance: AlcanceEnvio?,
        usuario: Usuario?
    ): List<String> =
        ReglasComunicado.destinosPara(
            alcance,
            usuario,
            datos.value
        )

    // Valida el formulario mediante las reglas del dominio.
    fun validarComunicado(
        asunto: String,
        cuerpo: String,
        tipo: TipoComunicacion?,
        alcance: AlcanceEnvio?,
        destino: String?
    ): ErroresComunicado =
        ReglasComunicado.validar(
            asunto,
            cuerpo,
            tipo,
            alcance,
            destino,
            _usuarioActivo.value,
            datos.value
        )

    // Calcula la cantidad de apoderados destinatarios.
    fun contarDestinatarios(
        alcance: AlcanceEnvio?,
        destino: String?
    ): Int =
        if (alcance == null) {
            0
        } else {
            ReglasComunicado.resolverApoderados(
                alcance,
                destino,
                datos.value
            ).size
        }

    // Comprueba si falta solicitar el permiso de notificaciones.
    fun necesitaPermisoNotificaciones(): Boolean =
        notificador.requierePermiso() &&
                !notificador.tienePermiso()

    // Estado observable del proceso de envío.
    private val _envioUi =
        MutableStateFlow<EstadoEnvioUi>(
            EstadoEnvioUi.Inactivo
        )

    val envioUi: StateFlow<EstadoEnvioUi> =
        _envioUi.asStateFlow()

    // Restablece el formulario después de finalizar un envío.
    fun reiniciarEnvio() {

        if (_envioUi.value !is EstadoEnvioUi.Enviando) {

            pendienteRegistro = null

            _envioUi.value = EstadoEnvioUi.Inactivo
        }
    }

    // Guarda temporalmente un despacho pendiente de registrar.
    private class PendienteRegistro(
        val comunicacion: Comunicacion,
        val destino: String?,
        val envios: List<EnvioDestinatario>
    )

    private var pendienteRegistro: PendienteRegistro? = null

    /**
     * Valida, prepara y envía un comunicado.
     *
     * Aplica una segunda validación antes del envío,
     * evitando depender únicamente de la interfaz.
     *
     * En modo local utiliza Room y despacho simulado.
     * Los comunicados registrados son inmutables.
     */
    fun enviarComunicado(
        solicitud: SolicitudComunicado
    ) {

        // Evita realizar dos envíos simultáneos.
        if (_envioUi.value is EstadoEnvioUi.Enviando) {
            return
        }

        val autor = _usuarioActivo.value

        // Impide enviar comunicados sin iniciar sesión.
        if (autor == null) {

            _envioUi.value =
                EstadoEnvioUi.Error(
                    ReglasComunicado.MSG_SIN_SESION
                )

            return
        }

        // En modo remoto exige una sesión válida.
        if (!repositorio.esLocal && !sesionApi.activa) {

            _envioUi.value =
                EstadoEnvioUi.Error(
                    MSG_REQUIERE_CONEXION
                )

            return
        }

        val datosActuales = datos.value

        // Segunda barrera de validación del RF-06.
        val errores = ReglasComunicado.validar(
            solicitud.asunto,
            solicitud.cuerpo,
            solicitud.tipo,
            solicitud.alcance,
            solicitud.destino,
            autor,
            datosActuales
        )

        if (errores.hayErrores) {

            _envioUi.value =
                EstadoEnvioUi.Error(
                    "El comunicado no cumple las reglas de tu perfil. Revisa el formulario."
                )

            return
        }

        // Obtiene los destinatarios y sus canales de envío.
        val destinatarios = ReglasComunicado
            .resolverApoderados(
                solicitud.alcance,
                solicitud.destino,
                datosActuales
            )
            .map {
                Destinatario(
                    it,
                    ReglasComunicado.canalDe(
                        it,
                        solicitud.requiereFirmaFisica
                    )
                )
            }

        if (destinatarios.isEmpty()) {

            _envioUi.value =
                EstadoEnvioUi.Error(
                    ReglasComunicado.MSG_SIN_DESTINATARIOS
                )

            return
        }

        // Activa el indicador de progreso.
        _envioUi.value =
            EstadoEnvioUi.Enviando(
                0,
                destinatarios.size
            )

        viewModelScope.launch {

            try {

                // Genera un identificador único para el comunicado.
                val comunicacionId =
                    UUID.randomUUID().toString()

                // Guarda una copia local del PDF adjunto.
                val nombreAdjunto = solicitud.adjunto?.let {

                    ArchivosPdf.guardarCopia(
                        getApplication<Application>(),
                        it,
                        comunicacionId
                    ) ?: throw IllegalStateException(
                        "No se pudo leer el PDF adjunto"
                    )
                }

                // Construye el comunicado que será registrado.
                val comunicacion = Comunicacion(
                    id = comunicacionId,
                    asunto = solicitud.asunto.trim(),
                    cuerpo = ReglasComunicado.cuerpoFinal(
                        solicitud.cuerpo,
                        solicitud.requiereFirmaFisica
                    ),
                    tipo = solicitud.tipo,
                    alcance = solicitud.alcance,
                    destinoReferencia =
                        if (
                            solicitud.alcance ==
                            AlcanceEnvio.COLEGIO_COMPLETO
                        ) {
                            null
                        } else {
                            solicitud.destino
                        },
                    adjuntoPdfNombre = nombreAdjunto,
                    requiereFirmaFisica =
                        solicitud.requiereFirmaFisica,
                    autorId = autor.id,
                    autorNombre = autor.nombre,
                    fechaHora = Fechas.ahora()
                )

                // Despacha y actualiza el progreso por destinatario.
                val resultados = Despachador(
                    repositorio.mensajeria()
                ).despachar(
                    comunicacion,
                    destinatarios
                ) { procesados, total ->

                    _envioUi.value =
                        EstadoEnvioUi.Enviando(
                            procesados,
                            total
                        )
                }

                // Guarda el comunicado y los resultados del envío.
                registrar(
                    PendienteRegistro(
                        comunicacion,
                        solicitud.destino,
                        resultados
                    )
                )

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _envioUi.value =
                    EstadoEnvioUi.Error(
                        if (repositorio.esLocal) {
                            "No se pudo registrar el comunicado. Intenta nuevamente."
                        } else {
                            "No se pudo enviar el comunicado. Intenta nuevamente."
                        }
                    )
            }
        }
    }

    // Reintenta el registro sin volver a despachar los mensajes.
    fun reintentarRegistro() {

        val pendiente = pendienteRegistro ?: return

        if (_envioUi.value is EstadoEnvioUi.Enviando) {
            return
        }

        _envioUi.value =
            EstadoEnvioUi.Enviando(
                pendiente.envios.size,
                pendiente.envios.size
            )

        viewModelScope.launch {

            try {

                registrar(pendiente)

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _envioUi.value =
                    EstadoEnvioUi.Error(
                        "No se pudo registrar el comunicado. Intenta nuevamente.",
                        puedeReintentar = true
                    )
            }
        }
    }

    // =====================================================
    // REGISTRO Y NOTIFICACIÓN DEL COMUNICADO
    // =====================================================

    private suspend fun registrar(
        pendiente: PendienteRegistro
    ) {

        val comunicacion = pendiente.comunicacion

        // Registra el comunicado mediante el repositorio.
        when (
            val resultado = repositorio.registrarEnvio(
                comunicacion,
                pendiente.destino,
                pendiente.envios
            )
        ) {

            is ApiResult.Success -> {

                // El registro se completó correctamente.
                pendienteRegistro = null

                val registrada = resultado.datos

                // Informa a la interfaz que finalizó el envío.
                _envioUi.value =
                    EstadoEnvioUi.Listo(registrada.resumen)

                // EV3: entrega el ID del comunicado al notificador.
                // Permite abrir su detalle al tocar la notificación.
                notificador.notificarEnvio(
                    comunicadoId = registrada.comunicacion.id,
                    asunto = registrada.comunicacion.asunto,
                    resumen = registrada.resumen
                )
            }

            is ApiResult.SinConexion -> {

                // Conserva el despacho para reintentar su registro.
                pendienteRegistro = pendiente

                _envioUi.value =
                    EstadoEnvioUi.Error(
                        "Los mensajes se despacharon, pero no se pudo registrar el comunicado en el servidor: " +
                                textoSinConexion(resultado) +
                                " Pulsa Reintentar para registrarlo.",
                        puedeReintentar = true
                    )
            }

            is ApiResult.Error -> when {

                resultado.causa ==
                        CausaError.NO_AUTORIZADO -> {

                    pendienteRegistro = null

                    sesionExpirada()
                }

                // Evita duplicar un comunicado ya registrado.
                resultado.causa == CausaError.CONFLICTO &&
                        resultado.campo == "id" -> {

                    pendienteRegistro = null

                    val resumen = ResumenEnvio.de(
                        pendiente.envios,
                        comunicacion.requiereFirmaFisica
                    )

                    _envioUi.value =
                        EstadoEnvioUi.Listo(resumen)

                    sincronizar()
                }

                else -> {

                    pendienteRegistro = null

                    _envioUi.value =
                        EstadoEnvioUi.Error(
                            resultado.mensaje
                        )
                }
            }
        }
    }

    // =====================================================
    // HISTORIAL Y TRAZABILIDAD
    // RF-09, RF-10 y RF-11
    // =====================================================

    // Almacena los filtros seleccionados por el usuario.
    private val _filtros =
        MutableStateFlow(FiltrosHistorial())

    val filtros: StateFlow<FiltrosHistorial> =
        _filtros.asStateFlow()

    // Aplica los filtros mediante las reglas del dominio.
    // El ViewModel no contiene la lógica interna del filtrado.
    val historial: StateFlow<List<Comunicacion>> =
        combine(comunicados, _filtros) { lista, filtros ->

            ReglasHistorial.aplicar(
                lista,
                filtros
            )

        }.comoEstado(emptyList())

    // Valida el rango de fechas mediante ReglasHistorial.
    val errorFechas: StateFlow<String?> =
        _filtros.map {

            ReglasHistorial.errorFechas(
                it.desde,
                it.hasta
            )

        }.comoEstado(null)

    // Actualiza el filtro por tipo de comunicado.
    fun filtrarPorTipo(tipo: TipoComunicacion?) {

        _filtros.value =
            _filtros.value.copy(tipo = tipo)
    }

    // Actualiza el filtro por usuario emisor.
    fun filtrarPorEmisor(emisorId: String?) {

        _filtros.value =
            _filtros.value.copy(emisorId = emisorId)
    }

    // Actualiza la fecha inicial del historial.
    fun filtrarDesde(fecha: String?) {

        _filtros.value =
            _filtros.value.copy(desde = fecha)
    }

    // Actualiza la fecha final del historial.
    fun filtrarHasta(fecha: String?) {

        _filtros.value =
            _filtros.value.copy(hasta = fecha)
    }

    // Elimina todos los filtros aplicados.
    fun limpiarFiltros() {

        _filtros.value = FiltrosHistorial()
    }

    // Busca un comunicado mediante su identificador.
    // Se utiliza para mostrar el detalle correspondiente.
    fun comunicadoPorId(id: String?): Comunicacion? =

        comunicados.value.find { it.id == id }

    // =====================================================
    // CANAL PREFERENTE DEL APODERADO (RF-13)
    // =====================================================

    // Comprueba si el perfil puede modificar canales.
    fun puedeConfigurarCanal(
        usuario: Usuario?
    ): Boolean =

        ReglasComunicado.puedeConfigurarCanal(usuario)

    // Obtiene los apoderados visibles para cada perfil.
    fun apoderadosVisibles(
        usuario: Usuario?
    ): List<Apoderado> =

        ReglasComunicado.apoderadosVisibles(
            usuario,
            datos.value
        )

    /**
     * Actualiza el canal preferente de un apoderado.
     *
     * Solo Inspectoría puede realizar esta modificación.
     * Comprueba que exista un dato de contacto válido.
     * El cambio se conserva mediante el repositorio.
     */
    fun actualizarCanalPreferente(
        apoderadoId: String,
        canal: CanalDespacho
    ) {

        // Comprueba los permisos del usuario.
        if (!puedeConfigurarCanal(_usuarioActivo.value)) {

            _mensajes.tryEmit(
                "Solo Inspectoría General puede cambiar el canal preferente"
            )

            return
        }

        val apoderado =
            datos.value.apoderadoPorId(apoderadoId)

        if (apoderado == null) {

            _mensajes.tryEmit(
                "El apoderado no existe"
            )

            return
        }

        // Valida el dato de contacto requerido por el canal.
        if (!ValidadorContacto.valido(apoderado, canal)) {

            _mensajes.tryEmit(
                "${apoderado.nombreCompleto} no tiene un dato válido para ${canal.etiqueta}"
            )

            return
        }

        if (!repositorio.esLocal && !sesionApi.activa) {

            _mensajes.tryEmit(
                MSG_REQUIERE_CONEXION
            )

            return
        }

        val actor = _usuarioActivo.value

        viewModelScope.launch {

            try {

                // Guarda el nuevo canal mediante el repositorio.
                when (
                    val resultado =
                        repositorio.actualizarCanalPreferente(
                            actor,
                            apoderadoId,
                            canal
                        )
                ) {

                    is ApiResult.Success -> {

                        _mensajes.tryEmit(
                            "Canal preferente de ${apoderado.nombreCompleto}: ${canal.etiqueta}"
                        )
                    }

                    is ApiResult.SinConexion -> {

                        _mensajes.tryEmit(
                            textoSinConexion(resultado)
                        )
                    }

                    is ApiResult.Error -> {

                        if (
                            resultado.causa ==
                            CausaError.NO_AUTORIZADO
                        ) {

                            sesionExpirada()

                        } else {

                            _mensajes.tryEmit(
                                resultado.mensaje
                            )
                        }
                    }
                }

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _mensajes.tryEmit(
                    "No se pudo guardar el canal"
                )
            }
        }
    }

    // =====================================================
    // MENSAJES GENERALES
    // =====================================================

    private companion object {

        const val MSG_MODO_SIN_CONEXION =
            "Sin conexión con el servidor: se muestran los datos guardados en el dispositivo. " +
                    "Para enviar comunicados o actualizar, cierra sesión e ingresa nuevamente con conexión."

        const val MSG_REQUIERE_CONEXION =
            "Esta acción necesita conexión con el servidor. Cierra sesión e ingresa nuevamente cuando la tengas."
    }
}
