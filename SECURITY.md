# Seguridad

## Qué datos maneja

- **Token de acceso de Canvas.** Da acceso completo a tu cuenta de Canvas. Usa un token propio para el teléfono, para poder revocarlo sin afectar otros dispositivos.
- **Tus pendientes y notas.** Se guardan en caché en el almacenamiento privado de la app para mostrarlos al instante.

## Dónde se guarda el token

- Cifrado con AES-GCM. La clave vive en el Android Keystore y no se puede exportar.
- El respaldo en la nube y la transferencia entre dispositivos están desactivados (`allowBackup="false"` y reglas de extracción).

## A dónde se conecta

- Solo al dominio de Canvas que configures, por https.
- No sigue redirecciones automáticamente, para que el token no termine en otro dominio.
- No hay servidores propios, analítica ni publicidad.
- Para comprobarlo, un firewall como NetGuard o RethinkDNS muestra los dominios que usa la app.

## Componentes expuestos

- Solo la pantalla principal y el receptor del widget, que el sistema necesita para actualizarlo.
- Los enlaces a tareas se abren en Canvas Student si está instalada; si no, en el navegador.

## Verificar una descarga

Compara el hash del APK con `SHA256SUMS.txt` de la misma release. Las releases se firman siempre con la misma clave: si Android dice que la firma no coincide con la app instalada, no instales ese APK.

## Cadena de suministro

- Las acciones de GitHub están fijadas por hash de commit.
- `setup-gradle` valida el Gradle wrapper.
- Dependabot avisa de dependencias con versiones nuevas o vulnerables.

## Reportar una vulnerabilidad

Usa **Security → Report a vulnerability** en este repositorio. No abras un issue público ni incluyas tokens reales.
