# Ferro

Registra el hierro. Mide el progreso.

Ferro es una app de registro de entrenamientos de fuerza para Android. La marca es dura y de hierro, pero limpia: mucho contraste, cifras grandes, ángulos en vez de curvas y un solo color caliente, la **brasa**, que se reserva para lo que importa. Este sistema es la referencia para el código de la app (Kotlin + Jetpack Compose) y para cualquier pieza de marca.

## Principios

1. **Los números mandan.** Peso, reps y tiempo son lo más grande de cada pantalla, en Barlow Condensed con cifras tabulares (`number`, `number-xl`). El resto del texto se aparta.
2. **Una mano y con sudor.** Ningún control por debajo de `touch-min` (48 dp). Las filas de serie miden `row-height` (52 dp). Nada depende de un toque preciso ni de leer texto pequeño.
3. **La brasa se gana.** `brasa` es para la acción principal (una por pantalla), la serie completada, el progreso del descanso y los récords. Nunca decora.
4. **Ángulos, no curvas.** Radios pequeños (`radius-sm` 4, `radius-md` 8, `radius-lg` 12) y ninguna pastilla. El chaflán de 45° es de la marca: el símbolo, el wordmark y la portada; la UI no lo usa.
5. **Oscuro y claro, iguales de buenos.** La app sigue el tema del sistema. Cada token tiene valor en los dos temas y cada par de texto cumple 4.5:1 en ambos.

## Voz

Hablamos como un buen compañero de gimnasio: directo, breve, en tú. Verbos al principio, sin exclamaciones (salvo el récord), sin emojis y sin frases motivacionales vacías. El dato va antes que el adjetivo.

| Situación | Español | English |
| --- | --- | --- |
| Botón principal | Empezar entreno | Start workout |
| Terminar | Terminar | Finish |
| Récord | Nuevo récord: 100 kg × 5 | New PR: 100 kg × 5 |
| Estado vacío | Aún no hay entrenos. Empieza el primero. | No workouts yet. Start your first one. |
| Descanso | Descanso · 1:30 | Rest · 1:30 |
| Error | No se pudo guardar. Tus datos siguen aquí; vuelve a intentarlo. | Couldn't save. Your data is still here; try again. |
| Confirmar borrado | ¿Borrar este entreno? No se puede deshacer. | Delete this workout? This can't be undone. |

Formato: `100 kg`, `225 lb`, `102,5` en español y `102.5` en inglés, `×` entre peso y reps, tiempos `1:30` y `52 min`. Mayúsculas solo en `label` y `display`.

## Logo

El **símbolo** es una placa de hierro achaflanada (`iron`) con una F cuyo extremo superior está al rojo (`brasa`): el hierro que se está forjando. El **wordmark** FERRO está dibujado a medida con el mismo trazo y la misma brasa en la F. El **lockup** junta los dos.

| Archivo | Uso |
| --- | --- |
| `ferro-mark.svg` | Símbolo. Funciona sobre fondos claros y oscuros. Mínimo 24 px. |
| `ferro-lockup-on-dark.svg` / `ferro-lockup-on-light.svg` | Símbolo + nombre: pantalla de bienvenida, Acerca de, tiendas. Mínimo 96 px de ancho. |
| `ferro-wordmark-on-dark.svg` / `ferro-wordmark-on-light.svg` | Solo el nombre, cuando el símbolo ya está presente (por ejemplo, junto al icono). |
| `app-icon.png` | Ficha de Google Play (redúcelo a 512 × 512) y referencia del icono; 1024 × 1024, sin transparencia. |
| `adaptive-icon-foreground.png` | Primer plano del icono adaptativo, con fondo `iron` (#1C1E22). Mejor aún: conviértelo a vector desde `ferro-mark.svg`. |

Espacio de respeto: el lado del cuadrado de brasa por los cuatro lados. No cambies el color de la brasa, no la quites, no gires el símbolo, no lo pongas sobre `brasa` ni sobre fotos con mucho detalle.

## Color

- Superficies en tres escalones: `bg` → `surface` → `surface-raised`. En oscuro la profundidad la da el escalón; en claro, `shadow-sm`.
- Texto: `ink` para datos y títulos, `ink-muted` para etiquetas y la columna Anterior, `ink-subtle` solo para placeholders y objetivos de la rutina.
- Naranja: relleno con `brasa` y texto encima en `on-brasa` (nunca blanco). Para texto o iconos naranjas sobre fondo, `brasa-text`. Para fondos de estado (serie hecha, récord), `brasa-soft`.
- Bordes de controles con `line-strong` (3:1); `line` solo para divisores.
- Tipos de serie: `set-warmup` (W, azul frío), normal (número en `ink-muted`), `set-failure` (F) y `set-drop` (D). Siempre letra + color, nunca solo color.
- `danger` va siempre con icono o texto. En oscuro es más claro que la brasa, pero su tono es cercano: no confíes en el color para distinguirlos.

## Tipografía

Dos familias de Google Fonts con licencia libre (OFL), incluidas en la app como archivos en `res/font`:

- **Barlow Condensed** (600 y 700) para `display`, `title`, `number-xl` y `number`. Es estrecha: caben 4 cifras en la columna de peso.
- **Barlow** (400 y 600) para `heading`, `body`, `body-strong`, `caption` y `label`.

En Compose, los estilos de cifras llevan `fontFeatureSettings = "tnum"` para que no bailen al cambiar. Los tamaños de texto van en sp y respetan la escala de fuente del sistema hasta 2,0; la tabla de series se reorganiza antes que recortar cifras.

## Espaciado y forma

Rejilla de 4. Margen de pantalla `space-4`, separación entre tarjetas de ejercicio `space-5`, entre secciones `space-6`. Columnas de la tabla de series: serie 40, anterior flexible, peso 72, reps 56, casilla 48 (en dp), con `space-2` entre ellas.

## Movimiento y háptica

- Duraciones: 120 ms para pulsaciones y casillas, 200 ms para cambios de estado, 300 ms para hojas y modales. Curva de salida rápida (ease-out); nada rebota.
- Serie completada: la casilla se llena de brasa en 120 ms y vibración ligera.
- Fin de descanso: vibración de notificación y la barra parpadea una vez.
- Récord: vibración de éxito y la cifra entra con una subida corta de 200 ms. Es el único momento con algo de celebración.
- Si el sistema pide reducir el movimiento, solo cambios de opacidad.

## Iconos

Material Symbols en estilo **Sharp** (esquinas rectas, a juego con la marca), importados como vector drawables desde fonts.google.com/icons: `fitness_center`, `timer`, `trophy`, `check`, `add`, `calendar_month`, `bar_chart`, `person`. Tamaños `icon-sm`, `icon-md` e `icon-lg`. Los iconos sin texto llevan `contentDescription`.

## Componentes de referencia

Las fichas de componentes de este sistema muestran el aspecto y las medidas; la implementación real vive en `core.designsystem` de la app. Hay cuatro: Button, SetTypeBadge, SetRow (la fila de la tabla de series) y RestTimer (la barra de descanso).

## Cómo llevarlo a la app

La app es nativa: Kotlin + Jetpack Compose. Los valores en px de `tokens.json` son dp en Android (sp en los tamaños de texto).

| Token | En Compose |
| --- | --- |
| Colores (`surface-raised`, `on-brasa`…) | `FerroColors` en camelCase (`surfaceRaised`, `onBrasa`), una instancia por tema, y un `ColorScheme` de Material 3 derivado |
| Estilos de texto | `FerroTypography`, un `TextStyle` por estilo |
| `space-*`, `radius-*`, `size` | `FerroSpacing`, `FerroRadius` y `FerroSizes` en dp |
| `shadow-sm`, `shadow-lg` | Elevación: ninguna en oscuro, pequeña en claro |

Para el código, descarga `tokens.json` y este README al repositorio como `docs/design/tokens.json` y `docs/design/DESIGN.md`, copia las fuentes a `res/font` y lanza el prompt 2 del plan de desarrollo, que lo implementa todo en `core.designsystem` con su icono adaptativo y su splash.
