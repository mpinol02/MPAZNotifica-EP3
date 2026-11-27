# MPAZ Notifica

Aplicación móvil Android (Kotlin, Jetpack Compose, Material Design 3, MVVM, Room) para la Escuela Escritora Marcela Paz (San Bernardo): reemplaza el flujo analógico de comunicaciones escolares por emisión, segmentación y trazabilidad de comunicados hacia apoderados, con permisos de emisión diferenciados por perfil.

Es un MVP académico. El proyecto se entrega en **modo local** (Room + despacho simulado, sin servidor; se puede cambiar a modo servidor en tiempo de ejecución, ver "Modo local y modo servidor") y conserva el **andamiaje de integración con una API REST** (Retrofit). Los desarrollos pendientes de integración remota están marcados con `TODO(E3-n)`.

Durante la Evaluación Parcial 3 se fortalecieron las validaciones, las reglas de negocio, la persistencia local, los recursos nativos, las animaciones y las pruebas automatizadas mediante JUnit 5 y Kotest.

Se incorporó JaCoCo para medir la cobertura de código, obteniendo un **91,75 % de cobertura de instrucciones en la capa Repository**, además de 288 pruebas automatizadas aprobadas.

Finalmente, se generó un APK release firmado, verificado e instalado correctamente.

Toda la información es **100 % ficticia** (RNF-04). Los apoderados son destinatarios finales y no operan la app.

El backend de práctica (Node.js) se distribuyó en un paquete aparte (carpeta `backend/`, con su propio README, `docs/API.md` y una colección Postman).

La integración REST permanece pendiente y no forma parte del alcance local validado en esta entrega.

## Integrantes

- Axel Arana.
- Gonzalo Hernández.
- Gaspar Ríos.
- Mario Pino.

**Institución:** Duoc UC, sede Plaza Maipú.

**Carrera:** Ingeniería en Informática.

**Asignatura:** Desarrollo de Aplicaciones Móviles.

**Sección:** DSY1105-007.

**Evaluación:** Evaluación Parcial 3.

**Repositorio:** https://github.com/mpinol02/MPAZNotifica-EP3

## Perfiles y usuarios de prueba

| Perfil | Usuario | Clave | Puede emitir |
|---|---|---|---|
| Inspectoría General (emisor principal) | `inspectoria@mpaznotifica.cl` | `inspectoria123` | Todos los tipos y los cuatro alcances. Configura el canal preferente de los apoderados. |
| Profesor/a Jefe 4° Básico A | `profesor.4basicoA@mpaznotifica.cl` | `profesor123` | Tipos pedagógica, citación y salida pedagógica, solo hacia su curso o un apoderado de su curso. |
| Profesor/a Jefe 2° Básico A | `profesor.2basicoA@mpaznotifica.cl` | `profesor123` | Igual que el anterior, para 2° Básico A. |

Las claves no se almacenan en texto plano dentro de Room: se guarda el SHA-256 de `sal de demo + clave` (`domain/Credenciales.kt`). La sal es fija y solo sirve para fines didácticos.

En el contrato del modo API está prevista la validación del ingreso mediante `POST /auth/login`.

Los tres usuarios de demostración fueron probados correctamente en la versión debug y en el APK release firmado.

## Requerimientos

| Código | Requerimiento | Estado en la entrega | Dónde |
|---|---|---|---|
| RF-01 | Inicio de sesión diferenciado por perfil | Local: implementado y validado. API: TODO E3-8 | `ui/screen/LoginScreen.kt`, `MpazNotificaViewModel.iniciarSesion`, `domain/Credenciales.kt` |
| RF-02 | Opciones de emisión según perfil | Implementado y validado | `ReglasComunicado.tiposPermitidos` y `alcancesPermitidos` |
| RF-03 | Redactar con asunto, cuerpo, tipo y PDF opcional | Implementado. Incluye validación, persistencia y apertura segura del PDF | `RedactarComunicadoScreen.kt`, `nativo/ArchivosPdf.kt`, `ReglasComunicado.validar` y `errorAdjunto` |
| RF-04 | Cuatro alcances de destinatarios | Implementado y validado | `ReglasComunicado.destinosPara` y `resolverApoderados` |
| RF-05 | Inspectoría emite a los cuatro alcances | Implementado y validado | `ReglasComunicado.alcancesPermitidos` |
| RF-06 | Profesor/a Jefe solo a su curso | Implementado: curso asignado y apoderados de su curso. No puede emitir hacia talleres ni colegio completo | `ReglasComunicado.alcancesPermitidos`, `destinosPara`, `validar` |
| RF-07 | Simulación de despacho por canal con servicios mock | Local: implementado y validado. API: previsto `POST /mensajes` | `data/remote/ServiciosMock.kt`, `Despachador.kt`, `MensajeriaApi.kt` |
| RF-08 | Estado despachado/fallido por destinatario | Implementado y validado | `EnvioDestinatario`, `Despachador`, `DetalleComunicadoScreen.kt` |
| RF-09 | Bitácora de comunicados para perfiles emisores | Local: implementado y persistente. API: TODO E3-6 | `HistorialScreen.kt`, `FuenteLocal`, `RoomCache`, `MpazRepository` |
| RF-10 | Filtros por fecha, tipo y emisor | Implementado. Incluye validación de fechas y filtros combinados | `domain/ReglasHistorial.kt`, `domain/Fechas.kt`, `HistorialScreen.kt` |
| RF-11 | Registro automático de autor, fecha/hora, contenido, destinatarios y canal | Local: implementado y validado mediante transacciones. API: TODO E3-7a | `Comunicacion`, `EnvioDestinatario`, `RoomCache`, `MpazRepository.registrarEnvio` |
| RF-12 | Autorización con firma legal: solo informativa | Implementado y validado | Casilla de firma física en `RedactarComunicadoScreen`; fuerza correo y agrega el aviso (`ReglasComunicado.canalDe`, `cuerpoFinal`). No se capturan firmas. |
| RF-13 | Canal preferente del apoderado, editable solo por Inspectoría | Local: implementado y validado. API: TODO E3-7b | `ui/screen/apoderados/ApoderadosScreen.kt`, `ReglasComunicado.puedeConfigurarCanal`, `ValidadorContacto.canalesDisponibles` |
| RN-01 | Profesor/a Jefe sin colegio completo ni otro curso | Implementado y validado localmente | `ReglasComunicado.validar` |
| RN-02 | Registro inmutable | Implementado y validado mediante pruebas instrumentadas | DAOs y triggers SQLite (`data/local/AppDatabase.kt`) |
| RNF-01 | Material 3, alertas de confirmación, navegación clara | Implementado. Incluye validaciones visuales, animaciones e indicadores de progreso | `ui/`, `navigation/AppNavigation.kt` |
| RNF-02 | Wi-Fi o datos móviles; sin modo offline exigido | Modo local validado sin conexión. Modo servidor pendiente | `MpazRepository`, `RoomCache`, `SelectorFuente`, `FuenteLocal` |
| RNF-03 | Sin cámara, micrófono, GPS ni biometría | Cumplido | `AndroidManifest.xml` (INTERNET, ACCESS_NETWORK_STATE, POST_NOTIFICATIONS, VIBRATE) |
| RNF-04 | Datos ficticios | Cumplido | `data/MockData.kt` |

**RF-13 (requerimiento del caso para esta entrega).** El apoderado tiene un canal de contacto preferente (Correo electrónico, SMS o WhatsApp). Solo Inspectoría General puede cambiarlo, y solo hacia un canal para el que el apoderado tenga un dato de contacto válido; el Profesor/a Jefe puede ver el canal pero no cambiarlo.

El canal elegido se usa en los despachos posteriores (salvo las autorizaciones con firma física, que van siempre por correo, RF-12).

Se comprobó que modificar el canal preferente de un apoderado no altera sus despachos históricos. Los nuevos comunicados utilizan el canal actualizado.

En el contrato del modo API está previsto el cambio mediante `PATCH /apoderados/{id}/canal`: 403 si el perfil no es Inspectoría, 422 si el canal no tiene contacto válido. La integración remota permanece pendiente.

### Validaciones implementadas

- Validación de campos obligatorios en el inicio de sesión.
- Validación del formato del correo electrónico.
- Visualización y ocultación de contraseña.
- Mensajes de error mediante `isError` y `supportingText`.
- Iconos de advertencia en campos inválidos.
- Prevención de solicitudes repetidas de autenticación durante la carga.
- Asunto obligatorio entre 3 y 100 caracteres.
- Cuerpo obligatorio entre 10 y 1000 caracteres.
- Tipo de comunicación obligatorio.
- Alcance de envío obligatorio.
- Destino obligatorio cuando corresponda.
- Validación de los permisos del usuario emisor.
- Validación del destinatario según el perfil y alcance.
- Prevención de destinatarios duplicados.
- Adjunto opcional exclusivamente en formato PDF.
- Tamaño máximo del PDF de 5 MiB.
- Validación de fechas del historial.
- Validación de rangos de fechas invertidos.
- Validación del canal preferente según los datos de contacto.
- Confirmación antes de registrar un comunicado.

### Persistencia y operaciones CRUD

La aplicación utiliza Room como fuente de datos local, permitiendo conservar la información después de cerrar y reiniciar la aplicación.

| Operación | Implementación |
|---|---|
| Create | Registro de comunicados y sus despachos por destinatario. |
| Read | Consulta de usuarios, apoderados, historial, comunicados y canales de contacto. |
| Update | Actualización del canal preferente del apoderado por Inspectoría. |
| Delete | Restablecimiento controlado de los datos locales de demostración. |

Los comunicados y sus despachos no pueden modificarse ni eliminarse durante la operación normal, debido a la regla RN-02.

La protección de los registros se implementa mediante reglas de negocio y triggers SQLite.

El restablecimiento de datos corresponde a una operación de mantenimiento controlada que permite restaurar los datos ficticios originales.

Las operaciones relacionadas con el registro de comunicados y despachos utilizan transacciones para mantener la integridad de la información.

Las pruebas instrumentadas verifican:

- Persistencia de registros.
- Operaciones CRUD locales.
- Integridad referencial.
- Transacciones.
- Inmutabilidad de comunicados y despachos.
- Actualización de canales preferentes.
- Restablecimiento de datos.
- Conservación de los registros históricos.

### Supuestos pendientes de confirmar con la institución

- Un apoderado puede tener más de un pupilo (AP1 tiene dos).
- Talleres como entidad propia con pupilos inscritos; el Profesor/a Jefe no emite hacia talleres.
- El Profesor/a Jefe puede emitir hacia un apoderado individual de su curso. Esta interpretación está implementada y queda sujeta a la confirmación del caso.
- Sin estado interno de "firma pendiente": la app solo informa.
- Un único intento de despacho por destinatario, sin reintentos; adjunto PDF de hasta 5 MiB.
- El contrato REST (`ApiService`, `MensajeriaApi`, DTO) es propio mientras no exista el formato oficial del backend de la institución.
- El PDF adjunto se guarda en el dispositivo; para el contrato remoto se contempla transmitir solamente su nombre.

## Recursos nativos

- **Selector de archivos del sistema (SAF)** para el PDF adjunto, con copia a `filesDir/adjuntos`.
- **Apertura segura de archivos PDF** mediante `FileProvider`, otorgando permiso temporal de lectura al visor externo.
- **Persistencia de documentos PDF:** el documento se conserva dentro del almacenamiento interno de la aplicación, permitiendo abrirlo después de reiniciar o cuando el archivo original deja de estar disponible.
- **Notificación local** al finalizar un envío, con solicitud del permiso `POST_NOTIFICATIONS` (Android 13+).
- **Navegación desde la notificación:** al tocarla se abre directamente el detalle del comunicado correspondiente. Si no existe una sesión activa, se solicita iniciar sesión antes de acceder al detalle.
- **Vibración** al confirmar el resultado del envío.

Los recursos nativos fueron comprobados en el emulador Android y en el APK release firmado.

## Arquitectura

    app/src/main/java/com/mpaznotifica/app
    ├── model/        Entidades de dominio y formularios (Kotlin puro)
    ├── domain/       Reglas de negocio puras, testeables en JVM
    │                 ReglasComunicado, ReglasHistorial,
    │                 ValidadorContacto, Credenciales,
    │                 Fechas, DatosReferencia
    ├── data/
    │   ├── local/    Room: entidades, DAOs, mapeos, AppDatabase
    │   │             (base local y caché de la API)
    │   ├── remote/   Contrato y capa remota:
    │   │             Dtos, ApiService, MensajeriaApi,
    │   │             ApiResult, SesionApi, Despachador
    │   │             ApiClient, ManejadorErrores, Mapeadores,
    │   │             FuenteRemota, UrlServidor,
    │   │             ConfiguracionServidor (TODO E3-n)
    │   ├── repository/
    │   │             SelectorFuente (selección del modo)
    │   │             FuenteLocal (solo Room)
    │   │             FuenteServidor + MpazRepository
    │   │             (modo servidor: TODO E3-6 a E3-8)
    │   │             CacheLocal / AlmacenLocal / RoomCache
    │   │             PreferenciasModo (local / servidor)
    │   └── MockData.kt  Base sintética inicial
    ├── nativo/       Archivos PDF (SAF y FileProvider),
    │                 notificación local, vibración,
    │                 estado de red
    ├── viewmodel/    MpazNotificaViewModel (StateFlow)
    ├── navigation/   Grafo único de navegación,
    │                 control de sesión y acceso al detalle
    │                 desde notificaciones
    └── ui/           Tema, componentes comunes y pantallas
                      desarrolladas con Jetpack Compose

**Flujo en modo local:** pantalla (Compose) → `MpazNotificaViewModel` (StateFlow) → `SelectorFuente` → `FuenteLocal` → `RoomCache` → Room (base local, reglas del dominio) / `Despachador` → `MensajeriaSimulada`.

**Flujo previsto en modo servidor:** pantalla → `MpazNotificaViewModel` → `SelectorFuente` → `FuenteServidor` → `MpazRepository` → `FuenteRemota` → `ApiService` (Retrofit, fuente de verdad) → Room (caché, vía `RoomCache`) → pantalla; el despacho va por `Despachador` → `MensajeriaApi` (`POST /mensajes`).

El flujo de servidor corresponde al diseño original y conserva desarrollos pendientes.

Las reglas viven en `domain/` sin dependencias de Android, por lo que se prueban sin emulador.

Durante la EV3 se mantuvo la arquitectura MVVM y se fortaleció la separación entre la interfaz, las reglas de negocio y la persistencia local.

### Tecnologías utilizadas

| Tecnología | Utilización |
|---|---|
| Kotlin | Lenguaje principal |
| Jetpack Compose | Desarrollo de interfaces |
| Material Design 3 | Componentes visuales |
| MVVM | Arquitectura de aplicación |
| ViewModel | Gestión del estado y operaciones |
| StateFlow | Observación reactiva de estados |
| Coroutines | Operaciones asíncronas |
| Room | Persistencia local |
| SQLite | Motor de base de datos |
| Navigation Compose | Navegación entre pantallas |
| Retrofit | Infraestructura REST pendiente |
| OkHttp | Infraestructura HTTP |
| JUnit 4 | Pruebas originales |
| JUnit 5 | Pruebas unitarias agregadas |
| Kotest | Pruebas de reglas de negocio |
| MockK | Simulación de dependencias |
| JaCoCo | Cobertura de código |
| Gradle | Compilación y dependencias |
| Keytool | Generación del almacén de firma |
| APKSigner | Verificación de firma digital |
| Git y GitHub | Control de versiones |

### Mejoras de interfaz y navegación

- Componentes reutilizables mediante Material Design 3.
- Validaciones mediante `isError`, `supportingText` e iconos de error.
- Indicadores de progreso durante la autenticación y el envío.
- Animaciones de entrada de pantallas.
- Transiciones de navegación.
- Animaciones de actualización del historial.
- Selectores desplegables adaptados al perfil y alcance.
- Validación de fechas y rangos del historial.
- Actualización del icono `Icons.AutoMirrored.Filled.Chat`.
- Actualización de `menuAnchor()` mediante `MenuAnchorType.PrimaryNotEditable`.

## Cómo ejecutar

### Modo local (como se entrega)

1. Clonar el repositorio:

       git clone https://github.com/mpinol02/MPAZNotifica-EP3.git

2. Abrir la carpeta `MpazNotifica-EV3` en Android Studio (JDK 17 recomendado para Gradle; el código apunta a JVM 11).

3. Verificar que `gradle.properties` tenga `USAR_API=false` (valor por defecto: la app parte en modo local).

4. Sincronizar Gradle y ejecutar el módulo `app` en un emulador o dispositivo con Android 7.0 (API 24) o superior.

5. Ingresar con alguno de los usuarios de prueba. La base se crea y se siembra automáticamente en el primer arranque.

Mantener las versiones del proyecto: no actualizar sin revisar compatibilidad.

| Componente | Versión |
|---|---|
| Android Gradle Plugin | 8.5.2 |
| Gradle | 8.7 |
| Kotlin | 2.0.0 |
| Room | 2.6.1 |
| compileSdk | 35 |
| targetSdk | 35 |
| minSdk | 24 |
| JVM objetivo | 11 |
| JDK recomendado para Gradle | 17 |

AGP 8.5.2 genera una advertencia de compatibilidad con compileSdk 35. Esta advertencia no impidió compilar ni ejecutar las versiones debug y release durante las pruebas.

Para compilar desde PowerShell:

    .\gradlew.bat assembleDebug

Para ejecutar desde Android Studio, seleccionar el módulo `app` y presionar Run.

### Instalación mediante APK firmado

El APK release firmado se encuentra disponible en:

    entrega/MPAZNotifica-EV3.apk

**[Descargar APK firmado](entrega/MPAZNotifica-EV3.apk)**

Se puede instalar directamente en un dispositivo Android compatible o mediante ADB:

    adb install entrega/MPAZNotifica-EV3.apk

Para actualizar una instalación con el mismo certificado:

    adb install -r entrega/MPAZNotifica-EV3.apk

Si existe una versión debug instalada con el mismo identificador de aplicación y un certificado diferente, será necesario desinstalarla antes de instalar la versión release:

    adb uninstall com.mpaznotifica.app

**Advertencia:** desinstalar la aplicación elimina sus datos locales.

### Modo servidor (API)

La integración REST se conserva como parte del proyecto base, pero permanece pendiente. El modo local es el alcance implementado y validado en esta entrega.

Los pasos previstos para una futura integración son:

1. Levantar el backend: descomprimir el paquete del backend (queda una carpeta `backend/`; se recomienda dejarla dentro de este proyecto, junto a `app/`) y, desde ella, ejecutar `npm install` y `npm start`. Debe responder en `http://localhost:3000/health`. Con `backend/` junto a `app/`, `npm run contrato` compara los DTO de `Dtos.kt` con el JSON real del servidor.

2. Completar los `TODO(E3-n)` (ver tabla más abajo). Con el modo servidor activo (interruptor o `USAR_API=true`) y sin completarlos, la app puede fallar con `NotImplementedError`.

3. Cambiar a modo servidor (ver "Modo local y modo servidor"): con el interruptor **Modo local (sin servidor)** de la pantalla de ingreso (sin recompilar), o bien poniendo `USAR_API=true` en `gradle.properties` para que la app parta en modo servidor (en ese caso revisar también `API_BASE_URL`, que debe terminar en `/`, y sincronizar Gradle).

`USAR_API` solo define el valor INICIAL: una vez que se usa el interruptor, manda la elección guardada en el dispositivo; si se cambia `USAR_API` y se vuelve a compilar, el nuevo valor inicial vuelve a regir según el comportamiento previsto en el proyecto base.

| Dónde corre la app | Dirección |
|---|---|
| Emulador de Android Studio | `http://10.0.2.2:3000/` (valor por defecto) |
| Teléfono físico en la misma red Wi-Fi | `http://IP-DEL-PC:3000/` (por ejemplo `http://192.168.1.15:3000/`) |
| Teléfono en otra red / presentación | URL `https://...` de VS Code Port Forwarding (pestaña Puertos, visibilidad Pública) |

- **Valor de compilación:** `API_BASE_URL` en `gradle.properties` (se convierte en `BuildConfig.API_BASE_URL`). Cambiarlo exige recompilar.
- **Sin recompilar:** el diseño original contempla el botón **Servidor** en la pantalla de ingreso (solo en modo servidor), con las opciones **Probar conexión**, **Guardar** y **Restaurar**.
- La configuración prevista permite HTTP hacia `10.0.2.2`, `localhost` o una IP de red local; cualquier servidor público debe utilizar HTTPS.

El funcionamiento completo de estas opciones depende de los TODO de integración remota.

Estas instrucciones se conservan como referencia técnica del proyecto base. No corresponden a funcionalidades REST verificadas en esta entrega.

#### Endpoints previstos

| Método | Endpoint | Propósito |
|---|---|---|
| GET | `/health` | Comprobar disponibilidad del servidor |
| POST | `/auth/login` | Autenticación |
| POST | `/auth/logout` | Cierre de sesión |
| POST | `/mensajes` | Despacho de mensajes |
| PATCH | `/apoderados/{id}/canal` | Actualización del canal preferente |

La tabla presenta los endpoints identificados en la documentación original. El contrato completo está definido en las interfaces `ApiService.kt` y `MensajeriaApi.kt`, junto con los DTO del proyecto base.

Los endpoints no se presentan como operativos ni validados en esta entrega.

#### Solución de problemas

| Síntoma | Qué revisar |
|---|---|
| `NotImplementedError: E3-n ...` | Falta implementar ese TODO, o se activó el modo servidor antes de tiempo. Volver a modo local mientras tanto. |
| "No se pudo conectar con el servidor" | Para una futura integración, comprobar que el backend esté funcionando y revisar su dirección. En el emulador se utiliza `10.0.2.2`, no `localhost`. |
| Funciona en el emulador pero no en el teléfono físico | En modo servidor, utilizar la IP del PC, mantener ambos dispositivos en la misma red Wi-Fi y revisar el firewall. |
| "Una dirección pública debe usar https://" | El diseño contempla HTTPS para direcciones públicas. |
| Port Forwarding no conecta | Revisar la visibilidad del puerto y probar la dirección correspondiente. |
| "Tu sesión expiró" | Para el modo servidor, volver a ingresar después de reiniciar el backend. |
| Datos distintos tras probar mucho | En modo local, utilizar Restablecer datos locales desde el Login. En el backend de práctica está previsto `npm run reset`. |
| Pantalla negra en el emulador | Reiniciar mediante Cold Boot Now. Durante las pruebas se recuperó la visualización con este procedimiento. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | La aplicación instalada tiene un certificado diferente. Desinstalar la versión anterior antes de instalar el APK firmado. |
| APK generado como `app-release-unsigned.apk` | Revisar que exista `keystore.properties` y que la configuración de firma esté completa. |
| `Keystore was tampered with, or password was incorrect` | Comprobar las contraseñas del JKS y de la clave privada, y su correspondencia con `keystore.properties`. |
| Advertencia de AGP y compileSdk 35 | Mantener las versiones del proyecto base mientras la compilación funcione. No actualizar dependencias sin comprobar compatibilidad. |

## Modo local y modo servidor (en tiempo de ejecución)

- **Modo local:** la app usa solo su base Room, sin red, con las mismas reglas de negocio del dominio (`ReglasComunicado`, `ValidadorContacto`, `Credenciales`) y el despacho simulado. Login por hash local, redactar y enviar, historial y canal preferente operan y escriben localmente. Este es el modo funcional validado para la entrega.

- **Modo servidor:** login, datos y escrituras por la API, con Room como caché. Depende de los `TODO(E3-n)` y permanece pendiente de integración.

- **Cómo cambiar:** en la pantalla de ingreso, interruptor **Modo local (sin servidor)** (el cambio es inmediato; si había una sesión abierta se cierra). Una franja en la parte superior de todas las pantallas indica **MODO LOCAL** o **MODO SERVIDOR**. En modo local, **Restablecer datos locales** borra lo creado y restaura los datos de ejemplo.

- **Datos independientes:** el modo local usa su propia base (`mpaz_local.db`) y el modo servidor otra (`mpaz_notifica.db`, caché de la API). Lo que se crea en modo local no se sincroniza al volver al modo servidor, ni al revés.

- **Qué NO se completa aquí:** activar el modo servidor sin haber implementado los `TODO(E3-n)` puede producir `NotImplementedError`; el interruptor no los reemplaza. El modo local no depende de ninguno de ellos.

Se realizaron pruebas sin conexión a internet, verificando el inicio de sesión, consulta del historial, apoderados y registro de nuevos comunicados mediante despacho simulado.

Los despachos no corresponden a envíos reales de correo electrónico, SMS ni WhatsApp.

## Mapa de TODO de la Experiencia 3

Buscar todos los huecos con `grep -rn "TODO(E3" app/src` (en Android Studio: búsqueda global de `TODO(E3`). Cada TODO indica qué hacer, en qué archivo y el criterio de aceptación.

Los siguientes desarrollos de integración remota se mantienen como pendientes del proyecto base.

| TODO | Archivo | Qué |
|---|---|---|
| E3-1a a E3-1d | `data/remote/ApiClient.kt` | Interceptor de token, `OkHttpClient` (timeouts), `Retrofit` con Gson, `api()` y `mensajeria()` |
| E3-2 | `data/remote/ManejadorErrores.kt` | `llamar`: envolver una llamada en `ApiResult` |
| E3-3a, E3-3b | `data/remote/ManejadorErrores.kt` | Excepciones y códigos HTTP (400/401/403/404/405/409/422/5xx) a errores en español |
| E3-4a a E3-4j | `data/remote/Mapeadores.kt` | DTO ↔ modelo, rechazando respuestas incompletas |
| E3-5a a E3-5f | `data/remote/FuenteRemota.kt` | Una función por endpoint usado por la app |
| E3-6 | `data/repository/MpazRepository.kt` | `sincronizar`: API → Room, todo o nada, sin red conserva la caché |
| E3-7a, E3-7b | `data/repository/MpazRepository.kt` | Escrituras API primero (comunicado y canal preferente) |
| E3-8a, E3-8b | `data/repository/MpazRepository.kt` | Login y logout contra `/auth/login` y `/auth/logout` |
| E3-9a | `data/remote/UrlServidor.kt` | Validación de la dirección del servidor |
| E3-9b | `data/remote/ConfiguracionServidor.kt` | Dirección activa, guardar y restaurar |
| E3-9c | `data/remote/ApiClient.kt` | `probarConexion` (`GET /health`) |
| E3-P4 | `src/test` y `src/androidTest` | Pruebas esqueleto de integración remota con `@Ignore("E3: completar")` |

Las pruebas del funcionamiento local fueron ampliadas mediante JUnit 5, Kotest y pruebas instrumentadas.

Los TODO de integración remota y sus pruebas pendientes no se consideran implementados.

## Cómo correr las pruebas

Desde PowerShell, en la raíz del proyecto:

    .\gradlew.bat testDebugUnitTest

Desde Linux o macOS:

    ./gradlew testDebugUnitTest

Pruebas JVM (JUnit 4, JUnit 5, Kotest, `kotlinx-coroutines-test`, MockWebServer y MockK) en `app/src/test`:

- Pruebas originales: selector de modo local/servidor, flujos locales, credenciales, reglas de emisión por perfil, validación, adjuntos, firma física, canal preferente, historial y filtros, fechas, despacho simulado e integridad de la base sintética.

- Pruebas JUnit 5 agregadas: `ReglasComunicadoJUnit5Test` y `FechasJUnit5Test`.

- Pruebas Kotest agregadas: `ReglasComunicadoKotestTest` y `ReglasHistorialKotestTest`.

- Pruebas de cobertura agregadas: `FuenteLocalCoberturaTest` y `SelectorFuenteCoberturaTest`.

- Esqueletos de la capa remota en `app/src/test/.../remote/` (`CapaRemotaTest`, `MapeadoresYServidorTest`, `RepositorioRemotoTest`): permanecen omitidos debido a los desarrollos REST pendientes.

### Pruebas JUnit 5

Se incorporaron pruebas JUnit 5 para validar las reglas de negocio, fechas, selección de fuentes de datos y operaciones locales.

| Clase | Pruebas aprobadas |
|---|---:|
| `ReglasComunicadoJUnit5Test` | 18 |
| `FechasJUnit5Test` | 30 |
| `SelectorFuenteCoberturaTest` | 26 |
| `FuenteLocalCoberturaTest` | 14 |
| **Total** | **88** |

Ejecutar pruebas de reglas y fechas:

    .\gradlew.bat testDebugUnitTest --tests "*JUnit5Test"

Ejecutar pruebas de cobertura:

    .\gradlew.bat testDebugUnitTest --tests "*CoberturaTest"

Para ejecutar todas las pruebas JVM, incluyendo JUnit 5:

    .\gradlew.bat testDebugUnitTest

### Pruebas Kotest

Se incorporó Kotest para ampliar la validación de las reglas de negocio relacionadas con los comunicados y los filtros del historial.

| Clase | Pruebas aprobadas |
|---|---:|
| `ReglasComunicadoKotestTest` | 16 |
| `ReglasHistorialKotestTest` | 14 |
| **Total** | **30** |

Ejecutar:

    .\gradlew.bat testDebugUnitTest --tests "*KotestTest"

### Pruebas instrumentadas

Se incorporaron las siguientes clases en `app/src/androidTest`:

- `RoomPersistenciaTest.kt`: persistencia, operaciones CRUD, integridad referencial, transacciones, inmutabilidad y restablecimiento de datos.
- `MpazRepositoryTest.kt`: operaciones locales de la capa Repository.
- `PreferenciasModoTest.kt`: persistencia y cambios de preferencias del modo de funcionamiento.
- `RoomReferenciaTest.kt`: operaciones sobre datos de referencia y registros locales.

| Clase | Pruebas aprobadas |
|---|---:|
| `RoomPersistenciaTest` | 28 |
| `MpazRepositoryTest` | 20 |
| `PreferenciasModoTest` | 11 |
| `RoomReferenciaTest` | 9 |
| **Total** | **68** |

Para ejecutar las cuatro clases, mantener un emulador o dispositivo conectado:

    .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.mpaznotifica.app.RoomPersistenciaTest,com.mpaznotifica.app.MpazRepositoryTest,com.mpaznotifica.app.PreferenciasModoTest,com.mpaznotifica.app.RoomReferenciaTest"

El proyecto conserva la prueba de interfaz original (Compose UI Test) en `app/src/androidTest` (`LoginUiTest`).

Para ejecutar todas las pruebas instrumentadas disponibles:

    .\gradlew.bat connectedDebugAndroidTest

`LoginUiTest` no está incluida en las 68 pruebas instrumentadas informadas.

Pruebas del backend (referencia del proyecto base):

    cd backend
    npm test

El backend no forma parte del alcance validado en esta entrega.

### Resultados de las pruebas automatizadas

| Tipo de prueba | Aprobadas | Fallidas |
|---|---:|---:|
| JUnit 4 originales | 102 | 0 |
| JUnit 5 | 88 | 0 |
| Kotest | 30 | 0 |
| Pruebas instrumentadas Android | 68 | 0 |
| **Total** | **288** | **0** |

**Resultados generales:**

- 220 pruebas unitarias aprobadas.
- 68 pruebas instrumentadas aprobadas.
- 288 pruebas automatizadas aprobadas.
- 0 pruebas fallidas en las ejecuciones informadas.
- 43 pruebas REST omitidas.

Las 43 pruebas REST omitidas no forman parte de las 288 pruebas aprobadas.

Considerando las pruebas aprobadas y omitidas, las ejecuciones informadas comprenden 331 casos de prueba.

**Informes:**

- [Informe de pruebas unitarias JUnit y Kotest](docs/evidencias/pruebas/unitarias/index.html).
- [Informe de pruebas instrumentadas Android](docs/evidencias/pruebas/instrumentadas/index.html).

### Cobertura de código con JaCoCo

Se incorporó JaCoCo para medir la cobertura de instrucciones, líneas y ramas mediante la combinación de pruebas unitarias e instrumentadas.

La configuración permite generar un informe HTML y un informe CSV para consultar la cobertura por paquete y clase.

Primero ejecutar las pruebas unitarias:

    .\gradlew.bat testDebugUnitTest

Después ejecutar las pruebas instrumentadas indicadas anteriormente, con el emulador conectado.

Finalmente, generar el informe:

    .\gradlew.bat jacocoTestReport

Abrir el informe HTML:

    Invoke-Item app/build/reports/jacoco/html/index.html

El informe CSV se encuentra en:

    app/build/reports/jacoco/cobertura.csv

### Resultados finales de cobertura

Los porcentajes corresponden a la cobertura de instrucciones obtenida del informe CSV de JaCoCo.

| Paquete | Cobertura de instrucciones |
|---|---:|
| `com.mpaznotifica.app.data` | 100,00% |
| `com.mpaznotifica.app.domain` | 96,16% |
| `com.mpaznotifica.app.data.repository` | **91,75%** |
| `com.mpaznotifica.app.data.local` | 90,05% |
| `com.mpaznotifica.app.model` | 74,63% |
| `com.mpaznotifica.app.data.remote` | 21,40% |
| `com.mpaznotifica.app.viewmodel` | 0,00% |
| `com.mpaznotifica.app` | 0,00% |
| `com.mpaznotifica.app.ui.theme` | 0,00% |
| `com.mpaznotifica.app.ui.components` | 0,00% |
| `com.mpaznotifica.app.navigation` | 0,00% |
| `com.mpaznotifica.app.nativo` | 0,00% |
| `com.mpaznotifica.app.ui.screen` | 0,00% |
| `com.mpaznotifica.app.ui.screen.comunicados` | 0,00% |
| `com.mpaznotifica.app.ui.screen.apoderados` | 0,00% |

### Cumplimiento del objetivo de cobertura

| Indicador | Resultado |
|---|---:|
| Objetivo de cobertura para Repository o ViewModel | 80,00% |
| Cobertura obtenida en Repository | 91,75% |
| Diferencia sobre el objetivo | 11,75 puntos porcentuales |

La capa Repository alcanza un **91,75 % de cobertura de instrucciones**, superando en 11,75 puntos porcentuales el objetivo del 80 % establecido para la evaluación.

Los resultados corresponden al informe combinado de JaCoCo, generado mediante pruebas unitarias e instrumentadas.

La cobertura de Repository no representa la cobertura global de la aplicación.

El ViewModel y las capas de interfaz presentan un 0,00 % de cobertura de instrucciones en este informe. Sus funcionalidades fueron comprobadas manualmente, lo que no equivale a cobertura automatizada.

La integración REST permanece pendiente.

**Informes de cobertura:**

- [Informe completo JaCoCo](docs/evidencias/pruebas/jacoco-html/index.html).
- [Cobertura de la capa Repository](docs/evidencias/pruebas/jacoco-html/com.mpaznotifica.app.data.repository/index.html).
- [Cobertura del dominio](docs/evidencias/pruebas/jacoco-html/com.mpaznotifica.app.domain/index.html).
- [Cobertura de la capa local Room](docs/evidencias/pruebas/jacoco-html/com.mpaznotifica.app.data.local/index.html).
- [Informe CSV de cobertura](docs/evidencias/pruebas/cobertura.csv).

Los informes HTML pueden consultarse descargando o clonando el repositorio y abriendo los archivos `index.html` mediante un navegador.

### Pruebas funcionales

Se realizaron siete fases de comprobación manual en el emulador Android.

| Fase | Funcionalidad validada | Resultado |
|---|---|---|
| 1 | Inicio de sesión y validaciones | Aprobada |
| 2 | Redacción, tipos, alcances y selectores | Aprobada |
| 3 | Envío, firma física, PDF y notificaciones | Aprobada |
| 4 | Historial, fechas y filtros combinados | Aprobada |
| 5 | Canales preferentes e inmutabilidad del historial | Aprobada |
| 6 | Permisos y restricciones de los profesores jefes | Aprobada |
| 7 | Persistencia y funcionamiento sin conexión | Aprobada |

### Validación funcional del APK release

También se realizaron 12 comprobaciones funcionales sobre el APK release firmado.

Se comprobó:

- Instalación y apertura de la aplicación.
- Inicio de sesión con Inspectoría General.
- Inicio de sesión con Profesor Jefe de 4° Básico A.
- Inicio de sesión con Profesor Jefe de 2° Básico A.
- Carga de los datos ficticios iniciales.
- Redacción de un comunicado institucional.
- Selección y registro de un documento PDF.
- Despacho simulado de comunicado con firma física.
- Generación de la notificación local.
- Navegación al detalle desde la notificación.
- Apertura del documento PDF.
- Persistencia del comunicado y su PDF después de reiniciar.

**Resultado de la validación funcional: 12 de 12 comprobaciones aprobadas.**

Posteriormente, se verificó la recompilación del APK firmado con el certificado original y la actualización de la aplicación mediante ADB.

## Cómo generar el APK firmado

El proyecto incluye un APK release firmado en la carpeta `entrega/`.

    entrega/MPAZNotifica-EV3.apk

El build `release` funciona sin firma (APK sin firmar); si se definen estas cuatro propiedades, se firma automáticamente:

| Propiedad | Contenido |
|---|---|
| `MPAZ_STORE_FILE` | Ruta del almacén de claves (`.jks`) |
| `MPAZ_STORE_PASSWORD` | Clave del keystore |
| `MPAZ_KEY_ALIAS` | Alias de la llave |
| `MPAZ_KEY_PASSWORD` | Clave de la llave |

Se buscan, en este orden, en `keystore.properties` (junto a `gradlew`; copiar `keystore.properties.example`), en las propiedades de Gradle y en variables de entorno.

Para esta evaluación se generó una llave exclusiva del proyecto:

    mpaz-evaluacion.jks

El archivo `.jks` y las credenciales reales se mantienen fuera del repositorio público mediante `.gitignore`.

La plantilla `keystore.properties.example` permite conocer la configuración necesaria sin publicar contraseñas.

La entrega del JKS y su configuración privada debe coordinarse con el docente, considerando que el proyecto base solicita estos archivos como parte de la evaluación.

### Pasos para generar el APK

1. Crear un keystore propio, si todavía no existe:

       keytool -genkeypair -v -keystore mpaz-evaluacion.jks -storetype JKS -alias mpaz-evaluacion -keyalg RSA -keysize 2048 -validity 10000

   Si ya existe el almacén utilizado para la evaluación, conservarlo para mantener la identidad de firma de la aplicación.

2. Crear `keystore.properties` a partir del ejemplo con las cuatro propiedades.

       MPAZ_STORE_FILE=../mpaz-evaluacion.jks
       MPAZ_STORE_PASSWORD=REEMPLAZAR
       MPAZ_KEY_ALIAS=mpaz-evaluacion
       MPAZ_KEY_PASSWORD=REEMPLAZAR

   La ruta indicada es relativa al módulo `app`.

   Los valores `REEMPLAZAR` deben sustituirse por las contraseñas privadas correspondientes.

3. Comprobar que Gradle reconoce la configuración:

       .\gradlew.bat signingReport

   El apartado `Variant: release` debe mostrar el certificado sin errores.

4. Compilar la versión release:

       .\gradlew.bat assembleRelease

5. El APK firmado queda en:

       app/build/outputs/apk/release/app-release.apk

6. Verificar la firma:

       apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk

7. Copiar el APK firmado a la carpeta de entrega:

       Copy-Item app/build/outputs/apk/release/app-release.apk entrega/MPAZNotifica-EV3.apk -Force

8. Instalar o actualizar el APK mediante ADB:

       adb install -r entrega/MPAZNotifica-EV3.apk

### Información del certificado generado

| Característica | Resultado |
|---|---|
| Almacén | `mpaz-evaluacion.jks` |
| Formato | JKS |
| Alias | `mpaz-evaluacion` |
| Algoritmo | RSA |
| Tamaño de clave | 2048 bits |
| Algoritmo del certificado | SHA384withRSA |
| Fecha de creación | 26/11/2026 |
| Validez | 10.000 días |
| Vencimiento | 13/04/2054 |
| Número de firmantes | 1 |
| Verificación APKSigner | Verifies |
| Instalación mediante ADB | Success |

### Esquemas de firma verificados

| Esquema | Resultado |
|---|---|
| APK Signature Scheme v1 | false |
| APK Signature Scheme v2 | true |
| APK Signature Scheme v3 | false |
| APK Signature Scheme v3.1 | false |
| APK Signature Scheme v4 | false |

El APK utiliza una firma válida mediante APK Signature Scheme v2.

Esta firma es compatible con el `minSdk=24` establecido en el proyecto.

### Huella SHA-256 del certificado

    645169fd53719a9049db19a502e1105bfdfaeb2b39800a4fcc467ddb42401bef

### Huella SHA-1 del certificado

    10e4fc99f0517f303cc28e8ddffb2662e1a6692c

Las huellas del APK coinciden con las del certificado generado mediante Keytool.

La versión release fue instalada y probada correctamente en el emulador Android.

La verificación criptográfica final se realizó sobre el APK disponible en la carpeta `entrega`.

**Evidencias del APK:**

- [Descargar APK release firmado](entrega/MPAZNotifica-EV3.apk).
- [Verificación de firma digital](docs/evidencias/apk/verificacion-firma.txt).
- [Hash SHA-256 del archivo APK](docs/evidencias/apk/integridad-apk.txt).

La huella del certificado identifica al firmante, mientras que el hash SHA-256 del archivo APK permite comprobar la integridad del archivo distribuido.

### Protección de las claves

Se actualizó `.gitignore` para impedir que se publiquen archivos privados de firma:

- `*.jks`
- `*.keystore`
- `*.p12`
- `*.pfx`
- `keystore.properties`
- `keystore.txt`
- `.env`

El APK firmado y la plantilla de configuración se encuentran versionados en GitHub.

Las contraseñas reales no deben publicarse en el repositorio.

Para verificar que los archivos privados están excluidos:

    git check-ignore -v mpaz-evaluacion.jks keystore.properties keystore.txt

Para comprobar que no están registrados en Git:

    git ls-files -- "*.jks" "keystore.properties" "keystore.txt"

Subir `versionCode` y `versionName` en `app/build.gradle.kts` cuando corresponda a una nueva versión de la aplicación.

## Control de versiones

El desarrollo de la Evaluación Parcial 3 se organizó en etapas, validando las modificaciones antes de incorporarlas al repositorio.

| Commit | Desarrollo |
|---|---|
| `8d87935` | Base inicial para el desarrollo de la EV3 |
| `883aaa0` | Corrección de configuración Gradle |
| `f25da86` | Apertura del detalle desde notificación |
| `aecd08a` | Apertura segura y persistencia de PDF |
| `387379b` | Validaciones, reglas de negocio y animaciones |
| `c90290e` | Pruebas JUnit 5 y persistencia CRUD con Room |
| `74e48df` | Kotest, cobertura JaCoCo y optimización de componentes |
| `173ac07` | APK release firmado y protección de configuración de firma |

La última etapa corresponde a la actualización del README y la incorporación de evidencias de la evaluación.

Las contribuciones individuales de los integrantes se acreditan mediante los commits, ramas y Pull Requests efectivamente realizados.

## Evidencias

Las evidencias de la Evaluación Parcial 3 se encuentran organizadas dentro de `docs/evidencias/`.

    docs/
    ├── Consultas_Pendientes_DSY1105-007.pdf
    ├── Requerimientos_DSY1105-007.pdf
    └── evidencias/
        ├── funcionales/
        │   ├── 01-login.png
        │   ├── 02-validaciones-login.png
        │   ├── 03-panel-inspectoria.png
        │   ├── 04-panel-profesor-4A.png
        │   ├── 05-panel-profesor-2A.png
        │   ├── 06-tipos-comunicacion.png
        │   ├── 07-alcances.png
        │   ├── 08-validaciones-formulario.png
        │   ├── 09-confirmacion-envio.png
        │   ├── 10-envio-exitoso.png
        │   ├── 11-detalle-comunicado.png
        │   ├── 12-apertura-pdf.png
        │   ├── 13-despacho-canal-actualizado.png
        │   ├── 14-curso-profesor-4A.png
        │   ├── 15-apoderados-profesor-4A.png
        │   ├── 16-curso-profesor-2A.png
        │   ├── 17-apoderados-profesor-2A.png
        │   ├── 18-alcances-profesor.png
        │   ├── 19-historial.png
        │   ├── 20-despachos-correo.png
        │   └── 21-detalle-sin-internet.png
        ├── pruebas/
        │   ├── cobertura.csv
        │   ├── unitarias/
        │   │   └── index.html
        │   ├── instrumentadas/
        │   │   └── index.html
        │   └── jacoco-html/
        │       └── index.html
        └── apk/
            ├── verificacion-firma.txt
            └── integridad-apk.txt

### Evidencias funcionales

Se incorporaron 21 capturas reales obtenidas durante las pruebas funcionales de la aplicación.

#### Inicio de sesión y perfiles

| Evidencia | Descripción | Captura |
|---|---|---|
| 01 | Pantalla de inicio de sesión | [Ver captura](docs/evidencias/funcionales/01-login.png) |
| 02 | Validaciones del inicio de sesión | [Ver captura](docs/evidencias/funcionales/02-validaciones-login.png) |
| 03 | Panel de Inspectoría General | [Ver captura](docs/evidencias/funcionales/03-panel-inspectoria.png) |
| 04 | Panel del Profesor Jefe de 4° Básico A | [Ver captura](docs/evidencias/funcionales/04-panel-profesor-4A.png) |
| 05 | Panel del Profesor Jefe de 2° Básico A | [Ver captura](docs/evidencias/funcionales/05-panel-profesor-2A.png) |

#### Redacción y envío de comunicados

| Evidencia | Descripción | Captura |
|---|---|---|
| 06 | Tipos de comunicación disponibles | [Ver captura](docs/evidencias/funcionales/06-tipos-comunicacion.png) |
| 07 | Alcances de envío | [Ver captura](docs/evidencias/funcionales/07-alcances.png) |
| 08 | Validaciones del formulario | [Ver captura](docs/evidencias/funcionales/08-validaciones-formulario.png) |
| 09 | Confirmación previa al envío | [Ver captura](docs/evidencias/funcionales/09-confirmacion-envio.png) |
| 10 | Resultado del envío simulado | [Ver captura](docs/evidencias/funcionales/10-envio-exitoso.png) |
| 11 | Detalle del comunicado registrado | [Ver captura](docs/evidencias/funcionales/11-detalle-comunicado.png) |
| 12 | Apertura del documento PDF | [Ver captura](docs/evidencias/funcionales/12-apertura-pdf.png) |

#### Canales preferentes

| Evidencia | Descripción | Captura |
|---|---|---|
| 13 | Despacho utilizando el canal preferente actualizado | [Ver captura](docs/evidencias/funcionales/13-despacho-canal-actualizado.png) |

#### Permisos y restricciones de profesores

| Evidencia | Descripción | Captura |
|---|---|---|
| 14 | Curso autorizado del Profesor Jefe de 4° Básico A | [Ver captura](docs/evidencias/funcionales/14-curso-profesor-4A.png) |
| 15 | Apoderados autorizados de 4° Básico A | [Ver captura](docs/evidencias/funcionales/15-apoderados-profesor-4A.png) |
| 16 | Curso autorizado del Profesor Jefe de 2° Básico A | [Ver captura](docs/evidencias/funcionales/16-curso-profesor-2A.png) |
| 17 | Apoderados autorizados de 2° Básico A | [Ver captura](docs/evidencias/funcionales/17-apoderados-profesor-2A.png) |
| 18 | Restricciones de alcance del Profesor Jefe | [Ver captura](docs/evidencias/funcionales/18-alcances-profesor.png) |

#### Historial y funcionamiento local

| Evidencia | Descripción | Captura |
|---|---|---|
| 19 | Historial de comunicados | [Ver captura](docs/evidencias/funcionales/19-historial.png) |
| 20 | Despachos mediante correo electrónico simulado | [Ver captura](docs/evidencias/funcionales/20-despachos-correo.png) |
| 21 | Detalle de comunicado registrado sin conexión a internet | [Ver captura](docs/evidencias/funcionales/21-detalle-sin-internet.png) |

### Informes de pruebas automatizadas

- [Informe de pruebas unitarias JUnit y Kotest](docs/evidencias/pruebas/unitarias/index.html).
- [Informe de pruebas instrumentadas Android](docs/evidencias/pruebas/instrumentadas/index.html).
- [Pruebas de persistencia Room](docs/evidencias/pruebas/instrumentadas/com.mpaznotifica.app.RoomPersistenciaTest.html).
- [Pruebas de Repository](docs/evidencias/pruebas/instrumentadas/com.mpaznotifica.app.MpazRepositoryTest.html).
- [Pruebas de preferencias de modo](docs/evidencias/pruebas/instrumentadas/com.mpaznotifica.app.PreferenciasModoTest.html).
- [Pruebas de datos de referencia Room](docs/evidencias/pruebas/instrumentadas/com.mpaznotifica.app.RoomReferenciaTest.html).

### Cobertura de código

- [Informe completo JaCoCo](docs/evidencias/pruebas/jacoco-html/index.html).
- [Cobertura de la capa Repository](docs/evidencias/pruebas/jacoco-html/com.mpaznotifica.app.data.repository/index.html).
- [Cobertura de la capa Domain](docs/evidencias/pruebas/jacoco-html/com.mpaznotifica.app.domain/index.html).
- [Cobertura de la capa Data Local](docs/evidencias/pruebas/jacoco-html/com.mpaznotifica.app.data.local/index.html).
- [Informe CSV de cobertura](docs/evidencias/pruebas/cobertura.csv).

### APK firmado

- [Descargar APK release firmado](entrega/MPAZNotifica-EV3.apk).
- [Verificación de firma digital](docs/evidencias/apk/verificacion-firma.txt).
- [Hash SHA-256 del APK](docs/evidencias/apk/integridad-apk.txt).
- [Plantilla de configuración de firma](keystore.properties.example).

Los informes HTML pueden consultarse descargando o clonando el repositorio y abriendo los archivos `index.html` mediante un navegador.

### Documentación del caso

- [Requerimientos DSY1105-007](docs/Requerimientos_DSY1105-007.pdf).
- [Consultas pendientes DSY1105-007](docs/Consultas_Pendientes_DSY1105-007.pdf).

## Resultados finales de la Evaluación Parcial 3

| Indicador | Resultado |
|---|---|
| Requerimientos funcionales RF-01 a RF-13 | Implementados en modo local |
| Arquitectura MVVM | Implementada |
| Persistencia mediante Room | Implementada y validada |
| Operaciones CRUD locales | Validadas |
| Validaciones y reglas de negocio | Implementadas y validadas |
| Animaciones y componentes Material 3 | Implementados |
| Recursos nativos Android | Implementados y validados |
| Pruebas JUnit 4 | 102 aprobadas |
| Pruebas JUnit 5 | 88 aprobadas |
| Pruebas Kotest | 30 aprobadas |
| Pruebas instrumentadas | 68 aprobadas |
| Total de pruebas automatizadas aprobadas | 288 |
| Pruebas fallidas en las ejecuciones informadas | 0 |
| Pruebas REST omitidas | 43 |
| Cobertura de instrucciones Repository | 91,75% |
| Cobertura de instrucciones Domain | 96,16% |
| Cobertura de instrucciones Data Local | 90,05% |
| Fases de validación funcional | 7 de 7 aprobadas |
| Validación funcional APK release | 12 de 12 aprobadas |
| Capturas funcionales incorporadas | 21 |
| APK release firmado | Generado y verificado |
| Integración REST | Pendiente |
| Entrega privada del JKS | Sujeta a confirmación docente |

La versión entregada corresponde al alcance local funcional de MPAZ Notifica.

La aplicación permite gestionar comunicados escolares mediante una arquitectura MVVM, persistencia Room, reglas de negocio diferenciadas por perfil, validaciones, recursos nativos y una interfaz desarrollada con Jetpack Compose y Material Design 3.

La documentación y las evidencias permiten comprobar los resultados obtenidos durante el desarrollo de la Evaluación Parcial 3.
