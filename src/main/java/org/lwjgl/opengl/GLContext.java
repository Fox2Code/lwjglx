package org.lwjgl.opengl;

import org.lwjgl.LWJGLException;

import javax.annotation.Nullable;

public final class GLContext {
	private static final ContextCapabilities contextCapabilities = new ContextCapabilities();
	
	public static ContextCapabilities getCapabilities() {
		return contextCapabilities;
	}

    public static synchronized void useContext(
            @Nullable Object context) throws LWJGLException {
        useContext(context, false);
    }

    public static synchronized void useContext(
            @Nullable Object context, boolean forwardCompatible) throws LWJGLException {}

	public static synchronized void loadOpenGLLibrary() throws LWJGLException {
		try {
			GL.initialize();
		} catch (Throwable t) {
			throw new LWJGLException(t);
		}
	}

	public static synchronized void unloadOpenGLLibrary() {}
}
