package org.lwjgl.opengl;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.glfw.GLFW.*;

import java.awt.Canvas;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Display {

	private static String windowTitle = "Game";

	private static org.lwjgl.opengl.GLContext context;

	private static DisplayImplementation currentImpl;

	private static boolean displayCreated = false;
	private static boolean displayFocused = false;
	private static boolean displayVisible = true;
	private static boolean displayDirty = false;
	private static boolean displayResizable = false;

	private static DisplayMode mode, desktopDisplayMode;

	private static int latestEventKey = 0;

	private static int displayX = -1;
	private static int displayY = -1;

	private static boolean displayResized = false;
	private static int displayWidth = 0;
	private static int displayHeight = 0;
	private static int displayFramebufferWidth = 0;
	private static int displayFramebufferHeight = 0;

	private static boolean latestResized = false;
	private static int latestWidth = 0;
	private static int latestHeight = 0;

	private static boolean vsyncEnabled = false;
	private static boolean displayFullscreen = false;
	private static float fps;

	private static GLFWImage.Buffer icons;

	static {
		Sys.initialize(); // init using dummy sys method

		long monitor = glfwGetPrimaryMonitor();
		GLFWVidMode vidmode = glfwGetVideoMode(monitor);

		int monitorWidth = vidmode.width();
		int monitorHeight = vidmode.height();
		int monitorBitPerPixel = vidmode.redBits() + vidmode.greenBits() + vidmode.blueBits();
		int monitorRefreshRate = vidmode.refreshRate();
        
        mode = desktopDisplayMode = new DisplayMode(monitorWidth, monitorHeight, monitorBitPerPixel, monitorRefreshRate);
        LWJGLUtil.log("Initial mode: " + desktopDisplayMode);
        
        // additional code workaround not called yet!
        LWJGLUtil.log("Calling Display.create()");
        create();
	}

	public static void create(PixelFormat pixel_format, Drawable shared_drawable) throws LWJGLException {
		// System.out.println("TODO: Implement Display.create(PixelFormat,
		// Drawable)"); // TODO
		create(pixel_format);
	}

	public static void create(PixelFormat pixel_format, ContextAttribs attribs) throws LWJGLException {
		// System.out.println("TODO: Implement Display.create(PixelFormat,
		// ContextAttribs)"); // TODO
		create(pixel_format);
	}

	public static void create(PixelFormat format) throws LWJGLException {
		glfwWindowHint(GLFW_ACCUM_ALPHA_BITS, format.getAccumulationBitsPerPixel());
		glfwWindowHint(GLFW_ALPHA_BITS, format.getAlphaBits());
		glfwWindowHint(GLFW_AUX_BUFFERS, format.getAuxBuffers());
		glfwWindowHint(GLFW_DEPTH_BITS, format.getDepthBits());
		glfwWindowHint(GLFW_SAMPLES, format.getSamples());
		glfwWindowHint(GLFW_STENCIL_BITS, format.getStencilBits());
		create();
	}

	public static void create() throws LWJGLException {
		if (Window.handle != MemoryUtil.NULL)
			glfwDestroyWindow(Window.handle);

		long monitor = glfwGetPrimaryMonitor();
		GLFWVidMode vidmode = glfwGetVideoMode(monitor);

		int monitorWidth = vidmode.width();
		int monitorHeight = vidmode.height();
		int monitorBitPerPixel = vidmode.redBits() + vidmode.greenBits() + vidmode.blueBits();
		int monitorRefreshRate = vidmode.refreshRate();

		desktopDisplayMode = new DisplayMode(monitorWidth, monitorHeight, monitorBitPerPixel, monitorRefreshRate);

		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, displayResizable ? GL_TRUE : GL_FALSE);
		glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GL_TRUE);

		Window.handle = glfwCreateWindow(mode.getWidth(), mode.getHeight(), windowTitle, NULL, NULL);
		if (Window.handle == NULL)
			throw new LWJGLException("Failed to create Display window");

		Window.keyCallback = new GLFWKeyCallback() {
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				latestEventKey = key;

				// if (action == GLFW_RELEASE || action == GLFW.GLFW_PRESS) {
				// Keyboard.addKeyEvent(key, action == GLFW.GLFW_PRESS ? true :
				// false);
				// }

				Keyboard.addKeyEvent(key, action);
			}
		};

		Window.charCallback = new GLFWCharCallback() {
			@Override
			public void invoke(long window, int codepoint) {
				Keyboard.addCharEvent(latestEventKey, (char) codepoint);
			}
		};

		Window.cursorEnterCallback = new GLFWCursorEnterCallback() {
			@Override
			public void invoke(long window, boolean entered) {
				Mouse.setMouseInsideWindow(entered == true);
			}
		};

		Window.cursorPosCallback = new GLFWCursorPosCallback() {
			@Override
			public void invoke(long window, double xpos, double ypos) {
				Mouse.addMoveEvent(xpos, ypos);
			}
		};

		Window.mouseButtonCallback = new GLFWMouseButtonCallback() {
			@Override
			public void invoke(long window, int button, int action, int mods) {
				Mouse.addButtonEvent(button, action == GLFW.GLFW_PRESS ? true : false);
			}
		};

		Window.windowFocusCallback = new GLFWWindowFocusCallback() {
			@Override
			public void invoke(long window, boolean focused) {
				displayFocused = focused == true;
			}
		};

		Window.windowIconifyCallback = new GLFWWindowIconifyCallback() {
			@Override
			public void invoke(long window, boolean iconified) {
				displayVisible = iconified == false;
			}
		};

		Window.windowSizeCallback = new GLFWWindowSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				latestResized = true;
				latestWidth = width;
				latestHeight = height;
			}
		};

		Window.windowPosCallback = new GLFWWindowPosCallback() {
			@Override
			public void invoke(long window, int xpos, int ypos) {
				displayX = xpos;
				displayY = ypos;
			}
		};

		Window.windowRefreshCallback = new GLFWWindowRefreshCallback() {
			@Override
			public void invoke(long window) {
				displayDirty = true;
			}
		};

		Window.framebufferSizeCallback = new GLFWFramebufferSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				displayFramebufferWidth = width;
				displayFramebufferHeight = height;
			}
		};

		Window.scrollCallback = new GLFWScrollCallback() {
			@Override
			public void invoke(long window, double xoffset, double yoffset) {
				Mouse.addWheelEvent((int) (yoffset * 120));
			}
		};

		Window.setCallbacks();

		displayWidth = mode.getWidth();
		displayHeight = mode.getHeight();

		IntBuffer fbw = BufferUtils.createIntBuffer(1);
		IntBuffer fbh = BufferUtils.createIntBuffer(1);
		glfwGetFramebufferSize(Window.handle, fbw, fbh);
		displayFramebufferWidth = fbw.get(0);
		displayFramebufferHeight = fbh.get(0);

		glfwSetWindowPos(Window.handle, (monitorWidth - mode.getWidth()) / 2, (monitorHeight - mode.getHeight()) / 2);

		if (displayX == -1) {
			displayX = (monitorWidth - mode.getWidth()) / 2;
		}

		if (displayY == -1) {
			displayY = (monitorHeight - mode.getHeight()) / 2;
		}

		makeCurrent();
		context = org.lwjgl.opengl.GLContext.createFromCurrent();

		glfwSwapInterval(0);
		glfwShowWindow(Window.handle);

		Mouse.create();
		Keyboard.create();

		// glfwSetWindowIcon(Window.handle, icons);
		currentImpl = new DisplayImplementation() {

			@Override
			public void setNativeCursor(Object handle) throws LWJGLException {
				try {
					Mouse.setNativeCursor((Cursor) handle);
				} catch (ClassCastException e) {
					throw new LWJGLException("Handle is not an instance of cursor");
				}
			}

			@Override
			public void setCursorPosition(int x, int y) {
				Mouse.setCursorPosition(x, y);
			}

			@Override
			public void readMouse(ByteBuffer buffer) {

			}

			@Override
			public void readKeyboard(ByteBuffer buffer) {
				// TODO Auto-generated method stub

			}

			@Override
			public void pollMouse(IntBuffer coord_buffer, ByteBuffer buttons) {

			}

			@Override
			public void pollKeyboard(ByteBuffer keyDownBuffer) {
				// TODO Auto-generated method stub

			}

			@Override
			public boolean isInsideWindow() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean hasWheel() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void grabMouse(boolean grab) {
				Mouse.setGrabbed(grab);
			}

			@Override
			public int getNativeCursorCapabilities() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getMinCursorSize() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getMaxCursorSize() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getButtonCount() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public void destroyMouse() {
				// TODO Auto-generated method stub

			}

			@Override
			public void destroyKeyboard() {
				// TODO Auto-generated method stub

			}

			@Override
			public void destroyCursor(Object cursor_handle) {
				// TODO Auto-generated method stub

			}

			@Override
			public void createMouse() throws LWJGLException {
				// TODO Auto-generated method stub

			}

			@Override
			public void createKeyboard() throws LWJGLException {
				// TODO Auto-generated method stub

			}

			@Override
			public Object createCursor(int width, int height, int xHotspot, int yHotspot, int numImages,
					IntBuffer images, IntBuffer delays) throws LWJGLException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean wasResized() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void update() {
				Display.update();
			}

			@Override
			public void switchDisplayMode(DisplayMode mode) throws LWJGLException {
				Display.setDisplayMode(mode);
			}

			@Override
			public void setTitle(String title) {
				windowTitle = title;
			}

			@Override
			public void setResizable(boolean resizable) {
				Display.setResizable(resizable);
			}

			@Override
			public void setPbufferAttrib(PeerInfo handle, int attrib, int value) {
				// TODO Auto-generated method stub

			}

			@Override
			public int setIcon(ByteBuffer[] icons) {
				// TODO Auto-generated method stub
				Display.setIcon(icons);
				return 0;
			}

			@Override
			public void setGammaRamp(FloatBuffer gammaRamp) throws LWJGLException {
				// TODO Auto-generated method stub

			}

			@Override
			public void reshape(int x, int y, int width, int height) {
				// TODO Auto-generated method stub

			}

			@Override
			public void resetDisplayMode() {
				try {
					Display.setDisplayMode(desktopDisplayMode);
				} catch (LWJGLException e) {
				}
			}

			@Override
			public void releaseTexImageFromPbuffer(PeerInfo handle, int buffer) {
				// TODO Auto-generated method stub

			}

			@Override
			public boolean isVisible() {
				// TODO Auto-generated method stub
				return Display.displayVisible;
			}

			@Override
			public boolean isDirty() {
				// TODO Auto-generated method stub
				return Display.displayDirty;
			}

			@Override
			public boolean isCloseRequested() {
				// TODO Auto-generated method stub
				return GLFW.glfwWindowShouldClose(Window.handle);
			}

			@Override
			public boolean isBufferLost(PeerInfo handle) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean isActive() {
				// TODO Auto-generated method stub
				return Display.displayFocused;
			}

			@Override
			public DisplayMode init() throws LWJGLException {
				// TODO Auto-generated method stub
				return desktopDisplayMode;
			}

			@Override
			public int getY() {
				// TODO Auto-generated method stub
				return Display.displayY;
			}

			@Override
			public int getX() {
				// TODO Auto-generated method stub
				return Display.displayX;
			}

			@Override
			public int getWidth() {
				// TODO Auto-generated method stub
				return Display.latestWidth;
			}

			@Override
			public String getVersion() {
				// TODO Auto-generated method stub
				return Sys.getVersion();
			}

			@Override
			public float getPixelScaleFactor() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getPbufferCapabilities() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getHeight() {
				// TODO Auto-generated method stub
				return Display.latestHeight;
			}

			@Override
			public int getGammaRampLength() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public DisplayMode[] getAvailableDisplayModes() throws LWJGLException {
				// TODO Auto-generated method stub
				return Display.getAvailableDisplayModes();
			}

			@Override
			public String getAdapter() {
				// TODO Auto-generated method stub
				return Display.getAdapter();
			}

			@Override
			public void destroyWindow() {
				Display.destroy();
			}

			@Override
			public void createWindow(DrawableLWJGL drawable, DisplayMode mode, Canvas parent, int x, int y)
					throws LWJGLException {
				// TODO Auto-generated method stub

			}

			@Override
			public PeerInfo createPeerInfo(PixelFormat pixel_format, ContextAttribs attribs) throws LWJGLException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public PeerInfo createPbuffer(int width, int height, PixelFormat pixel_format, ContextAttribs attribs,
					IntBuffer pixelFormatCaps, IntBuffer pBufferAttribs) throws LWJGLException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void bindTexImageToPbuffer(PeerInfo handle, int buffer) {
				// TODO Auto-generated method stub

			}
		};

		displayCreated = true;

	}

	public static boolean isCreated() {
		return displayCreated;
	}

	public static boolean isActive() {
		return displayFocused;
	}

	public static boolean isVisible() {
		return displayVisible;
	}

	public static org.lwjgl.opengl.GLContext getContext() {
		return context;
	}

	public static void setLocation(int new_x, int new_y) {
		if (Window.handle == 0L) {
			Display.displayX = new_x;
			Display.displayY = new_y;
		} else {
			GLFW.glfwSetWindowPos(Window.handle, new_x, new_y);
		}
	}

	public static void setVSyncEnabled(boolean sync) {
		vsyncEnabled = sync;
	}

	public static long getWindow() {
		return Window.handle;
	}

	public static void update() {
		update(true);
	}

	public static void update(boolean processMessages) {
		try {
			swapBuffers();
			displayDirty = false;

		} catch (LWJGLException e) {
			throw new RuntimeException(e);
		}

		if (processMessages)
			processMessages();
	}

	public static void processMessages() {
		glfwPollEvents();
		Keyboard.poll();
		Mouse.poll();

		if (latestResized) {
			latestResized = false;
			displayResized = true;
			displayWidth = latestWidth;
			displayHeight = latestHeight;
		} else {
			displayResized = false;
		}
	}

	public static void swapBuffers() throws LWJGLException {
		glfwSwapBuffers(Window.handle);
	}

	public static void destroy() {
		Window.releaseCallbacks();
		glfwDestroyWindow(Window.handle);

		displayCreated = false;
	}

	public static void setDisplayMode(DisplayMode dm) throws LWJGLException {
		mode = dm;
		newCurrentWindow(GLFW.glfwCreateWindow(dm.getWidth(), dm.getHeight(), windowTitle, 0, 0));
	}

	public static DisplayMode getDisplayMode() {
		return mode;
	}

	public static DisplayMode[] getAvailableDisplayModes() throws LWJGLException {
		GLFWVidMode.Buffer modes = GLFW.glfwGetVideoModes(GLFW.glfwGetPrimaryMonitor());

		DisplayMode[] displayModes = new DisplayMode[modes.capacity()];

		for (int i = 0; i < modes.capacity(); i++) {
			modes.position(i);

			int w = modes.width();
			int h = modes.height();
			int b = modes.redBits() + modes.greenBits() + modes.blueBits();
			int r = modes.refreshRate();

			displayModes[i] = new DisplayMode(w, h, b, r);
		}

		return displayModes;
	}

	public static DisplayMode getDesktopDisplayMode() {
		long mon = GLFW.glfwGetPrimaryMonitor();
		GLFWVidMode mode = GLFW.glfwGetVideoMode(mon);
		return new DisplayMode(mode.width(), mode.height(), mode.redBits() + mode.greenBits() + mode.blueBits(),
				mode.refreshRate());
	}

	public static boolean wasResized() {
		return displayResized;
	}

	public static int getX() {
		return displayX;
	}

	public static int getY() {
		return displayY;
	}

	public static int getWidth() {
		return displayWidth;
	}

	public static int getHeight() {
		return displayHeight;
	}

	public static int getFramebufferWidth() {
		return displayFramebufferWidth;
	}

	public static int getFramebufferHeight() {
		return displayFramebufferHeight;
	}

	public static void setTitle(String title) {
		windowTitle = title;
	}

	public static boolean isCloseRequested() {
		return glfwWindowShouldClose(Window.handle) == true;
	}

	public static boolean isDirty() {
		return displayDirty;
	}

	public static void setInitialBackground(float red, float green, float blue) {
		// System.out.println("TODO: Implement Display.setInitialBackground(float, float, float)");
        
        if (Window.handle != MemoryUtil.NULL) {
            GL11.glClearColor(red, green, blue, 1f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        }
	}

	public static int setIcon(java.nio.ByteBuffer[] icons) {
		// TODO
		// if(Window.handle == MemoryUtil.NULL)
		Display.icons = new GLFWImage.Buffer(icons[1]);
		// else glfwSetWindowIcon(Window.handle, new
		// GLFWImage.Buffer(icons[0]));
		return 0;
	}

	public static void setResizable(boolean resizable) {
		displayResizable = resizable;
		if (displayResizable ^ resizable) {
			if (Window.handle != 0) {
				IntBuffer width = BufferUtils.createIntBuffer(1);
				IntBuffer height = BufferUtils.createIntBuffer(1);
				GLFW.glfwGetWindowSize(Window.handle, width, height);
				width.rewind();
				height.rewind();

				GLFW.glfwDefaultWindowHints();
				glfwWindowHint(GLFW_VISIBLE, displayVisible ? GL_TRUE : GL_FALSE);
				glfwWindowHint(GLFW_RESIZABLE, displayResizable ? GL_TRUE : GL_FALSE);
				glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GL_TRUE);

				newCurrentWindow(GLFW.glfwCreateWindow(width.get(), height.get(), windowTitle,
						GLFW.glfwGetWindowMonitor(Window.handle), NULL));
			}
		}
		displayResizable = resizable;
	}

	public static boolean isResizable() {
		return displayResizable;
	}

	public static void setDisplayModeAndFullscreen(DisplayMode mode) throws LWJGLException {
		Display.mode = mode;
		newCurrentWindow(glfwCreateWindow(mode.getWidth(), mode.getHeight(), windowTitle,
				mode.isFullscreenCapable() ? glfwGetPrimaryMonitor() : NULL, NULL));
	}

	public static void setFullscreen(boolean fullscreen) throws LWJGLException {
		if (isFullscreen() ^ fullscreen) {
			if (fullscreen && (!mode.isFullscreenCapable()))
				throw new LWJGLException("Display mode is not fullscreen capable");
			if (Window.handle != 0) {
				IntBuffer width = BufferUtils.createIntBuffer(1);
				IntBuffer height = BufferUtils.createIntBuffer(1);
				glfwGetWindowSize(Window.handle, width, height);
				width.rewind();
				height.rewind();

				glfwDefaultWindowHints();
				glfwWindowHint(GLFW_VISIBLE, displayVisible ? GL_TRUE : GL_FALSE);
				glfwWindowHint(GLFW_RESIZABLE, displayResizable ? GL_TRUE : GL_FALSE);
				glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GL_TRUE);

				if (fullscreen)
					newCurrentWindow(glfwCreateWindow(width.get(), height.get(), windowTitle,
							glfwGetPrimaryMonitor(), NULL));
				else
					newCurrentWindow(glfwCreateWindow(width.get(), height.get(), windowTitle, NULL, NULL));
			}
		}
		displayFullscreen = fullscreen;
	}

	public static boolean isFullscreen() {
		return displayFullscreen;
	}

	public static void releaseContext() throws LWJGLException {
		glfwMakeContextCurrent(0);
	}

	public static boolean isCurrent() throws LWJGLException {
		return glfwGetCurrentContext() == Window.handle;
	}

	public static void makeCurrent() throws LWJGLException {
		glfwMakeContextCurrent(Window.handle);
	}

	public static java.lang.String getAdapter() {
		// TODO
		return "GeNotSupportedAdapter";
	}

	public static java.lang.String getVersion() {
		// TODO
		return "1.0 NOT SUPPORTED";
	}

	/**
	 * An accurate sync method that will attempt to run at a constant frame
	 * rate. It should be called once every frame.
	 *
	 * @param fps
	 *            - the desired frame rate, in frames per second
	 */
	public static void sync(int fps) {
		if (vsyncEnabled)
			Sync.sync(fps);
	}

	public static Drawable getDrawable() {
		return null;
	}

	static DisplayImplementation getImplementation() {
		return currentImpl;
	}

	private static void newCurrentWindow(long newWindow) {
        if (Window.handle != MemoryUtil.NULL)
		    glfwDestroyWindow(Window.handle);
		Window.handle = newWindow;
		try {
			Mouse.setNativeCursor(Mouse.getCurrentCursor());
		} catch (LWJGLException e) {
			System.err.println("Failed to set new window cursor!");
			e.printStackTrace();
		}
		GLFW.glfwSetWindowTitle(newWindow, windowTitle);
		Window.setCallbacks();

		glfwMakeContextCurrent(Window.handle);
		context = org.lwjgl.opengl.GLContext.createFromCurrent();

		glfwSwapInterval(0);
		glfwShowWindow(Window.handle);
	}

	private static class Window {
		static long handle;

		static GLFWKeyCallback keyCallback;
		static GLFWCharCallback charCallback;
		static GLFWCursorEnterCallback cursorEnterCallback;
		static GLFWCursorPosCallback cursorPosCallback;
		static GLFWMouseButtonCallback mouseButtonCallback;
		static GLFWWindowFocusCallback windowFocusCallback;
		static GLFWWindowIconifyCallback windowIconifyCallback;
		static GLFWWindowSizeCallback windowSizeCallback;
		static GLFWWindowPosCallback windowPosCallback;
		static GLFWWindowRefreshCallback windowRefreshCallback;
		static GLFWFramebufferSizeCallback framebufferSizeCallback;
		static GLFWScrollCallback scrollCallback;

		public static void setCallbacks() {
			glfwSetKeyCallback(handle, keyCallback);
			glfwSetCharCallback(handle, charCallback);
			glfwSetCursorEnterCallback(handle, cursorEnterCallback);
			glfwSetCursorPosCallback(handle, cursorPosCallback);
			glfwSetMouseButtonCallback(handle, mouseButtonCallback);
			glfwSetWindowFocusCallback(handle, windowFocusCallback);
			glfwSetWindowIconifyCallback(handle, windowIconifyCallback);
			glfwSetWindowSizeCallback(handle, windowSizeCallback);
			glfwSetWindowPosCallback(handle, windowPosCallback);
			glfwSetWindowRefreshCallback(handle, windowRefreshCallback);
			glfwSetFramebufferSizeCallback(handle, framebufferSizeCallback);
			glfwSetScrollCallback(handle, scrollCallback);
		}

		public static void releaseCallbacks() {
			Callbacks.glfwFreeCallbacks(handle);
			keyCallback = null;
			charCallback = null;
			cursorEnterCallback = null;
			cursorPosCallback = null;
			mouseButtonCallback = null;
			windowFocusCallback = null;
			windowIconifyCallback = null;
			windowSizeCallback = null;
			windowPosCallback = null;
			windowRefreshCallback = null;
			framebufferSizeCallback = null;
			scrollCallback = null;
			System.gc();
		}
	}

}
