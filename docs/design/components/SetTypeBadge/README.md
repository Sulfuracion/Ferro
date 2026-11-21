# SetTypeBadge

Indica el tipo de serie en la primera columna de la tabla: número para la normal y letra para las especiales.

- Normal: el número de serie (1, 2, 3…) en `ink-muted`. Las series especiales no consumen número.
- Calentamiento: **W** en `set-warmup`. No cuenta para récords ni volumen.
- Fallo: **F** en `set-failure`.
- Drop set: **D** en `set-drop`.

Cuadrado de 28 sobre `surface-raised`, radio `radius-sm`, Barlow Condensed 600. Al tocarlo se abre una hoja para cambiar el tipo; el objetivo táctil es la celda entera (48 dp). Lee en voz alta "Serie 2", "Calentamiento", "Al fallo", "Drop set".
