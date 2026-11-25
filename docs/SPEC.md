# Ferro — Especificación v1

## Producto

App nativa Android de registro de entrenamientos de gimnasio, **offline-first**: todos los datos viven en el dispositivo.

- **Idiomas:** español y English. Por defecto, el del sistema, con selector de idioma por app en Ajustes (`locales_config`).
- **Unidades kg/lb:** el peso se guarda **siempre en kg** y se convierte solo al mostrarlo o al introducirlo.
- **Tema:** claro/oscuro según el sistema, con opción manual.
- **Marca Ferro:** la guía y los tokens de diseño están en [`docs/design/`](design/README.md).

## Navegación

Barra inferior con cuatro pestañas: **Entrenar**, **Rutinas**, **Progreso**, **Perfil**. Cada pestaña conserva su pila y su desplazamiento al cambiar entre ellas (`saveState` / `restoreState`).

La **biblioteca de ejercicios** se abre desde Rutinas, desde el entreno activo y desde Perfil.

Cuando hay un entreno activo minimizado, se muestra una barra mini sobre la barra inferior (oculta por defecto hasta activar el flujo de entreno en segundo plano).

### Mapa de rutas

Todas las rutas son objetos o clases `@Serializable` bajo `com.davidguijo.ferro`:

| Ruta | Paquete | Barra inferior | Parámetros | Pantalla |
| --- | --- | --- | --- | --- |
| `TrainRoute` | `feature.workout` | Sí | — | Entrenar |
| `RoutinesRoute` | `feature.routines` | Sí | — | Rutinas |
| `ProgressRoute` | `feature.progress` | Sí | — | Progreso |
| `ProfileRoute` | `feature.settings` | Sí | — | Perfil |
| `ActiveWorkoutRoute` | `feature.workout` | No | `routineId: String? = null` | Entreno activo (pantalla completa) |
| `WorkoutSummaryRoute` | `feature.workout` | No | `workoutId: String` | Resumen de entreno |
| `RoutineEditorRoute` | `feature.routines` | No | `routineId: String? = null` | Editor de rutina |
| `ExerciseLibraryRoute` | `feature.exercises` | No | — | Biblioteca de ejercicios |
| `ExerciseDetailRoute` | `feature.exercises` | No | `exerciseId: String` | Detalle de ejercicio |
| `HistoryRoute` | `feature.history` | No | — | Historial |
| `SessionDetailRoute` | `feature.history` | No | `workoutId: String` | Detalle de sesión |
| `CalendarRoute` | `feature.history` | No | — | Calendario |
| `BodyMeasurementsRoute` | `feature.body` | No | — | Medidas |
| `SettingsRoute` | `feature.settings` | No | — | Ajustes |
| `BackupRoute` | `feature.backup` | No | — | Copia de seguridad |

## Funciones v1

### Biblioteca de ejercicios
- Unos 120 ejercicios semilla: nombre (es/en), músculo principal, músculos secundarios, equipo, tipo de registro e instrucciones breves propias.
- Búsqueda; filtros por músculo y por equipo.
- Ejercicios personalizados.

### Tipos de registro
- Peso + reps
- Peso corporal + reps
- Peso corporal lastrado
- Peso corporal asistido
- Duración
- Distancia + duración

### Rutinas
- Crear, editar, duplicar y borrar; carpetas.
- Ordenar ejercicios; superseries.
- Notas por rutina y por ejercicio.
- Series objetivo (tipo, peso, rango de reps); descanso por ejercicio.
- Plantillas predefinidas: Full Body 3 días, Torso/Pierna 4 días, Push/Pull/Legs 6 días, Fuerza 5x5.

### Entreno activo
- Empezar vacío o desde una rutina.
- Añadir, quitar y reordenar ejercicios y series.
- Tipos de serie: calentamiento (W), normal, fallo (F), drop set (D).
- Peso, reps y RPE opcional; columna «Anterior» con lo de la última sesión.
- Marcar serie hecha.
- Temporizador de descanso automático con aviso y vibración aunque la app esté en segundo plano.
- Notificación persistente mientras hay un entreno en curso.
- Cronómetro de sesión; pantalla encendida.
- Minimizar y reanudar.
- El entreno en curso se guarda en la base de datos **en cada cambio**, para no perderlo si el sistema cierra la app.
- Terminar con resumen; descartar con confirmación.

#### Aviso de fin de descanso en segundo plano y pantalla apagada
- **Decisión adoptada:**
  1. **Servicio en primer plano (`ActiveWorkoutService`)** con `android:foregroundServiceType="specialUse"` y `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" ... />`. Mantiene la notificación persistente en curso (`setUsesChronometer(true)` para la sesión, `setChronometerCountDown(true)` cuando hay descanso, acciones `+15 s` y `Saltar`), conserva el proceso en el bucket `ACTIVE` y vigila el fin del descanso mientras la CPU está despierta.
  2. **`AlarmManager` (`RTC_WAKEUP`) + `RestTimerAlarmReceiver`** para despertar el dispositivo con pantalla apagada o reposo: usa `setExactAndAllowWhileIdle` en API 26–30 y en API 31+ cuando `alarmManager.canScheduleExactAlarms()` es `true` (permiso opcional `SCHEDULE_EXACT_ALARM`, con acceso directo no bloqueante a *Alarmas y recordatorios* si falta), degradando de forma segura a `setAndAllowWhileIdle` si el usuario no lo concede (sin `SecurityException`).
  3. **Vibración y aviso (`RestTimerAlertNotifier`)**: vibración directa vía `VibratorManager`/`Vibrator` con `VibrationAttributes.USAGE_ALARM` (API 33+) o `AudioAttributes.USAGE_ALARM` (API 26–32), lo que permite vibrar en segundo plano, pantalla apagada y ahorro de batería respetando `timerVibrationEnabled`, más notificación `CATEGORY_ALARM` de alta prioridad en canales separados (`ferro_rest_finished_sound` y `ferro_rest_finished_silent`) según `timerSoundEnabled`.
- **Opciones descartadas por políticas de Android 14+ y Google Play:**
  - `foregroundServiceType="health"`: requiere prerrequisitos de sensores o actividad (`ACTIVITY_RECOGNITION`, `BODY_SENSORS`/`READ_HEART_RATE` o `HIGH_SAMPLING_RATE_SENSORS`); Ferro no lee sensores biométricos ni pasos, por lo que pedir permisos sin uso incumple Play y lanza `SecurityException` si el usuario los deniega.
  - `foregroundServiceType="shortService"`: límite estricto de ~3 minutos sin renovación en segundo plano, incompatible con entrenos de fuerza completos y descansos de hasta 10 minutos.
  - `USE_EXACT_ALARM`: restringido por Google Play únicamente a apps cuya función principal sea despertador, temporizador dedicado o calendario; prohibido para diarios de entrenamiento.
  - `WorkManager` / solo corrutinas: diferibles por Doze o pausados al suspender la CPU con la pantalla apagada.
- **Límites documentados:**
  - Si en Android 13+ se deniega `POST_NOTIFICATIONS`, no se muestran la notificación persistente ni la alerta en segundo plano (el temporizador en pantalla sigue funcionando).
  - Si en Android 13+ no se concede `SCHEDULE_EXACT_ALARM`, `setAndAllowWhileIdle` es inexacto y puede retrasarse en Deep Doze o ahorro de batería agresivo.
  - En Deep Doze, Android limita las alarmas `AllowWhileIdle` a ~1 cada 9 minutos (infrecuente durante un entreno activo con uso frecuente y servicio en primer plano, posible tras largos periodos inactivos).
  - Si el usuario o un gestor agresivo del fabricante fuerza la detención del proceso, el servicio y la alarma pueden cancelarse, pero el entreno en Room y la marca de fin del descanso en DataStore se recuperan intactos al reabrir la app.
  - Para publicar en Google Play con `specialUse`, es obligatorio declarar el servicio en primer plano en Play Console con descripción y vídeo demostrativo del flujo de entreno y cuenta atrás con pantalla bloqueada.

### Récords
- Peso máximo, 1RM estimado (Epley), mejor volumen de una serie (peso × reps), más reps a un peso dado.
- Se detectan al terminar el entreno y se celebran en el resumen.
- Las series de calentamiento no cuentan.

### Historial
- Lista de sesiones y detalle.
- Editar y borrar sesiones pasadas.
- Calendario mensual; racha semanal.

### Progreso
- Gráficas por ejercicio: 1RM estimado, peso máximo, volumen.
- Volumen semanal.
- Series por grupo muscular.
- Recuperación por grupo muscular (días desde el último entreno de cada grupo).

### Medidas corporales
- Peso (en `kg` en base de datos, mostrado en `kg`/`lb`), % de grasa y perímetros (en `cm` en base de datos, mostrados en `cm` o `in` si la unidad de peso es `lb`).
- Fotos de progreso copiadas al almacenamiento privado (`filesDir/photos`), con pose (`frente`, `lado`, `espalda`), galería por fecha y comparador lado a lado.
- Gráficas por medida con selector de rango.
- **Volumen con peso corporal:** el peso corporal más reciente registrado se usa para calcular el volumen de los ejercicios de peso corporal lastrado (`(pesoCorporal + lastre) × reps`; si no hay peso corporal registrado, se usa solo `lastre × reps` cuando `lastre > 0`) y asistido (`max(0, pesoCorporal - asistencia) × reps`; sin peso corporal registrado no suma volumen).

### Ajustes
- Idioma, unidades, tema, descanso por defecto, inicio de semana.

### Copia de seguridad
- Exportar e importar JSON completo.
- Exportar CSV de entrenos.

### Fuera de la v1
Cuentas, nube, red social, Health Connect, Wear OS, widgets.

## Arquitectura

- Una sola Activity con Jetpack Compose y un único `NavHost` con rutas tipadas (kotlinx.serialization).
- **MVVM con flujo unidireccional:** cada pantalla tiene un ViewModel que expone un `StateFlow<UiState>` inmutable y recibe eventos. La UI no conoce la base de datos.
- **Capas:**
  - `data`: Room, DataStore, repositorios.
  - `domain`: modelos y cálculos puros.
  - `ui`: pantallas y componentes.
- Inyección de dependencias con Hilt.
- Corrutinas y Flow; nada de LiveData ni RxJava.

## Paquetes

Todos bajo `com.davidguijo.ferro`:

| Paquete | Contenido |
| --- | --- |
| `core.designsystem` | Tema y componentes |
| `core.data` | Base de datos, DAO, repositorios, DataStore |
| `core.domain` | Modelos y cálculos: unidades, 1RM, récords, volumen, fechas |
| `feature.exercises` | Biblioteca de ejercicios |
| `feature.routines` | Rutinas y carpetas |
| `feature.workout` | Entreno activo |
| `feature.history` | Historial |
| `feature.progress` | Progreso y gráficas |
| `feature.body` | Medidas corporales |
| `feature.settings` | Ajustes |
| `feature.backup` | Copia de seguridad |
| `navigation` | NavHost y rutas |

## Stack técnico

Versiones en `gradle/libs.versions.toml`. Kotlin integrado de AGP 9, KSP, Compose (BOM), Material 3, Hilt, Room 3 (`androidx.room3`, esquemas en `app/schemas`), DataStore Preferences, Navigation Compose, Lifecycle ViewModel Compose, kotlinx-coroutines, kotlinx-serialization-json, Paging 3 (`room3-paging`), Vico, Coil 3. Tests: JUnit 4, kotlinx-coroutines-test, Turbine, room3-testing. Calidad: ktlint y Android Lint enganchados a `./gradlew check`.

Configuración: `minSdk 26`, `compileSdk 37.2`, `targetSdk 37`, Java 21 (toolchain), R8 en release.
