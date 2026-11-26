# MPAZ Notifica

Aplicación móvil Android (Kotlin, Jetpack Compose, Material Design 3, MVVM, Room) para la Escuela Escritora Marcela Paz (San Bernardo): reemplaza el flujo analógico de comunicaciones escolares por emisión, segmentación y trazabilidad de comunicados hacia apoderados, con permisos de emisión diferenciados por perfil.

Es un MVP académico. El proyecto se entrega en **modo local** (Room + despacho simulado, sin servidor; se puede cambiar a modo servidor en tiempo de ejecución, ver "Modo local y modo servidor") y con el **andamiaje de integración con una API REST** (Retrofit) listo para completar: los huecos están marcados con `TODO(E3-n)`. Toda la información es **100 % ficticia** (RNF-04). Los apoderados son destinatarios finales y no operan la app.

El backend de práctica (Node.js) se entrega en un paquete aparte (carpeta `backend/`, con su propio README, `docs/API.md` y una colección Postman).

## Perfiles y usuarios de prueba

| Perfil | Usuario | Clave | Puede emitir |
|---|---|---|---|
| Inspectoría General (emisor principal) | `inspectoria@mpaznotifica.cl` | `inspectoria123` | Todos los tipos y los cuatro alcances. Configura el canal preferente de los apoderados. |
| Profesor/a Jefe 4° Básico A | `profesor.4basicoA@mpaznotifica.cl` | `profesor123` | Tipos pedagógica, citación y salida pedagógica, solo hacia su curso o un apoderado de su curso. |
| Profesor/a Jefe 2° Básico A | `profesor.2basicoA@mpaznotifica.cl` | `profesor123` | Igual que el anterior, para 2° Básico A. |

Las claves no se almacenan en texto plano: se guarda el SHA-256 de `sal de demo + clave` (`domain/Credenciales.kt`). La sal es fija y solo sirve para fines didácticos. En modo API el servidor valida el ingreso (`POST /auth/login`).

## Requerimientos

| Código | Requerimiento | Estado en la entrega | Dónde |
|---|---|---|---|
| RF-01 | Inicio de sesión diferenciado por perfil | Local: implementado. API: TODO E3-8 | `ui/screen/LoginScreen.kt`, `MpazNotificaViewModel.iniciarSesion`, `domain/Credenciales.kt` |
| RF-02 | Opciones de emisión según perfil | Implementado | `ReglasComunicado.tiposPermitidos` y `alcancesPermitidos` |
| RF-03 | Redactar con asunto, cuerpo, tipo y PDF opcional | Implementado | `RedactarComunicadoScreen.kt`, `nativo/ArchivosPdf.kt`, `ReglasComunicado.validar` y `errorAdjunto` |
| RF-04 | Cuatro alcances de destinatarios | Implementado | `ReglasComunicado.destinosPara` y `resolverApoderados` |
| RF-05 | Inspectoría emite a los cuatro alcances | Implementado | `ReglasComunicado.alcancesPermitidos` |
| RF-06 | Profesor/a Jefe solo a su curso | Implementado (talleres: supuesto pendiente) | `ReglasComunicado.alcancesPermitidos`, `validar` |
| RF-07 | Simulación de despacho por canal con servicios mock | Local: implementado. API: `POST /mensajes` | `data/remote/ServiciosMock.kt`, `Despachador.kt`, `MensajeriaApi.kt` |
| RF-08 | Estado despachado/fallido por destinatario | Implementado | `EnvioDestinatario`, `Despachador`, `DetalleComunicadoScreen.kt` |
| RF-09 | Bitácora de comunicados para perfiles emisores | Local: implementado. API: TODO E3-6 | `HistorialScreen.kt`, `MpazRepository` |
| RF-10 | Filtros por fecha, tipo y emisor | Implementado | `domain/ReglasHistorial.kt`, `HistorialScreen.kt` |
| RF-11 | Registro automático de autor, fecha/hora, contenido, destinatarios y canal | Local: implementado. API: TODO E3-7a | `Comunicacion` + `EnvioDestinatario`, `MpazRepository.registrarEnvio` |
| RF-12 | Autorización con firma legal: solo informativa | Implementado | Casilla de firma física en `RedactarComunicadoScreen`; fuerza correo y agrega el aviso (`ReglasComunicado.canalDe`, `cuerpoFinal`). No se capturan firmas. |
| RF-13 | Canal preferente del apoderado, editable solo por Inspectoría | Local: implementado. API: TODO E3-7b | `ui/screen/apoderados/ApoderadosScreen.kt`, `ReglasComunicado.puedeConfigurarCanal`, `ValidadorContacto.canalesDisponibles` |
| RN-01 | Profesor/a Jefe sin colegio completo ni otro curso | Implementado (en modo API el servidor lo repite: 403) | `ReglasComunicado.validar` |
| RN-02 | Registro inmutable | Implementado | DAOs sin `@Update`/`@Delete` de uso y triggers SQLite (`data/local/AppDatabase.kt`); el servidor responde 405 |
| RNF-01 | Material 3, alertas de confirmación, navegación clara | Implementado | `ui/`, `navigation/AppNavigation.kt` |
| RNF-02 | Wi-Fi o datos móviles; sin modo offline exigido | Modo servidor: requiere conexión para enviar; sin red se ven los últimos datos guardados. Respaldo: modo local (todo con la base del dispositivo) | `MpazRepository`, `RoomCache`, `SelectorFuente`, `FuenteLocal` |
| RNF-03 | Sin cámara, micrófono, GPS ni biometría | Cumplido | `AndroidManifest.xml` (INTERNET, ACCESS_NETWORK_STATE, POST_NOTIFICATIONS, VIBRATE) |
| RNF-04 | Datos ficticios | Cumplido | `data/MockData.kt` |

**RF-13 (requerimiento del caso para esta entrega).** El apoderado tiene un canal de contacto preferente (Correo electrónico, SMS o WhatsApp). Solo Inspectoría General puede cambiarlo, y solo hacia un canal para el que el apoderado tenga un dato de contacto válido; el Profesor/a Jefe puede ver el canal pero no cambiarlo. El canal elegido se usa en los despachos posteriores (salvo las autorizaciones con firma física, que van siempre por correo, RF-12). En modo API el cambio se hace con `PATCH /apoderados/{id}/canal`: 403 si el perfil no es Inspectoría, 422 si el canal no tiene contacto válido.

### Supuestos pendientes de confirmar con la institución

- Un apoderado puede tener más de un pupilo (AP1 tiene dos).
- Talleres como entidad propia con pupilos inscritos; el Profesor/a Jefe no emite hacia talleres.
- Sin estado interno de "firma pendiente": la app solo informa.
- Un único intento de despacho por destinatario, sin reintentos; adjunto PDF de hasta 5 MB.
- El contrato REST (`ApiService`, `MensajeriaApi`, DTO) es propio mientras no exista el formato oficial del backend de la institución.
- El PDF adjunto se guarda en el dispositivo; al servidor solo viaja su nombre.

## Recursos nativos

- **Selector de archivos del sistema (SAF)** para el PDF adjunto, con copia a `filesDir/adjuntos`.
- **Notificación local** al finalizar un envío, con solicitud del permiso `POST_NOTIFICATIONS` (Android 13+); al tocarla se abre la app.
- **Vibración** al confirmar el resultado del envío.

## Arquitectura

```
app/src/main/java/com/mpaznotifica/app
├── model/        Entidades de dominio y formularios (Kotlin puro)
├── domain/       Reglas de negocio puras, testeables en JVM (ReglasComunicado, ReglasHistorial,
│                 ValidadorContacto, Credenciales, Fechas, DatosReferencia)
├── data/
│   ├── local/    Room: entidades, DAOs, mapeos, AppDatabase (base local y caché de la API)
│   ├── remote/   Contrato y capa remota:
│   │               Dtos, ApiService, MensajeriaApi, ApiResult, SesionApi, Despachador   (completos)
│   │               ApiClient, ManejadorErrores, Mapeadores, FuenteRemota,
│   │               UrlServidor, ConfiguracionServidor                                   (TODO E3-n)
│   ├── repository/ SelectorFuente (elige la fuente según la preferencia de modo), FuenteLocal (solo Room),
│   │               FuenteServidor + MpazRepository (modo servidor: TODO E3-6 a E3-8),
│   │               CacheLocal / AlmacenLocal / RoomCache (completas), PreferenciaModo (local / servidor)
│   └── MockData.kt  Base sintética inicial
├── nativo/       Archivos PDF (SAF), notificación local, vibración, estado de red
├── viewmodel/    MpazNotificaViewModel (StateFlow; el modo local / servidor se lee en cada operación)
├── navigation/   Grafo único de navegación con control de sesión
└── ui/           Tema, componentes comunes (incl. diálogo "Servidor") y pantallas (Compose)
```

Flujo en modo local: pantalla (Compose) → `MpazNotificaViewModel` (StateFlow) → `SelectorFuente` → `FuenteLocal` → Room (base local, reglas del dominio) / `Despachador` → `MensajeriaSimulada`.

Flujo en modo servidor: pantalla → `MpazNotificaViewModel` → `SelectorFuente` → `FuenteServidor` → `MpazRepository` → `FuenteRemota` → `ApiService` (Retrofit, **fuente de verdad**) → Room (**caché**, vía `RoomCache`) → pantalla; el despacho va por `Despachador` → `MensajeriaApi` (`POST /mensajes`).

Las reglas viven en `domain/` sin dependencias de Android, por lo que se prueban sin emulador.

## Cómo ejecutar

### Modo local (como se entrega)

1. Abrir la carpeta del proyecto en Android Studio (JDK 17 recomendado para Gradle; el código apunta a JVM 11).
2. Verificar que `gradle.properties` tenga `USAR_API=false` (valor por defecto: la app parte en modo local).
3. Sincronizar Gradle y ejecutar el módulo `app` en un emulador o dispositivo con Android 7.0 (API 24) o superior.
4. Ingresar con alguno de los usuarios de prueba. La base se crea y se siembra automáticamente en el primer arranque.

Mantener las versiones del proyecto (AGP 8.5.2, Gradle 8.7, Kotlin 2.0.0): no actualizar sin revisar compatibilidad.

### Modo servidor (API)

1. Levantar el backend: descomprimir el paquete del backend (queda una carpeta `backend/`; se recomienda dejarla dentro de este proyecto, junto a `app/`, para versionar todo en el mismo repositorio) y, desde ella, ejecutar `npm install` y `npm start`. Debe responder en `http://localhost:3000/health`. Con `backend/` junto a `app/`, `npm run contrato` compara los DTO de `Dtos.kt` con el JSON real del servidor.
2. Completar los `TODO(E3-n)` (ver tabla más abajo). Con el modo servidor activo (interruptor o `USAR_API=true`) y sin completarlos, la app falla con `NotImplementedError`.
3. Cambiar a modo servidor (ver "Modo local y modo servidor"): con el interruptor **Modo local (sin servidor)** de la pantalla de ingreso (sin recompilar), o bien poniendo `USAR_API=true` en `gradle.properties` para que la app parta en modo servidor (en ese caso revisar también `API_BASE_URL`, que debe terminar en `/`, y sincronizar Gradle). `USAR_API` solo define el valor INICIAL: una vez que se usa el interruptor, manda la elección guardada en el dispositivo; si se cambia `USAR_API` y se vuelve a compilar, el nuevo valor inicial vuelve a regir.

| Dónde corre la app | Dirección |
|---|---|
| Emulador de Android Studio | `http://10.0.2.2:3000/` (valor por defecto) |
| Teléfono físico en la misma red Wi-Fi | `http://IP-DEL-PC:3000/` (por ejemplo `http://192.168.1.15:3000/`) |
| Teléfono en otra red / presentación | URL `https://...` de VS Code Port Forwarding (pestaña **Puertos**, visibilidad **Pública**) |

- **Valor de compilación**: `API_BASE_URL` en `gradle.properties` (se convierte en `BuildConfig.API_BASE_URL`). Cambiarlo exige recompilar.
- **Sin recompilar**: en la pantalla de ingreso (solo en modo servidor) el botón **Servidor** abre un diálogo: se escribe la dirección, **Probar conexión** consulta `GET /health`, **Guardar** la conserva en el dispositivo y **Restaurar** vuelve al valor de compilación. El `http://` solo se acepta hacia `10.0.2.2`, `localhost` o una IP de red local; cualquier servidor público debe usar `https://`.

#### Solución de problemas

| Síntoma | Qué revisar |
|---|---|
| `NotImplementedError: E3-n ...` | Falta implementar ese TODO (el mensaje dice cuál), o se activó el modo servidor (interruptor o `USAR_API=true`) antes de tiempo. Volver a modo local mientras tanto. |
| "No se pudo conectar con el servidor" | ¿Está corriendo `npm start`? Abrir `/health` en el navegador del PC. En el emulador la dirección es `10.0.2.2`, no `localhost`. |
| Funciona en el emulador pero no en el teléfono físico | Usar la IP del PC (no `10.0.2.2`), mismo Wi-Fi, y permitir Node.js en el firewall (puerto 3000). |
| "Una dirección pública debe usar https://" | Es intencional. Con Port Forwarding usar la URL `https://`. |
| Port Forwarding no conecta | Visibilidad del puerto **Pública** y la URL termina en el puerto reenviado; probarla antes en el navegador. |
| "Tu sesión expiró" | El token vive en memoria del servidor: si se reinició, volver a ingresar. |
| Datos distintos tras probar mucho | Detener el servidor, `npm run reset` y volver a iniciar. |

## Modo local y modo servidor (en tiempo de ejecución)

- **Modo local** (respaldo): la app usa solo su base Room, sin red, con las **mismas reglas de negocio** del dominio (`ReglasComunicado`, `ValidadorContacto`, `Credenciales`) y el despacho simulado. Login por hash local (misma sal que el servidor), redactar y enviar, historial y canal preferente operan y escriben localmente.
- **Modo servidor**: login, datos y escrituras por la API, con Room como caché. Depende de los `TODO(E3-n)`. **Es el modo que se debe mostrar en la defensa**: el modo local es solo un respaldo si el servicio no está disponible.
- **Cómo cambiar**: en la pantalla de ingreso, interruptor **Modo local (sin servidor)** (el cambio es inmediato; si había una sesión abierta se cierra). Una franja en la parte superior de todas las pantallas indica **MODO LOCAL** o **MODO SERVIDOR**. En modo servidor, si **Probar conexión** (diálogo **Servidor**) falla, el diálogo ofrece **Usar modo local**. En modo local, **Restablecer datos locales** borra lo creado y restaura los datos de ejemplo.
- **Datos independientes**: el modo local usa su propia base (`mpaz_local.db`) y el modo servidor otra (`mpaz_notifica.db`, caché de la API). Lo que se crea en modo local **no se sincroniza** al volver al modo servidor, ni al revés.
- **Qué NO se completa aquí**: activar el modo servidor sin haber implementado los `TODO(E3-n)` produce `NotImplementedError`; el interruptor no los reemplaza. El modo local no depende de ninguno de ellos.

## Mapa de TODO de la Experiencia 3

Buscar todos los huecos con `grep -rn "TODO(E3" app/src` (en Android Studio: búsqueda global de `TODO(E3`). Cada TODO indica qué hacer, en qué archivo y el criterio de aceptación.

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
| E3-P4 | `src/test` y `src/androidTest` | Pruebas esqueleto con `@Ignore("E3: completar")` y la lista de casos |

## Cómo correr las pruebas

```
./gradlew testDebugUnitTest
```

Pruebas JVM (JUnit 4, `kotlinx-coroutines-test`, MockWebServer y MockK) en `app/src/test`:

- Completas (reglas de negocio, no se modifican): selector de modo local/servidor y flujos locales (`app/src/test/.../modo/`), credenciales, reglas de emisión por perfil, validación, adjuntos, firma física, canal preferente, historial y filtros, fechas, despacho simulado e integridad de la base sintética.
- Esqueletos de la capa remota en `app/src/test/.../remote/` (`CapaRemotaTest`, `MapeadoresYServidorTest`, `RepositorioRemotoTest`): están con `@Ignore("E3: completar")` y llevan comentada la lista de casos que deben cubrir.

Prueba de interfaz (Compose UI Test) en `app/src/androidTest` (`LoginUiTest`): `./gradlew connectedDebugAndroidTest` con un emulador o dispositivo conectado.

Pruebas del backend: `cd backend && npm test`.

## Cómo generar el APK firmado

El proyecto no incluye llaves. El build `release` funciona sin firma (APK sin firmar); si se definen estas cuatro propiedades, se firma automáticamente:

| Propiedad | Contenido |
|---|---|
| `MPAZ_STORE_FILE` | Ruta absoluta al keystore (`.jks`) |
| `MPAZ_STORE_PASSWORD` | Clave del keystore |
| `MPAZ_KEY_ALIAS` | Alias de la llave |
| `MPAZ_KEY_PASSWORD` | Clave de la llave |

Se buscan, en este orden, en `keystore.properties` (junto a `gradlew`; copiar `keystore.properties.example`), en `~/.gradle/gradle.properties` y en variables de entorno. Para la evaluación, el `.jks` y su configuración van en el repositorio (llave exclusiva de esta entrega, no reutilizable en otra app); el `.gitignore` excluye solo `local.properties`, `build/` y similares.

1. Crear un keystore propio: `keytool -genkeypair -v -keystore mpaz-release.jks -alias mpaz -keyalg RSA -keysize 2048 -validity 10000`
2. Crear `keystore.properties` a partir del ejemplo con las cuatro propiedades.
3. `./gradlew assembleRelease`. El APK queda en `app/build/outputs/apk/release/`.
4. Verificar la firma con `apksigner verify --verbose --print-certs app-release.apk` (debe informar esquemas v1 y v2).

Subir `versionCode` y `versionName` en `app/build.gradle.kts` en cada entrega. Los pasos detallados y la bitácora de firma están en el enunciado de la actividad.
