# SetRow

Una serie dentro de la tarjeta de ejercicio del entreno activo: tipo, lo que hiciste la última vez, peso, reps y casilla de hecho.

Columnas (dp): serie 40 · anterior flexible · peso 72 · reps 56 · casilla 48, con `space-2` entre ellas; alto `row-height` (52). Cabecera en estilo `label`.

Estados:
- **Pendiente**: campos sobre `surface-raised` con borde `line-strong`; si la rutina tiene objetivo, se muestra en `ink-subtle` como placeholder.
- **Editando**: borde de 2 px en `focus`. El teclado numérico trae "Siguiente" para saltar a reps y a la serie siguiente.
- **Hecha**: la fila pasa a `brasa-soft`, los campos pierden el borde y la casilla se llena de `brasa` con check en `on-brasa`. Vibración ligera y arranca el descanso.
- **Récord**: etiqueta "RÉCORD" con trofeo en `brasa-text` bajo la fila al terminar.

Tocar "Anterior" copia esos valores. Deslizar a la izquierda borra la serie. El consumidor aporta los valores ya convertidos a la unidad del usuario y formateados (coma decimal en español).
