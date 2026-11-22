# StageLite

Mod de **luces de escenario simples** para **Minecraft Forge 1.20.1**, pensado para que lo maneje el **staff** del servidor. No tiene recetas: las luces se obtienen desde la pestaña creativa o con `/give`.

Sin DMX ni software externo: color, brillo, efectos, movimiento y programación básica de shows, con un panel de faders como una mesa de luces.

> **Estado: primera versión (0.9.3). El código nunca se ha compilado ni probado en el juego** (se escribió sin acceso a los repositorios de Forge). Es posible que haya que corregir algún error de compilación o detalle visual. Ver "Primer arranque".

## Obtener el .jar (sin instalar nada)

1. Crea un repositorio en GitHub y sube todo el contenido de esta carpeta.
2. Ve a la pestaña **Actions**. El workflow **Build StageLite** se ejecuta solo (o pulsa *Run workflow*).
3. Cuando termine (unos minutos), entra en la ejecución y descarga el artefacto **stagelite-jar**: dentro está `stagelite-0.9.3.jar`.
4. Pon ese `.jar` en la carpeta `mods` del servidor **y** de cada jugador (hace falta en ambos lados). Requiere Forge 1.20.1 (47.x).

Si el workflow falla, abre el paso "Compilar": el mensaje dice qué línea corregir. Se puede pegar a Claude Code para que lo arregle.

## Las luces

| Bloque | Qué es |
|---|---|
| **Foco (Par)** `stagelite:spot_light` | Foco en horquilla, haz medio. |
| **Cabeza móvil** `stagelite:moving_head` | La cabeza **gira e inclina de verdad** (horquilla + cabeza animadas). Por defecto hace un barrido. |
| **Panel LED** `stagelite:led_panel` | Luz ancha tipo wash. |
| **Estroboscopio** `stagelite:strobe_light` | Barra de estrobo (viene con efecto estrobo). |

Se colocan mirando hacia la cara donde haces clic (en el techo apuntan hacia abajo). Son **indestructibles en supervivencia**: solo se quitan en creativo. Los jugadores sin permiso no pueden cambiarlas.


## Dispositivos nuevos (0.9.3)

Todos se colocan, se abren con clic derecho (solo staff) y se pueden meter en **grupos** (1-99), así que funcionan con la consola, las escenas, los shows y `/stagelite`.

| Bloque | Qué hace | Faders propios |
|---|---|---|
| **Láser** `laser` | Abanico de rayos finos que giran/barren (usa Movimiento) | Rayos |
| **Máquina de humo** `smoke_machine` | Echa humo (partículas). Con color distinto de blanco, humo de color. Efecto *estrobo* = a ráfagas | Alcance (Brillo = cantidad) |
| **Máquina de fuego** `fire_machine` | Chorro de llamas **solo visual** (no quema ni daña) | Altura |
| **Lanzador de fuegos artificiales** `firework_launcher` | Dispara cohetes reales de su color (*arcoíris* = colores al azar) | Altura (Vel. = ritmo) |
| **Bola disco** `disco_ball` | Bola de espejos que gira con rayos de colores | Rayos |
| **Holograma** `hologram` | Muestra una **imagen desde un link web** flotando | Tamaño, Giro, casilla del link |
| **Consola de luces** `light_console` | Un canal por cada grupo: ON/OFF, 10 colores, Brillo, Vel., Efecto, Movimiento; además Apagón, escenas y shows | — |

### Holograma
- **Imagen del computador:** pulsa *Elegir archivo...*, arrastra la imagen a la ventana del panel, o escribe la ruta y pulsa Enter. Se sube al servidor (carpeta `stagelite_images` del mundo, máx. `maxImageKB` = 2048 KB por defecto) y todos los jugadores la ven; cada cliente la guarda en `stagelite_cache`.
- Pega en la casilla un link **directo a una imagen** (PNG, JPG, BMP o **GIF animado**, máx. 8 MB; los GIF muy grandes se reducen de tamaño/cuadros para ahorrar memoria). Un link de YouTube o de una página web **no funciona**; tampoco hay video.
- Cada jugador descarga la imagen desde su propio cliente: el sitio de la imagen ve la IP de los jugadores. Se bloquean direcciones privadas/locales. Quien no quiera descargar nada: `hologramsEnabled = false` en `stagelite-client.toml`.
- *Mira al jugador* = siempre de frente; *Fijo* = queda en una dirección (fader Giro).

### Consola
Clic derecho (staff). Muestra los grupos que tengan luces **cargadas**. **Haz clic en el título de un canal (`G1`) para ponerle un nombre** (Escenario, Pista...; también `/stagelite group <n> name <texto>`). Pasa el ratón sobre el título para ver qué dispositivos hay en el grupo. Las escenas y shows son los mismos de los comandos. Las escenas guardan encendido, color, brillo, efecto, movimiento y velocidad (no cambian el link, tamaño ni otros ajustes propios de cada dispositivo).

### Notas
- Al inclinar una luz fija, el modelo del bloque se inclina junto con el haz.
- El láser no se corta con bloques (atraviesa paredes); los rayos de la bola disco tampoco. Los rayos de la bola siempre se dibujan de frente a la cámara.
- Los fuegos artificiales son cohetes de Minecraft: su explosión hace daño cerca. Se pueden desactivar con `fireworksEnabled = false` en `stagelite-common.toml`.
- La versión del protocolo de red cambió: servidor y jugadores deben tener la misma versión del mod.

## Pista de baile
Bloque `stagelite:dance_floor`: cada baldosa tiene luz propia. Las baldosas pegadas (por los 4 lados y del mismo grupo) forman **una sola pista**: al colocar una nueva copia el estilo de su vecina, y al editar cualquiera (clic derecho) cambia toda la pista conectada. Para separar una zona, ponle otro número de grupo a una baldosa (solo cambia esa).
Efectos de pista (botón *Efecto*): Estático, Pulso, Estrobo, Arcoíris y los patrones **Ola, Ondas, Ajedrez, Espiral, Rayas y Destellos**, que usan la posición de cada baldosa para dibujar figuras que cruzan toda la pista. Funciona con grupos, la consola, escenas y comandos como el resto de luces.

## Molinete 13x13
Bloque decorativo `stagelite:molinete` (pestaña creativa). Al colocarlo ocupa **13x13 bloques y 5 de alto** de una vez (193 piezas; el bloque donde haces clic es el centro de la base y necesita el área libre). Solo se quita en creativo: al romper una pieza se quitan todas. Es un modelo estático; solo la base tiene colisión.

## El panel de faders

Clic derecho sobre una luz (solo staff):

- **Colores rápidos:** 10 cuadritos arriba, un clic y listo.
- **Faders verticales:** R, V, A (color), Brillo, Ancho y Largo del haz, Velocidad (y Mov. en la cabeza móvil). Arrastra, o usa la **rueda del ratón** (Shift = pasos de 5).
- **Botones:** Efecto (fija, respiración, estrobo, arcoíris), Movimiento (cabeza móvil), Encendida/Apagada y **Grupo** (- / +).
- Pasa el ratón por cualquier control para ver una explicación.
- Los cambios se ven **en vivo** mientras arrastras.

## Comandos (`/stagelite`, solo staff)

Todos admiten TAB para autocompletar. Las luces del mismo **grupo** (1-99) se controlan juntas.

| Comando | Qué hace |
|---|---|
| `/stagelite list` | Cuántas luces hay cargadas y por grupo |
| `/stagelite blackout` / `on` | Apaga / enciende todas |
| `/stagelite group <n> on` / `off` | Enciende o apaga un grupo |
| `/stagelite group <n> color <nombre\|hex>` | `red`, `blue`, `pink`... o `ff8800` (sin `#`) |
| `/stagelite group <n> rgb <r> <g> <b>` | Color por componentes 0-255 |
| `/stagelite group <n> intensity <0-100>` | Brillo |
| `/stagelite group <n> effect <static\|pulse\|strobe\|rainbow>` | Efecto |
| `/stagelite group <n> move <none\|sweep\|nod\|circle\|eight\|spin>` | Movimiento automático (todas las luces con haz y el láser; `spin` = giro continuo) |
| `/stagelite group <n> speed <1-10>` | Velocidad de los efectos |
| `/stagelite group <n> movespeed <1-10>` | Velocidad del movimiento / giro |
| `/stagelite group <n> yaw <-90..90>` | Giro fijo hacia un lado (junto con tilt permite apuntar a cualquier dirección) |
| `/stagelite group <n> tilt <-90..90>` | Inclinación fija (+ arriba, - abajo) de focos, láser, paneles y estrobos |
| `/stagelite scene save\|load\|delete <nombre>` / `scene list` | Escenas (el look de todos los grupos) |
| `/stagelite show create <nombre>` | Crea un show vacío |
| `/stagelite show add <show> <escena> <segundos>` | Añade un paso |
| `/stagelite show play <nombre>` / `loop <nombre>` | Reproduce (una vez o en bucle) |
| `/stagelite show stop` / `list` / `delete <nombre>` | Detiene, lista o borra |

### Ejemplo de show

```
/stagelite group 1 color red
/stagelite group 1 effect pulse
/stagelite scene save intro

/stagelite group 1 color blue
/stagelite group 1 move sweep
/stagelite scene save estribillo

/stagelite show create concierto
/stagelite show add concierto intro 20
/stagelite show add concierto estribillo 30
/stagelite show loop concierto
```

Escenas y shows se guardan con el mundo. Solo afectan a luces **en chunks cargados**.

## Cómo se usa (staff)

1. Dale OP a tu staff (nivel 2; configurable en `stagelite-common.toml` → `permissionLevel`).
2. Consigue las luces: pestaña creativa **StageLite**, o por ejemplo `/give @s stagelite:moving_head`.
3. Colócalas, abre el panel con clic derecho y ponles un **grupo** si quieres controlarlas por comandos.

## Compilar en tu ordenador (opcional)

Requisitos: **JDK 17**. El wrapper de Gradle (`gradlew`) no está incluido: abre la carpeta en IntelliJ como proyecto Gradle, o copia `gradlew`, `gradlew.bat` y `gradle/` del MDK oficial de Forge 1.20.1.

```
./gradlew build        # genera build/libs/stagelite-0.9.3.jar
./gradlew runClient    # abre Minecraft con el mod para probar
./gradlew runServer    # servidor de pruebas
```

## Primer arranque: qué revisar

Si algo falla, estos son los puntos más probables (no se pudo verificar nada en el juego):

1. **Errores de compilación** por nombres de clases/métodos de Forge 1.20.1.
2. **Cabeza móvil:** que horquilla y cabeza se vean bien y giren (`StageLightRenderer`, métodos `drawYoke` / `drawHead`).
3. **Haz de luz:** usa `RenderType.lightning()` (aditivo, sin textura). Si se ve raro, mirar `drawBeam`.
4. **Orientación** según la cara donde se coloca (`FACING_ROT` y los `blockstates`).
5. **Panel:** posición de los faders y de los botones según el tamaño de pantalla (`StageLightScreen`).
6. **Shaders:** probar con y sin shaders (Oculus/Embeddium).

## Configuración

- `stagelite-common.toml` (servidor): `permissionLevel` (por defecto 2).
- `stagelite-client.toml` (cada jugador): `beamRenderDistance` (96) y `beamSides` (10; bajar para más FPS).

## Estructura

```
com.stagelite
  StageLite                 entrada del mod
  block/StageLightBlock     bloque (uno por tipo de luz)
  blockentity/              estado de cada luz (guardado y sincronizado)
  light/                    FixtureType, LightData, efectos, movimiento, registro de luces
  data/                     escenas y shows (guardado con el mundo) y reproductor
  command/                  /stagelite
  network/                  paquetes abrir panel / aplicar cambios
  client/                   panel de faders y renderer (haz + cabeza móvil)
  config/                   configuración
```

## Ideas para después

- Control por redstone.
- Efectos (persecución entre luces de un grupo, sincronizada a BPM).
- Pantalla para armar shows sin comandos.
- Texturas más trabajadas (las actuales son provisionales, generadas por código).

## Licencia

Sin definir (`All Rights Reserved` en `gradle.properties`). Cámbiala antes de publicar el repositorio.
