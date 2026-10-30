# Desafío Menu — Fabric 1.21.1 (mappings Yarn)

Menú de Misiones / Guía / Historia / Bestiario con Modo Editor, copiado del video.
Abrir con **G** (rebindable en Controles → "Desafío"). Cerrar con **G** o **Esc**.

## Compilar
Necesitas JDK 21. Elige una opción:

1. **Con Gradle instalado** (8.12+), dentro de esta carpeta:
       gradle wrapper --gradle-version 8.12
       ./gradlew build          (el .jar queda en build/libs/, usa el que NO termina en -sources)
       ./gradlew runClient      (para probarlo en Minecraft)
2. **Sin instalar nada**: sube la carpeta a un repositorio de GitHub; la pestaña *Actions* compila
   y deja el .jar descargable (.github/workflows/build.yml).
3. **IntelliJ IDEA**: abre la carpeta como proyecto Gradle e importa; ejecuta la tarea `runClient`.

Instalación: copia el .jar a `mods/` junto con **Fabric Loader 0.16+** y **Fabric API** para 1.21.1.
Debe estar en el cliente **y** en el servidor (si juegas en servidor).
Si `fabric_version` ya no existe, usa la última `+1.21.1` de https://fabricmc.net/develop/.

## Paquetes
- `com.desafio.menu`            -> DesafioMenu (ModInitializer: registra payloads, eventos, comando)
- `...client`                   -> DesafioMenuClient (keybind G + receptor), ClientData
- `...screen`                   -> DesafioScreen (menú), EditorScreen, EntryFormScreen, Ui, RichText
- `...data`                     -> Content (modelo), ContentStore/ProgressStore (JSON), QuestLogic, Defaults
- `...networking`               -> SyncPayload (S2C), EditPayload / ClaimPayload (C2S), ServerNetworking
- `...server`                   -> QuestTracker (progreso), RecipeApplier, DesafioCommand

## JSON (config/desafio_menu/)
quests.json, guide.json, lore.json, bestiary.json, recipes.json, progress.json.
Se crean con el contenido del video la primera vez. Puedes editarlos a mano y usar `/desafio reload`.

## Tipos de misión
- obtain: cuenta ítems en el inventario (target = item id)
- visit:  entrar a una dimensión (target = minecraft:the_nether)
- kill:   matar entidades (target = zombie / minecraft:zombie)
- custom: `/desafio add <jugador> <misión> <cantidad>`
Una misión se desbloquea cuando las de `requires` están COMPLETADAS (reclamadas).

## Texto resaltado
`[[texto]]` se dibuja en azul (como "Lingotes de Hierro" en el video).

## Texturas opcionales (assets/desafio_menu/textures/gui/)
frame.png (1280x674), tab_lore.png, tab_quests.png, tab_guide.png, tab_bestiary.png (128x128).
Sin ellas, el marco se dibuja por código y los íconos usan ítems vanilla.

## Permisos del editor
Operadores (nivel 2) o el anfitrión en un mundo local.
