/*
 * Copyright (c) 2002-2008 LWJGL Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.lwjgl.input;

import java.util.ArrayList;

import org.lwjgl.LWJGLException;

/**
 * The collection of controllers currently connected.
 *
 * @author Kevin Glass
 */
public class Controllers {
    /** The current list of events */
    private static ArrayList<ControllerEvent> events = new ArrayList<ControllerEvent>();
    /** The current event */
    private static ControllerEvent event;

    /** Whether controllers were created */
    private static boolean created;

    private static boolean lwjglxHasJInput;

    /**
     * Initialise the controllers collection
     *
     * @throws LWJGLException Indicates a failure to initialise the controller library.
     */
    public static void create() throws LWJGLException {
        if (created && !lwjglxHasJInput) {
            return;
        }
        created = true;
        try {
            ControllersJInput.create();
            lwjglxHasJInput = true;
        } catch (LWJGLException e) {
            lwjglxHasJInput = true;
            throw e;
        } catch (Throwable ignored) {
            lwjglxHasJInput = false;
        }
    }

    /**
     * Get a controller from the collection
     *
     * @param index The index of the controller to retrieve
     * @return The controller requested
     */
    public static Controller getController(int index) {
        if (lwjglxHasJInput) {
            return ControllersJInput.getController(index);
        }
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: 0");
    }

    /**
     * Retrieve a count of the number of controllers
     *
     * @return The number of controllers available
     */
    public static int getControllerCount() {
        if (lwjglxHasJInput) {
            return ControllersJInput.getControllerCount();
        }
        return 0;
    }

    /**
     * Poll the controllers available. This will both update their state
     * and generate events that must be cleared.
     */
    public static void poll() {
        if (lwjglxHasJInput) {
            ControllersJInput.poll();
        }
    }

    /**
     * Clear any events stored for the controllers in this set
     */
    public static void clearEvents() {
        events.clear();
    }

    /**
     * Move to the next event that has been stored.
     *
     * @return True if there is still an event to process
     */
    public static boolean next() {
        if (events.isEmpty()) {
            event = null;
            return false;
        }

        event = events.remove(0);

        return event != null;
    }

    /**
     * @return True if Controllers has been created
     */
    public static boolean isCreated() {
        return lwjglxHasJInput ? ControllersJInput.isCreated() : created;
    }

    /**
     * Destroys any resources used by the controllers
     */
    public static void destroy() {
        if (lwjglxHasJInput) {
            ControllersJInput.destroy();
        }
    }

    /**
     * Get the source of the current event
     *
     * @return The source of the current event
     */
    public static Controller getEventSource() {
        return event.getSource();
    }

    /**
     * Get the index of the control that caused the current event
     *
     * @return The index of the control that cause the current event
     */
    public static int getEventControlIndex() {
        return event.getControlIndex();
    }

    /**
     * Check if the current event was caused by a button
     *
     * @return True if the current event was caused by a button
     */
    public static boolean isEventButton() {
        return event.isButton();
    }

    /**
     * Check if the current event was caused by a axis
     *
     * @return True if the current event was caused by a axis
     */
    public static boolean isEventAxis() {
        return event.isAxis();
    }

    /**
     * Check if the current event was caused by movement on the x-axis
     *
     * @return True if the current event was cause by movement on the x-axis
     */
    public static boolean isEventXAxis() {
        return event.isXAxis();
    }

    /**
     * Check if the current event was caused by movement on the y-axis
     *
     * @return True if the current event was caused by movement on the y-axis
     */
    public static boolean isEventYAxis() {
        return event.isYAxis();
    }

    /**
     * Check if the current event was cause by the POV x-axis
     *
     * @return True if the current event was caused by the POV x-axis
     */
    public static boolean isEventPovX() {
        return event.isPovX();
    }

    /**
     * Check if the current event was cause by the POV x-axis
     *
     * @return True if the current event was caused by the POV x-axis
     */
    public static boolean isEventPovY() {
        return event.isPovY();
    }

    /**
     * Get the timestamp assigned to the current event
     *
     * @return The timestamp assigned to the current event
     */
    public static long getEventNanoseconds() {
        return event.getTimeStamp();
    }

    /**
     * Gets the state of the button that generated the current event
     *
     * @return True if button was down, or false if released
     */
    public static boolean getEventButtonState() {
        return event.getButtonState();
    }

    /**
     * Get the value on an X axis of the current event
     *
     * @return The value on a x axis of the current event
     */
    public static float getEventXAxisValue() {
        return event.getXAxisValue();
    }

    /**
     * Get the value on an Y axis of the current event
     *
     * @return The value on a y axis of the current event
     */
    public static float getEventYAxisValue() {
        return event.getYAxisValue();
    }

    /**
     * Add an event to the stack of events that have been caused
     *
     * @param event The event to add to the list
     */
    static void addEvent(ControllerEvent event) {
        if (event != null) {
            events.add(event);
        }
    }
}
