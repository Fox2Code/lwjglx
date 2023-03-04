# lwjglx
LWJGL2 compatibility layer for LWJGL3

## Overrides

Overrides are classes that LWJGLX may need to override from LWJGL3 to fully support LWJGL2 compatibility.

List of overrides:
- `org.lwjgl.BufferUtils`
- `org.lwjgl.openal.AL`
- `org.lwjgl.openal.AL10`
- `org.lwjgl.openal.ALC10`
- `org.lwjgl.opengl.ARBShaderObjects`
- `org.lwjgl.opengl.GL11`
- `org.lwjgl.opengl.GL15`
- `org.lwjgl.opengl.GL20`

If one of these classes appear in your stack-trace, please make sure you made LWJGLX load before LWJGL.
