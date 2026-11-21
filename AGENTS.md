# AGENTS.md — Reglas para trabajar en Ferro

- Lee [`docs/SPEC.md`](docs/SPEC.md) antes de cada tarea. Si una petición contradice la SPEC, pregunta antes.
- Antes de tocar UI, lee `docs/design/DESIGN.md`. Solo `FerroTheme`; `brasa` solo para la acción principal, series hechas y récords; texto sobre `brasa` en `onBrasa`.
- Kotlin idiomático; nada de `!!` salvo justificado. Composables sin lógica de negocio; el estado vive en el ViewModel.
- Ningún texto visible escrito en el código: todo en `res/values/strings.xml` (es) y `res/values-en/strings.xml` (en), con plurals donde toque.
- Ningún color, tamaño o tipografía literal en pantallas: solo el tema Ferro de `core.designsystem`.
- Pesos en kg en la base de datos; conversión en `core.domain` (`Units.kt`).
- IDs UUID como `String`. Fechas como epoch en milisegundos (`Long`, UTC); `java.time` para mostrarlas.
- La lógica de cálculo va en `core.domain` como funciones puras con tests unitarios. El tiempo se inyecta (`Clock`) para poder testearlo.
- Acceso a datos solo a través de repositorios; los DAO no salen de `core.data`.
- Cada cambio de esquema de Room lleva su migración y su test; nunca `fallbackToDestructiveMigration`.
- Consulta developer.android.com para APIs y versiones; no inventes APIs.
- No añadas dependencias sin justificarlo.
- Al terminar cada tarea: `./gradlew check assembleDebug` en verde, y resume qué archivos cambiaste y cómo probarlo.
