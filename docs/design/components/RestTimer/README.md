# RestTimer

Barra fija sobre el borde inferior del entreno activo que cuenta el descanso tras marcar una serie.

Muestra la etiqueta "Descanso", el tiempo restante en `number-xl`, una pista de 4 px en `line` que se vacía en `brasa`, y tres acciones: −15 s, +15 s (secondary) y Saltar (ghost). Fondo `surface-raised`, esquinas superiores `radius-lg`, sombra `shadow-lg`.

El tiempo se calcula desde una marca de fin, no con intervalos, para ser exacto al volver de segundo plano. Al llegar a cero: vibración de notificación, un parpadeo y se oculta; con la app cerrada avisa una notificación local. Con el entreno minimizado, el tiempo pasa a la barra mini sobre las pestañas.
