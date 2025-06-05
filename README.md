# lwjglx
Desktop LWJGL2 compatibility layer for LWJGL3

## Overrides

Overrides are classes that LWJGLX may need to override from LWJGL3 to fully support LWJGL2 compatibility.

List of overrides:
- `org.lwjgl.BufferUtils`
- `org.lwjgl.openal.AL`
- `org.lwjgl.openal.AL10`
- `org.lwjgl.openal.ALC10`
- `org.lwjgl.opengl.AMDDebugOutput`
- `org.lwjgl.opengl.ARBDebugOutput`
- `org.lwjgl.opengl.ARBShaderObjects`
- `org.lwjgl.opengl.GL11`
- `org.lwjgl.opengl.GL15`
- `org.lwjgl.opengl.GL20`
- `org.lwjgl.opengl.KHRDebug`

If one of these classes appear in your stack-trace, please make sure you made LWJGLX load before LWJGL.

## Workarounds

They can be set by either `-Djvm.value=true/false` or by defining `0/1` as environment variable.

| JVM Argument                         | Environment Variable              | Description                                                                                             |
|:-------------------------------------|:----------------------------------|:--------------------------------------------------------------------------------------------------------|
| `org.lwjglx.assume-gl-extensions`    | `LWJGLX_ASSUME_GL_EXTENSIONS`     | Should assume some LWJGL2 extension support if their LWJGL3 counterpart are supported (default enabled) |
| `org.lwjglx.attr-recreate-display`   | `LWJGLX_ATTR_RECREATE_DISPLAY`    | Should attribute change recreate GLFW display (default enabled)                                         |
| `org.lwjglx.resize-recreate-display` | `LWJGLX_RESIZE_RECREATE_DISPLAY`  | Should resizing recreate GLFW display (default disabled)                                                |
| `org.lwjglx.awt-canvas-no-create`    | `LWJGLX_AWT_CANVAS_NO_CREATE`     | Should avoid creating Display when `new AWTGLCanvas()` is called. (default disabled)                    |
| `org.lwjglx.early-display-create`    | `LWJGLX_EARLY_DISPLAY_CREATE`     | Should create Display as soon as possible (default disabled)                                            |
| `org.lwjglx.early-display-resizable` | `LWJGLX_EARLY_DISPLAY_RESIZEABLE` | Should Display be initially resizable (default disabled)                                                |
| `org.lwjglx.translate-key-names`     | `LWJGLX_TRANSLATE_KEY_NAMES`      | Should use keymap for key names instead of static key name (default enabled)                            |
| `org.lwjglx.disable-window-icon`     | `LWJGLX_DISABLE_WINDOW_ICON`      | Should ignore windows icons (default enabled cause icons are currently broken)                          |


## Game specific workaround

TODO (defaults shuold work fine)

For an android version of the library please check out [pojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/tree/v3_openjdk/jre_lwjgl3glfw).
