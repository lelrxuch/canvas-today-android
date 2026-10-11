# Cambios

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y
[Versionado Semántico](https://semver.org/lang/es/).

## [Sin publicar]

## [0.1.1]

### Cambiado

- Modo oscuro con la paleta de iOS: fondo negro, celdas en gris oscuro, texto secundario translúcido y azul de
  sistema como acento. Antes usaba los colores dinámicos, que en oscuro quedaban grises y sin contraste.
- Barra de pestañas sin la píldora gris: la pestaña activa se marca en color. Botón "Nota" con el color de
  acento.

## [0.1.0]

### Agregado

- Pestañas Inicio, Tareas y Notas en la barra inferior, como en Canvas Today de escritorio. Inicio muestra
  lo atrasado, lo de hoy y lo de mañana, con un enlace al resto de tareas.
- Lista de pendientes del planner de Canvas agrupada en Atrasadas, Hoy, Mañana, Esta semana y Más adelante,
  con las mismas reglas que Canvas Today de escritorio.
- Caché local: la lista y el widget se muestran al instante y se actualizan por detrás.
- Sincronización cada 15 minutos con WorkManager.
- Avisos programados en el teléfono antes de cada vencimiento (1, 3, 6, 12 o 24 h), sin depender de las
  notificaciones de Canvas.
- Widget de pantalla de inicio con lo atrasado, lo de hoy y lo de mañana.
- Notas personales: se crean y eliminan desde la app y se guardan en el planner de Canvas.
- "Abrir en Canvas", el widget y los avisos abren la tarea en Canvas Student si está instalada (ya tiene la
  sesión iniciada); si no, en el navegador.
- Token cifrado con una clave del Android Keystore.
- Interfaz en español e inglés, colores del sistema (Material You).
