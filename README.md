# StageLite

Mod de **luces de escenario simples** para **Minecraft Forge 1.20.1**, pensado para que lo maneje el **staff** del servidor. No tiene recetas: las luces se obtienen desde la pestaña creativa o con `/give`.

Sin DMX ni software externo: color, brillo, efectos, movimiento y programación básica de shows, con un panel de faders como una mesa de luces.

> **Estado: primera versión (0.1.0). El código nunca se ha compilado ni probado en el juego** (se escribió sin acceso a los repositorios de Forge). Es posible que haya que corregir algún error de compilación o detalle visual. Ver "Primer arranque".

## Obtener el .jar (sin instalar nada)

1. Crea un repositorio en GitHub y sube todo el contenido de esta carpeta.
2. Ve a la pestaña **Actions**. El workflow **Build StageLite** se ejecuta solo (o pulsa *Run workflow*).
3. Cuando termine (unos minutos), entra en la ejecución y descarga el artefacto **stagelite-jar**: dentro está `stagelite-0.1.0.jar`.
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
| `/stagelite group <n> move <none\|sweep\|nod\|circle\|eight>` | Movimiento (solo cabezas móviles) |
| `/stagelite group <n> speed <1-10>` | Velocidad |
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
./gradlew build        # genera build/libs/stagelite-0.1.0.jar
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
- Más tipos de luz (láser, bola de espejos) y efectos (persecución entre luces de un grupo, sincronizada a BPM).
- Pantalla para armar shows sin comandos.
- Texturas más trabajadas (las actuales son provisionales, generadas por código).

## Licencia

Sin definir (`All Rights Reserved` en `gradle.properties`). Cámbiala antes de publicar el repositorio.
