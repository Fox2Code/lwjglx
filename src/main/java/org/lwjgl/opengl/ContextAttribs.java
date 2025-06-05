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
package org.lwjgl.opengl;

public final class ContextAttribs {
	public static final int CONTEXT_MAJOR_VERSION_ARB = 0x2091;
	public static final int CONTEXT_MINOR_VERSION_ARB = 0x2092;

	public static final int CONTEXT_PROFILE_MASK_ARB = 0x9126,
			CONTEXT_CORE_PROFILE_BIT_ARB                 = 0x00000001,
			CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB        = 0x00000002,
			CONTEXT_ES2_PROFILE_BIT_EXT                  = 0x00000004;

	public static final int CONTEXT_FLAGS_ARB = 0x2094,
			CONTEXT_DEBUG_BIT_ARB                 = 0x0001,
			CONTEXT_FORWARD_COMPATIBLE_BIT_ARB    = 0x0002,
			CONTEXT_ROBUST_ACCESS_BIT_ARB         = 0x00000004,
			CONTEXT_RESET_ISOLATION_BIT_ARB       = 0x00000008;

	public static final int CONTEXT_RESET_NOTIFICATION_STRATEGY_ARB = 0x8256,
			NO_RESET_NOTIFICATION_ARB                                   = 0x8261,
			LOSE_CONTEXT_ON_RESET_ARB                                   = 0x8252;

	public static final int CONTEXT_RELEASE_BEHABIOR_ARB = 0x2097,
			CONTEXT_RELEASE_BEHAVIOR_NONE_ARB                = 0x0000,
			CONTEXT_RELEASE_BEHAVIOR_FLUSH_ARB               = 0x2098;

	public static final int CONTEXT_LAYER_PLANE_ARB = 0x2093;

	private int majorVersion;
	private int minorVersion;

	private int profileMask;
	private int contextFlags;

	public ContextAttribs() {
		this(1, 0, 0, 0);
	}

	public ContextAttribs(final int majorVersion, final int minorVersion) {
		this(majorVersion, minorVersion, 0, 0);
	}
	public ContextAttribs(int majorVersion, int minorVersion, int profileMask, int contextFlags) {
		this.majorVersion = majorVersion;
		this.minorVersion = minorVersion;

		this.profileMask = profileMask;
		this.contextFlags = contextFlags;
	}

	private ContextAttribs(ContextAttribs other) {
		this.majorVersion = other.majorVersion;
		this.minorVersion = other.minorVersion;

		this.profileMask = other.profileMask;
		this.contextFlags = other.contextFlags;
	}

	public int getMajorVersion() {
		return this.majorVersion;
	}

	public int getMinorVersion() {
		return this.minorVersion;
	}

	public int getProfileMask() {
		return this.profileMask;
	}

	public int getContextFlags() {
		return this.contextFlags;
	}

	public int getLayerPlane() {
		return 0;
	}

	public boolean isDebug() {
		return (this.contextFlags & CONTEXT_DEBUG_BIT_ARB) != 0;
	}

	public boolean isForwardCompatible() {
		return (this.contextFlags & CONTEXT_FORWARD_COMPATIBLE_BIT_ARB) != 0;
	}

	public boolean isRobustAccess() {
		return (this.contextFlags & CONTEXT_ROBUST_ACCESS_BIT_ARB) != 0;
	}

	public boolean isContextResetIsolation() {
		return (this.contextFlags & CONTEXT_RESET_ISOLATION_BIT_ARB) != 0;
	}

	public boolean isProfileCore() {
		return this.profileMask == CONTEXT_CORE_PROFILE_BIT_ARB;
	}

	public boolean isProfileCompatibility() {
		return this.profileMask == CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB;
	}

	public boolean isProfileES() {
		return this.profileMask == CONTEXT_ES2_PROFILE_BIT_EXT;
	}

	public ContextAttribs withLayer(final int layerPlane) {
		return this;
	}

	public ContextAttribs withDebug(final boolean debug) {
		return this.lwjglxSetContextFlag(CONTEXT_DEBUG_BIT_ARB, debug);
	}

	public ContextAttribs withForwardCompatible(final boolean forwardCompatible) {
		return this.lwjglxSetContextFlag(CONTEXT_FORWARD_COMPATIBLE_BIT_ARB, forwardCompatible);
	}

	public ContextAttribs withRobustAccess(final boolean robustAccess) {
		return this.lwjglxSetContextFlag(CONTEXT_ROBUST_ACCESS_BIT_ARB, robustAccess);
	}

	public ContextAttribs withProfileCore(final boolean profileCore) {
		return this.lwjglxSetProfileMask(profileCore ?
				CONTEXT_CORE_PROFILE_BIT_ARB : 0);
	}

	public ContextAttribs withProfileCompatibility(final boolean profileCompatibility) {
		return this.lwjglxSetProfileMask(profileCompatibility ?
				CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB : 0);
	}

	public ContextAttribs withProfileES(final boolean profileES) {
		return this.lwjglxSetProfileMask(profileES ?
				CONTEXT_ES2_PROFILE_BIT_EXT : 0);
	}

	public ContextAttribs withLoseContextOnReset(final boolean loseContextOnReset) {
		return this;
	}

	public ContextAttribs withContextResetIsolation(final boolean contextResetIsolation) {
		return this.lwjglxSetContextFlag(CONTEXT_RESET_ISOLATION_BIT_ARB, contextResetIsolation);
	}

	private ContextAttribs lwjglxSetProfileMask(int profileMask) {
		if (this.profileMask == profileMask) {
			return this;
		}
		ContextAttribs contextAttribs = new ContextAttribs(this);
		contextAttribs.profileMask = profileMask;
		return contextAttribs;
	}

	private ContextAttribs lwjglxSetContextFlag(int flag, boolean enable) {
		int newFlags = this.contextFlags;
		if (enable) {
			newFlags |= flag;
		} else {
			newFlags &= ~flag;
		}
		if (newFlags == this.contextFlags) {
			return this;
		}
		ContextAttribs contextAttribs = new ContextAttribs(this);
		contextAttribs.contextFlags = newFlags;
		return contextAttribs;
	}

	public String toString() {
		return "ContextAttribs";
	}

}
