package org.lwjgl.opengl;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.glfw.GLFW.*;

import java.awt.Canvas;
import java.nio.*;
import java.util.Objects;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.system.LWJGLXHelper;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.system.Pointer;

import javax.annotation.Nullable;

public class Display {

    private static String windowTitle = "Game";

    private static DisplayImplementation display_impl;

    private static boolean displayCreated = false;
    private static boolean displayFocused = true;
    private static boolean displayVisible = true;
    private static boolean displayDirty = false;
    private static boolean displayResizable = LWJGLXHelper.earlyDisplayResizable;

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
    private static boolean displayFullscreen = true;
    private static float fps;
    
    private static boolean window_created;

    /** The Drawable instance that tracks the current Display context */
    @Nullable
    private static DrawableLWJGL drawable;

    @Nullable
    private static Canvas parent;

    @Nullable
    private static GLFWIcons icons;

    @Nullable
    private static GLFWIcons activeIcons;

    private static int swap_interval;

    static {
        Sys.initialize(); // init using dummy sys method

        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode vidmode = glfwGetVideoMode(monitor);

        int monitorWidth = displayWidth = displayFramebufferWidth = vidmode.width();
        int monitorHeight = displayHeight = displayFramebufferHeight = vidmode.height();
        int monitorBitPerPixel = vidmode.redBits() + vidmode.greenBits() + vidmode.blueBits();
        int monitorRefreshRate = vidmode.refreshRate();

        mode = desktopDisplayMode = new DisplayMode(monitorWidth, monitorHeight, monitorBitPerPixel, monitorRefreshRate);
        LWJGLUtil.log("Initial mode: " + desktopDisplayMode);

        if (LWJGLXHelper.earlyDisplayCreate) {
            // additional code workaround not called yet!
            LWJGLUtil.log("Calling Display.create()");
            try {
                create();
            } catch (LWJGLException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    public static void setSwapInterval(int value) {
        synchronized ( GlobalLock.lock ) {
            swap_interval = value;
            if (drawable != null && isCreated()) {

                drawable.setSwapInterval(swap_interval);
                
            }
        }
	}
    
    private static void makeCurrentAndSetSwapInterval() throws LWJGLException {
        makeCurrent();
        if (drawable != null) {
            try {
                drawable.checkGLError();
            } catch (OpenGLException e) {
                LWJGLUtil.log("OpenGL error during context creation: " + e.getMessage());
            }
        }
        setSwapInterval(swap_interval);
    }

    private static void initContext() {
        if (drawable == null) {
            throw new IllegalStateException("No drawable for context!");
        }
        drawable.initContext(0, 0, 0);
        update();
	}
    
    private static void initControls() {
        // Automatically create mouse, keyboard and controller
        if ( true ) {
            if ( !Mouse.isCreated() ) {
                try {
                    Mouse.create();
                } catch (LWJGLException e) {
                    if ( LWJGLUtil.DEBUG ) {
                        e.printStackTrace(System.err);
                    } else {
                        LWJGLUtil.log("Failed to create Mouse: " + e);
                    }
                }
            }
            if ( !Keyboard.isCreated() ) {
                try {
                    Keyboard.create();
                } catch (LWJGLException e) {
                    if ( LWJGLUtil.DEBUG ) {
                        e.printStackTrace(System.err);
                    } else {
                        LWJGLUtil.log("Failed to create Keyboard: " + e);
                    }
                }
            }
        }
	}
    
    private static void releaseDrawable() {
        if (drawable == null) {
            return;
        }
        try {
            Context context = drawable.getContext();
            if (context != null && context.isCurrent()) {
                context.releaseCurrent();
                context.releaseDrawable();
            }
        } catch (LWJGLException e) {
            LWJGLUtil.log("Exception occurred while trying to release context: " + e);
        }
	}
    
    private static void destroyWindow() {
        if ( !window_created ) {
            return;
        }
        releaseDrawable();

        // Automatically destroy keyboard & mouse
        if ( Mouse.isCreated() ) {
            Mouse.destroy();
        }
        if ( Keyboard.isCreated() ) {
            Keyboard.destroy();
        }
        display_impl.destroyWindow();
        window_created = false;
	}

    private static void reset() {
        display_impl.resetDisplayMode();
	}

    private static void createWindow() throws LWJGLException {
        if ( window_created ) {
            return;
        }
        if (drawable == null) {
            throw new IllegalStateException("No drawable for context!");
        }
        DisplayMode mode = Display.getDisplayMode();
        display_impl.createWindow(drawable, mode, null, 0, 0 /* getWindowX(), getWindowY() */);
        window_created = true;

        displayWidth = mode.getWidth();
        displayHeight = mode.getHeight();

        // setTitle(title);
        initControls();

        // set cached window icon if exists
        /*
        if ( cached_icons != null ) {
            setIcon(cached_icons);
        } else {
            
        }
        */
	}

    public static void create(PixelFormat pixel_format, Drawable shared_drawable) throws LWJGLException {
        ContextAttribs contextAttribs = new ContextAttribs();
        create(pixel_format, contextAttribs);
        
        final DrawableGL drawable = new DrawableGL() {
            public void destroy() {
                synchronized ( GlobalLock.lock ) {
                    if ( !isCreated() )
                        return;

                    releaseDrawable();
                    super.destroy();
                    destroyWindow();
                    // x = y = -1;
                    // cached_icons = null;
                    reset();
                }
            }
        };
        Display.drawable = drawable;

        try {
            drawable.setPixelFormat(pixel_format, contextAttribs);
            try {
                createWindow();
                try {
                    drawable.context = new ContextGL(drawable.peer_info, contextAttribs,
                            shared_drawable != null ? ((DrawableGL)shared_drawable).getContext() : null);
                    try {
                        makeCurrentAndSetSwapInterval();
                        initContext();
                    } catch (LWJGLException e) {
                        //drawable.destroy();
                        throw e;
                    }
                } catch (LWJGLException e) {
                    destroyWindow();
                    throw e;
                }
            } catch (LWJGLException e) {
                drawable.destroy();
                throw e;
            }
        } catch (LWJGLException e) {
            display_impl.resetDisplayMode();
            throw e;
        }
    }

    public static void create(PixelFormat format) throws LWJGLException {
        create(format, (ContextAttribs) null);
    }

    public static void create(PixelFormat format,@Nullable ContextAttribs attribs) throws LWJGLException {
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_ACCUM_ALPHA_BITS, format.getAccumulationBitsPerPixel());
        glfwWindowHint(GLFW_ALPHA_BITS, format.getAlphaBits());
        glfwWindowHint(GLFW_AUX_BUFFERS, format.getAuxBuffers());
        glfwWindowHint(GLFW_DEPTH_BITS, format.getDepthBits());
        glfwWindowHint(GLFW_SAMPLES, format.getSamples());
        glfwWindowHint(GLFW_STENCIL_BITS, format.getStencilBits());
        if (attribs != null) {
            if (attribs.getMajorVersion() != 1 || attribs.getMinorVersion() != 0) {
                glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, attribs.getMajorVersion());
                glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, attribs.getMinorVersion());
            }
            if (attribs.isProfileCore()) {
                glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            } else if (attribs.isProfileCompatibility()) {
                glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE);
            }
            if (attribs.isForwardCompatible()) {
                glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
            }
        }
        lwjglxCreate();
    }

    private static boolean isCreated = false;
    public static void create() throws LWJGLException {
        glfwDefaultWindowHints();
        lwjglxCreate();
    }

    private static void lwjglxCreate() throws LWJGLException {
        if (isCreated) return;
        else isCreated = true;

        if (Window.handle != MemoryUtil.NULL)
            glfwDestroyWindow(Window.handle);

        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode vidmode = glfwGetVideoMode(monitor);

        int monitorWidth = vidmode.width();
        int monitorHeight = vidmode.height();
        int monitorBitPerPixel = vidmode.redBits() + vidmode.greenBits() + vidmode.blueBits();
        int monitorRefreshRate = vidmode.refreshRate();

        desktopDisplayMode = new DisplayMode(monitorWidth, monitorHeight, monitorBitPerPixel, monitorRefreshRate);

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

                if(parent != null) parent.setSize(width, height);
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

        glfwMakeContextCurrent(Window.handle);
        Objects.requireNonNull(org.lwjgl.opengl.GLContext.getCapabilities());

        glfwShowWindow(Window.handle);
        if(parent != null) parent.setSize(displayWidth, displayHeight);

        Mouse.create();
        Keyboard.create();

        if (parent == null) {
            if (Display.icons != null) {
                lwjglxSetWindowIcons(Display.icons);
                Display.icons = null;
            } else if (Display.activeIcons != null) {
                lwjglxSetWindowIcons(Display.activeIcons);
            } else {
                lwjglxSetDefaultIcons();
            }
        }

        Keyboard.lwjglxInitializeKeyboardTranslate();

        display_impl = new DisplayImplementation() {

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
                Display.setLocation(x, y);
                if (Window.handle != NULL) {
                    GLFW.glfwSetWindowSize(Window.handle, width, height);
                }
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
                return false;
            }

            @Override
            public boolean isActive() {
                return Display.displayFocused;
            }

            @Override
            public DisplayMode init() {
                return desktopDisplayMode;
            }

            @Override
            public int getY() {
                return Display.displayY;
            }

            @Override
            public int getX() {
                return Display.displayX;
            }

            @Override
            public int getWidth() {
                return Display.latestWidth;
            }

            @Override
            public String getVersion() {
                return Sys.getVersion();
            }

            @Override
            public float getPixelScaleFactor() {
                return 0;
            }

            @Override
            public int getPbufferCapabilities() {
                return 0;
            }

            @Override
            public int getHeight() {
                return Display.latestHeight;
            }

            @Override
            public int getGammaRampLength() {
                return 0;
            }

            @Override
            public DisplayMode[] getAvailableDisplayModes() throws LWJGLException {
                return Display.getAvailableDisplayModes();
            }

            @Override
            public String getAdapter() {
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

    public static void setLocation(int new_x, int new_y) {
        if (Window.handle == NULL) {
            Display.displayX = new_x;
            Display.displayY = new_y;
        } else {
            GLFW.glfwSetWindowPos(Window.handle, new_x, new_y);
        }
    }

    public static void setVSyncEnabled(boolean sync) {
        vsyncEnabled = sync;
        glfwSwapInterval(vsyncEnabled ? 1 : 0);
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
    
    /** Return the last parent set with setParent(). */
    @Nullable
    public static Canvas getParent() {
        return parent;
    }

    /**
     * Set the parent of the Display. If parent is null, the Display will appear as a top level window.
     * If parent is not null, the Display is made a child of the parent. A parent's isDisplayable() must be true when
     * setParent() is called and remain true until setParent() is called again with
     * null or a different parent. This generally means that the parent component must remain added to it's parent container.<p>
     * It is not advisable to call this method from an AWT thread, since the context will be made current on the thread
     * and it is difficult to predict which AWT thread will process any given AWT event.<p>
     * While the Display is in fullscreen mode, the current parent will be ignored. Additionally, when a non null parent is specified,
     * the Dispaly will inherit the size of the parent, disregarding the currently set display mode.<p>
     */
    public static void setParent(Canvas parent) throws LWJGLException {
        if ( Display.parent != parent ) {
            Display.parent = parent;
            /*
            if ( !isCreated() )
                return;
            destroyWindow();
            try {
                if ( isFullscreen() ) {
                    switchDisplayMode();
                } else {
                    display_impl.resetDisplayMode();
                }
                createWindow();
                makeCurrentAndSetSwapInterval();
            } catch (LWJGLException e) {
                drawable.destroy();
                display_impl.resetDisplayMode();
                throw e;
            }
            */
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
        if (dm.equals(mode)) return;
        boolean attrChange = dm.lwjglxAttrChange(mode);
        mode = dm;
        if (Window.handle == NULL) return; // LWJGLX - Allow setDisplayMode even when display is not yet created.
        boolean fullscreen = displayFullscreen && dm.isFullscreenCapable();
        if (LWJGLXHelper.resizeRecreateDisplay || (attrChange && LWJGLXHelper.attrRecreateDisplay)) {
            newCurrentWindow(GLFW.glfwCreateWindow(dm.getWidth(), dm.getHeight(), windowTitle,
                    fullscreen ? glfwGetPrimaryMonitor() : NULL, NULL));
            displayFullscreen = fullscreen;
        } else {
            GLFW.glfwSetWindowSize(Window.handle, dm.getWidth(), dm.getHeight());
        }
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

    public static String getTitle() {
        return windowTitle;
    }

    public static void setTitle(String title) {
        windowTitle = title;
    }

    public static boolean isCloseRequested() {
        return Window.handle != MemoryUtil.NULL && glfwWindowShouldClose(Window.handle);
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
        if (LWJGLXHelper.disableWindowIcon) return 0;
        try {
            if (Window.handle == MemoryUtil.NULL) {
                Display.icons = lwjglxToGLFWIcons(icons);
            } else {
                lwjglxSetWindowIcons(lwjglxToGLFWIcons(icons));
            }
        } catch (NullPointerException e) {
            LWJGLUtil.log("Couldn't set icon");
            e.printStackTrace();
        }
        return 0;
    }

    public static void setResizable(boolean resizable) {
        if (displayResizable ^ resizable) {
            if (Window.handle != MemoryUtil.NULL) {
                IntBuffer width = BufferUtils.createIntBuffer(1);
                IntBuffer height = BufferUtils.createIntBuffer(1);
                GLFW.glfwGetWindowSize(Window.handle, width, height);
                width.rewind();
                height.rewind();

                GLFW.glfwDefaultWindowHints();
                glfwWindowHint(GLFW_VISIBLE, displayVisible ? GL_TRUE : GL_FALSE);
                glfwWindowHint(GLFW_RESIZABLE, displayResizable ? GL_TRUE : GL_FALSE);
                glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GL_TRUE);

                if (LWJGLXHelper.attrRecreateDisplay) {
                    newCurrentWindow(GLFW.glfwCreateWindow(width.get(), height.get(), windowTitle,
                            GLFW.glfwGetWindowMonitor(Window.handle), NULL));
                } else {
                    GLFW.glfwSetWindowSize(Window.handle, width.get(), height.get());
                }
            }
        }
        displayResizable = resizable;
    }

    public static boolean isResizable() {
        return displayResizable;
    }

    public static void setDisplayModeAndFullscreen(DisplayMode dm) throws LWJGLException {
        boolean fullScreenChange = (dm.isFullscreenCapable() != displayFullscreen);
        if (dm.equals(mode) && !fullScreenChange) return;
        boolean attrChange = dm.lwjglxAttrChange(mode) || fullScreenChange;
        mode = dm;
        if (LWJGLXHelper.resizeRecreateDisplay || (attrChange && LWJGLXHelper.attrRecreateDisplay)) {
            newCurrentWindow(glfwCreateWindow(dm.getWidth(), dm.getHeight(), windowTitle,
                    dm.isFullscreenCapable() ? glfwGetPrimaryMonitor() : NULL, NULL));
            displayFullscreen = dm.isFullscreenCapable();
        } else {
            GLFW.glfwSetWindowSize(Window.handle, dm.getWidth(), dm.getHeight());
        }
    }

    public static void setFullscreen(boolean fullscreen) throws LWJGLException {
        System.out.println("LWJGLX: switch fullscreen to " + fullscreen);
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

                if (LWJGLXHelper.resizeRecreateDisplay || LWJGLXHelper.attrRecreateDisplay) {
                    if (fullscreen)
                        newCurrentWindow(glfwCreateWindow(width.get(), height.get(), windowTitle,
                                glfwGetPrimaryMonitor(), NULL));
                    else
                        newCurrentWindow(glfwCreateWindow(width.get(), height.get(), windowTitle, NULL, NULL));
                } else {
                    GLFW.glfwSetWindowSize(Window.handle, width.get(), height.get());
                }
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
        if (!isCurrent()) {
            Display.drawable.makeCurrent();
        }
    }

    public static @Nullable java.lang.String getAdapter() {
        // Only returned non-null on Windows
        return null;
    }

    public static @Nullable java.lang.String getVersion() {
        // Only returned non-null on Windows
        return null;
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
        return drawable;
	}

    static DisplayImplementation getImplementation() {
        return display_impl;
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

        // glfwMakeContextCurrent(Window.handle);
        Objects.requireNonNull(org.lwjgl.opengl.GLContext.getCapabilities());

        glfwSwapInterval(0);
        glfwShowWindow(Window.handle);
    }

    private static GLFWIcons lwjglxToGLFWIcons(java.nio.ByteBuffer[] icons) {
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(GLFWImage.SIZEOF * icons.length);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        LongBuffer longBuffer = byteBuffer.asLongBuffer();
        for (int i = 0; i < icons.length; i++) {
            long pixelData;
            ByteBuffer icon = icons[i];
            try {
                pixelData = MemoryUtil.memAddress0((Buffer) icon);
            } catch (NoSuchMethodError error) {
                if (icon.position() != 0) {
                    ((Buffer) icon).position(0);
                }
                pixelData = MemoryUtil.memAddress((Buffer) icon);
            }
            int pixelCount = (icon.capacity() / 4);
            if (pixelData == MemoryUtil.NULL) {
                throw new RuntimeException("Invalid or null pixel data!");
            }
            int sideLen = (int) Math.floor(Math.sqrt(pixelCount));
            if (sideLen * sideLen != pixelCount) {
                throw new RuntimeException("Only supports square icons!");
            }
            int off;
            if (Pointer.POINTER_SIZE == 8) {
                off = (i * 4);
                longBuffer.put(1 + (i * 2), pixelData);
            } else if (Pointer.POINTER_SIZE == 4) {
                off = (i * 3);
                intBuffer.put(2 + off, (int) (pixelData & 0xFFFFFFFFL));
            } else {
                throw new RuntimeException("Unsupported pointer size: " + Pointer.POINTER_SIZE);
            }
            intBuffer.put(off, sideLen);
            intBuffer.put(off + 1, sideLen);
        }
        ((Buffer) byteBuffer).position(0);
        return new GLFWIcons(byteBuffer);
    }

    private static void lwjglxSetWindowIcons(GLFWIcons icons) {
        if (LWJGLXHelper.disableWindowIcon) return;
        if (icons.address() != MemoryUtil.NULL && Window.handle != MemoryUtil.NULL) {
            GLFWImage.Buffer oldBuffer = Display.activeIcons;
            glfwSetWindowIcon(Window.handle, icons);
            Display.activeIcons = icons;
            if (oldBuffer != icons &&
                    oldBuffer != null) {
                oldBuffer.free();
            }
        }
    }

    private static void lwjglxSetDefaultIcons() {
        setIcon(new ByteBuffer[] { LWJGLUtil.LWJGLIcon32x32, LWJGLUtil.LWJGLIcon16x16 });
    }

    // Keep track if the icons have been freed
    static class GLFWIcons extends GLFWImage.Buffer {
        private boolean freed;

        public GLFWIcons(ByteBuffer container) {
            super(container);
        }

        @Override
        public long address() {
            return this.freed ? MemoryUtil.NULL : super.address();
        }

        @Override
        public void free() {
            if (!this.freed) {
                this.freed = true;
                super.free();
            }
        }
    }

    static class Window {
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
