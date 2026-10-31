# El Limbo — Fabric 1.21.1

## Compilar
Requisitos: JDK 21.

1. Entra a https://fabricmc.net/develop/template/ , elige Minecraft 1.21.1, mod id `limbo`,
   descarga la plantilla y copia SOLO `gradlew`, `gradlew.bat` y la carpeta `gradle/` a esta carpeta
   (o ejecuta `gradle wrapper --gradle-version 8.10` si tienes Gradle instalado).
2. `./gradlew build`  → el .jar queda en `build/libs/limbo-1.0.0.jar`
3. Mete el jar + Fabric API 1.21.1 en la carpeta `mods` (cliente Y servidor).
   Para probar en desarrollo: `./gradlew runClient`.

Si Gradle no encuentra la versión de Fabric API, cambia `fabric_version` en gradle.properties
por la última para 1.21.1.

## Probar rápido
- `/give @s limbo:limbo_trident` o craftea: tridente + lingote de netherita (sale con Riptide III).
- Pon una cama en el Nether y úsala, o rompe una cama en el Overworld → vas al Limbo.
- `/execute in limbo:limbo run tp @s 0 41 -18` para ir directo.
- Para regenerar las estructuras: borra la carpeta `DIM-limbo`... y `data/limbo_return.dat` del mundo.

## Ajustes
- `LimboDimension`: spawn, radio y altura del agujero.
- `LimboEvents`: DRAIN_PER_SECOND y MIN_MAX_HEALTH (agua), bypass de creativo/op.
- `LimboManager`: RESTORE_MAX_HEALTH_ON_ESCAPE.
- `LimboStructure`: tamaño y daño del coliseo, torres y puentes.
