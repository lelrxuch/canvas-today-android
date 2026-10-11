# Canvas Today para Android

[![CI](https://github.com/lelrxuch/canvas-today-android/actions/workflows/ci.yml/badge.svg)](https://github.com/lelrxuch/canvas-today-android/actions/workflows/ci.yml)

Tus pendientes de Canvas en el teléfono: una lista que abre al instante, un widget de pantalla de inicio y
avisos que no dependen de las notificaciones de Canvas. Versión Android de
[Canvas Today](https://github.com/lelrxuch/canvas-today).

## Qué hace

- **Lista limpia:** solo lo que vence en los próximos días, agrupado en Atrasadas, Hoy, Mañana, Esta
  semana y Más adelante. Los cursos de ciclos anteriores no aparecen.
- **Abre al instante:** muestra la última lista guardada y actualiza en segundo plano.
- **Avisos propios:** cada 15 minutos baja tus pendientes y programa un aviso en el teléfono antes de
  cada entrega (1 a 24 h, configurable).
- **Widget:** atrasadas, hoy y mañana; tocar una tarea la abre en Canvas.
- **Notas personales:** se guardan en el planner de Canvas, así que también aparecen en Canvas Today de
  escritorio y en la web.

## Instalar

Descarga el APK de [Releases](https://github.com/lelrxuch/canvas-today-android/releases), ábrelo en el
teléfono y permite la instalación desde esa fuente cuando Android lo pida. Requiere Android 10 o superior.

La primera vez pide la dirección de tu Canvas y un token de acceso
(Canvas → Cuenta → Configuraciones → + Nuevo token de acceso).

### Si los avisos llegan tarde

Samsung, Xiaomi y otras marcas detienen las apps en segundo plano para ahorrar batería. En
**Ajustes → Avisos puntuales → Abrir ajustes de batería**, marca Canvas Today como "Sin restricciones".

## Privacidad

- El token se cifra con AES-GCM usando una clave del Android Keystore, que no sale del teléfono.
- La app solo se conecta al dominio de Canvas que configures, por https.
- No hay servidores propios, analítica ni respaldo en la nube de los datos de la app.

## Desarrollo

Requiere JDK 17 y el SDK de Android (Android Studio lo instala).

```bash
./gradlew testDebugUnitTest   # pruebas de la lógica (JVM)
./gradlew assembleDebug       # APK en app/build/outputs/apk/debug/
```

Estructura:

- `core/`: reglas del planner, fechas relativas y validación. Kotlin puro, probado en la JVM.
- `data/`: cliente de la API de Canvas, token cifrado, ajustes y caché.
- `work/`: sincronización periódica y avisos (WorkManager).
- `widget/`: widget de pantalla de inicio (Jetpack Glance).
- `ui/`: pantallas en Jetpack Compose.

### Firma de releases

`release.yml` firma el APK con una clave guardada en los secretos del repositorio: `KEYSTORE_BASE64`,
`KEYSTORE_PASSWORD`, `KEY_ALIAS` y `KEY_PASSWORD`. Para crearla:

```bash
keytool -genkeypair -v -keystore release.jks -alias canvas-today -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 release.jks   # valor de KEYSTORE_BASE64
```

Guarda `release.jks` fuera del repositorio: sin esa clave, las versiones nuevas no se pueden instalar encima
de las anteriores.

## Aviso

Proyecto personal, sin relación con Instructure. Canvas es una marca de Instructure, Inc.

## Licencia

[MIT](LICENSE)
