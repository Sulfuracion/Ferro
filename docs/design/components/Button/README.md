# Button

Acción en una línea, con verbo primero: "Empezar entreno", "Añadir serie", "Terminar".

- **primary** (`brasa` con texto `on-brasa`): una por pantalla, para lo que la pantalla existe. Pulsado: `brasa-pressed`.
- **secondary** (borde `line-strong`, texto `ink`): acciones de apoyo, como "Añadir ejercicio".
- **ghost** (texto `brasa-text`): acciones de menor peso dentro de tarjetas, como "+ Añadir serie".
- **danger** (texto `danger`, borde `line-strong`): "Descartar entreno", "Borrar". Siempre pide confirmación.

Alto `button-height` (48), radio `radius-md`, texto `body-strong`. Icono opcional a la izquierda en `icon-md`. Foco: anillo de 2 px en `focus` separado 2 px. Desactivado: opacidad 40 % y sin háptica. El consumidor aporta el texto traducido y la `contentDescription` si solo hay icono.
