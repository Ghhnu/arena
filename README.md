# Arena Mod (Fabric · Minecraft Java 1.21.1)

Mod que añade el comando `/arena` para crear una arena de combate por
oleadas, compatible con **singleplayer y servidores dedicados** (todo
el código corre en el lado del servidor).

## Comandos

| Comando | Qué hace |
|---|---|
| `/arena <x> <y> <z>` | Construye una arena nueva centrada en esas coordenadas (requiere nivel de operador 2). También arranca automáticamente la Ronda 1. |
| `/arena open` | Quita los barrotes de hierro de la arena más cercana a ti. Mensaje en el chat. |
| `/arena close` | Vuelve a poner los barrotes de hierro. Mensaje en el chat. |

## Qué construye `/arena x y z`

- Un **suelo circular** decorado (anillos de blackstone pulida / con
  grabados / dorada, con un borde de bloques de oro).
- Un **muro circular decorativo** con pilares y linternas.
- Una **caja invisible de bloques `barrier`** que envuelve *toda* la
  instalación (arena + pasillo + sala de espera) por los 6 lados —
  así es físicamente imposible salir, incluso con el elytra que se
  consigue en la última ronda.
- Una **sala de espera** conectada por un pasillo, con una **puerta
  de barrotes de hierro** que `/arena open` / `/arena close`
  sustituyen por aire o vuelven a colocar.

## Las 4 rondas

1. **Araña gigante** (x7 tamaño, 500 de vida, x3 de daño). Al morir,
   aparece un cofre en un punto aleatorio *accesible* de la arena con
   2 lingotes de netherite, la plantilla de armadura **Eye** (la que
   se llamó "diseño de armadura del End", ya que se obtiene en las
   End Cities), una ballesta y 16 flechas. El cofre desaparece a los
   15 segundos.
2. **Husk / zombi del desierto** (x5, 250 de vida, x10 de daño).
   Recompensa: 32 diamantes, un tridente renombrado "Lanza de
   Diamante" (Minecraft no tiene un ítem de "lanza de diamante"
   independiente, así que se usa el tridente como equivalente más
   cercano), un arco, 64 flechas, 7 totems y la plantilla **Vex**.
3. **2 brujas** (x4, 400 de vida cada una, x3 de daño) → al morir
   ambas aparece un **Warden** (x3, 750 de vida, -50% de daño).
   Recompensa: plantilla **Silence**, 12 sensores de sculk, 12
   fragmentos de netherite, una maza (Mace), 64 cargas de viento y 7
   totems.
4. **5 Golems de hierro** (x3, 1000 de vida, x5 de daño) → **3
   Pillagers con hacha** (x4, 500 de vida, -60% de daño) → **10
   Silverfish** (x5, 200 de vida). Al morir el último, todos los
   jugadores dentro de la arena reciben **Oscuridad durante 1
   minuto** y aparecen **3 cofres finales**:
   - Cofre A: Elytra "irrompible" (Irrompibilidad nivel 100), 2
     mazas, 32 manzanas doradas encantadas.
   - Cofre B: Elytra igual, 3 tridentes, 64 manzanas doradas
     encantadas.
   - Cofre C: 3 bloques `barrier`, 1 bloque de luz, 1 elytra igual y
     una **Poción de 100 Corazones** (Health Boost + curación
     instantánea, +200 de vida en total).

## Compilar

### Opción A — GitHub Actions (recomendado, no necesitas instalar nada)
1. Crea un repositorio nuevo en GitHub y sube **todo** este
   contenido tal cual (incluida la carpeta `.github/workflows`).
2. Ve a la pestaña **Actions** del repo: el workflow `Build ArenaMod`
   se ejecutará solo y te dejará el `.jar` compilado como artefacto
   descargable ("arenamod-jar").

### Opción B — Local
Necesitas JDK 21 instalado.
```bash
gradle wrapper --gradle-version 8.8   # solo la primera vez, genera ./gradlew
./gradlew build
```
El `.jar` final queda en `build/libs/arenamod-1.0.0.jar`.

## Instalar

1. Instala **Fabric Loader** para 1.21.1 (fabricmc.net/use).
2. Descarga **Fabric API** para 1.21.1 y ponlo en la carpeta `mods`.
3. Copia el `.jar` de este mod (el que NO termina en `-sources.jar`)
   también en `mods`.
4. Funciona igual en un servidor dedicado: mismos tres archivos en la
   carpeta `mods` del servidor.

## Notas honestas / posibles ajustes

Este código se ha escrito íntegramente para las convenciones de
Fabric + Yarn 1.21.1, pero **no ha podido compilarse dentro de este
entorno** (no tiene acceso a internet para descargar Gradle/Loom/
Minecraft). Es muy probable que compile a la primera, pero si te da
algún error de compilación al ejecutar el Action o `./gradlew
build`, pégamelo y te lo arreglo — normalmente son detalles menores
de nombres de método que cambian ligeramente entre builds de Yarn
(sobre todo en la parte de encantamientos/`ItemStack` de
`LootFactory.java` y en `EntityType#create(...)` de
`WaveController.java`).

Otras cosas a tener en cuenta:
- Los Pillagers de la ronda 4 llevan un hacha en la mano como se
  pidió, pero por defecto su IA de vainilla sigue usando el
  comportamiento de ballesta a distancia; si quieres que ataquen
  cuerpo a cuerpo de verdad con el hacha hace falta una IA
  personalizada (se puede añadir, dímelo).
- Los multiplicadores de daño ("ataque más fuerte", "x10 de daño",
  etc.) están todos como constantes al principio de
  `WaveController.java` — fácilmente ajustables.
- El estado de una oleada en curso vive en memoria; si el servidor
  se reinicia a mitad de una ronda, la arena en sí y la puerta se
  recuerdan perfectamente, pero el contador de mobs vivos de esa
  ronda concreta se reinicia (limitación asumida para no complicar
  el mod con un sistema de reenganche de entidades).
