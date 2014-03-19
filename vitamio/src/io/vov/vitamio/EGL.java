/*
 * Copyright (C) 2013 YIXIA.COM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.vov.vitamio;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;
import android.util.Log;
import android.view.Surface;


/**
 * DON'T MODIFY THIS FILE IF YOU'RE NOT FAMILIAR WITH EGL, IT'S USED BY NATIVE CODE!!!
 */
public class EGL {
  private EGL10 mEgl;
  private EGLDisplay mEglDisplay;
  private EGLSurface mEglSurface;
  private EGLConfig mEglConfig;
  private EGLContext mEglContext;
  private EGLConfigChooser mEGLConfigChooser;
  private EGLContextFactory mEGLContextFactory;
  private EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;

  public EGL() {
    mEGLConfigChooser = new SimpleEGLConfigChooser();
    mEGLContextFactory = new EGLContextFactory();
    mEGLWindowSurfaceFactory = new EGLWindowSurfaceFactory();
  }

  public boolean initialize(Surface surface) {
    start();
    return createSurface(surface) != null;
  }

  public void release() {
    destroySurface();
    finish();
  }

  public void start() {
    mEgl = (EGL10) EGLContext.getEGL();
    mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

    if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
      throw new RuntimeException("eglGetDisplay failed");
    }

    int[] version = new int[2];
    if (!mEgl.eglInitialize(mEglDisplay, version)) {
      throw new RuntimeException("eglInitialize failed");
    }
    mEglConfig = mEGLConfigChooser.chooseConfig(mEgl, mEglDisplay);

    mEglContext = mEGLContextFactory.createContext(mEgl, mEglDisplay, mEglConfig);
    if (mEglContext == null || mEglContext == EGL10.EGL_NO_CONTEXT) {
      mEglContext = null;
      throwEglException("createContext");
    }

    mEglSurface = null;
  }

  public GL createSurface(Surface surface) {
    if (mEgl == null)
      throw new RuntimeException("egl not initialized");
    if (mEglDisplay == null)
      throw new RuntimeException("eglDisplay not initialized");
    if (mEglConfig == null)
      throw new RuntimeException("mEglConfig not initialized");

    if (mEglSurface != null && mEglSurface != EGL10.EGL_NO_SURFACE) {
      mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
      mEGLWindowSurfaceFactory.destroySurface(mEgl, mEglDisplay, mEglSurface);
    }

    mEglSurface = mEGLWindowSurfaceFactory.createWindowSurface(mEgl, mEglDisplay, mEglConfig, surface);

    if (mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE) {
      int error = mEgl.eglGetError();
      if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
        Log.e("EglHelper", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
        return null;
      }
      throwEglException("createWindowSurface", error);
    }

    if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
      throwEglException("eglMakeCurrent");
    }

    GL gl = mEglContext.getGL();

    return gl;
  }

  public boolean swap() {
    if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
      int error = mEgl.eglGetError();
      switch (error) {
        case EGL11.EGL_CONTEXT_LOST:
          return false;
        case EGL10.EGL_BAD_NATIVE_WINDOW:
          Log.e("EglHelper", "eglSwapBuffers returned EGL_BAD_NATIVE_WINDOW. tid=" + Thread.currentThread().getId());
          break;
        case EGL10.EGL_BAD_SURFACE:
          Log.e("EglHelper", "eglSwapBuffers returned EGL_BAD_SURFACE. tid=" + Thread.currentThread().getId());
          return false;
        default:
          throwEglException("eglSwapBuffers", error);
      }
    }
    return true;
  }

  public void destroySurface() {
    if (mEglSurface != null && mEglSurface != EGL10.EGL_NO_SURFACE) {
      mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
      mEGLWindowSurfaceFactory.destroySurface(mEgl, mEglDisplay, mEglSurface);
      mEglSurface = null;
    }
  }

  public void finish() {
    if (mEglContext != null) {
      mEGLContextFactory.destroyContext(mEgl, mEglDisplay, mEglContext);
      mEglContext = null;
    }
    if (mEglDisplay != null) {
      mEgl.eglTerminate(mEglDisplay);
      mEglDisplay = null;
    }
  }

  private void throwEglException(String function) {
    throwEglException(function, mEgl.eglGetError());
  }

  private void throwEglException(String function, int error) {
    String message = String.format("%s failed: %x", function, error);
    Log.e("EglHelper", "throwEglException tid=" + Thread.currentThread().getId() + " " + message);
    throw new RuntimeException(message);
  }

  private static class EGLWindowSurfaceFactory {

    public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display, EGLConfig config, Object nativeWindow) {
      return egl.eglCreateWindowSurface(display, config, nativeWindow, null);
    }

    public void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface) {
      egl.eglDestroySurface(display, surface);
    }
  }

  private class EGLContextFactory {
    private int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
      int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};

      return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, attrib_list);
    }

    public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
      if (!egl.eglDestroyContext(display, context)) {
        Log.e("DefaultContextFactory", "display:" + display + " context: " + context);
        throw new RuntimeException("eglDestroyContext failed: ");
      }
    }
  }

  private abstract class EGLConfigChooser {
    protected int[] mConfigSpec;

    public EGLConfigChooser(int[] configSpec) {
      mConfigSpec = filterConfigSpec(configSpec);
    }

    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
      int[] num_config = new int[1];
      if (!egl.eglChooseConfig(display, mConfigSpec, null, 0, num_config)) {
        throw new IllegalArgumentException("eglChooseConfig failed");
      }

      int numConfigs = num_config[0];

      if (numConfigs <= 0) {
        throw new IllegalArgumentException("No configs match configSpec");
      }

      EGLConfig[] configs = new EGLConfig[numConfigs];
      if (!egl.eglChooseConfig(display, mConfigSpec, configs, numConfigs, num_config)) {
        throw new IllegalArgumentException("eglChooseConfig#2 failed");
      }
      EGLConfig config = chooseConfig(egl, display, configs);
      if (config == null) {
        throw new IllegalArgumentException("No config chosen");
      }
      return config;
    }

    abstract EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs);

    private int[] filterConfigSpec(int[] configSpec) {
      int len = configSpec.length;
      int[] newConfigSpec = new int[len + 2];
      System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1);
      newConfigSpec[len - 1] = EGL10.EGL_RENDERABLE_TYPE;
      newConfigSpec[len] = 4; /* EGL_OPENGL_ES2_BIT */
      newConfigSpec[len + 1] = EGL10.EGL_NONE;
      return newConfigSpec;
    }
  }

  private class ComponentSizeChooser extends EGLConfigChooser {
    protected int mRedSize;
    protected int mGreenSize;
    protected int mBlueSize;
    protected int mAlphaSize;
    protected int mDepthSize;
    protected int mStencilSize;
    private int[] mValue;

    public ComponentSizeChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize) {
      super(new int[]{EGL10.EGL_RED_SIZE, redSize, EGL10.EGL_GREEN_SIZE, greenSize, EGL10.EGL_BLUE_SIZE, blueSize, EGL10.EGL_ALPHA_SIZE, alphaSize, EGL10.EGL_DEPTH_SIZE, depthSize, EGL10.EGL_STENCIL_SIZE, stencilSize, EGL10.EGL_NONE});
      mValue = new int[1];
      mRedSize = redSize;
      mGreenSize = greenSize;
      mBlueSize = blueSize;
      mAlphaSize = alphaSize;
      mDepthSize = depthSize;
      mStencilSize = stencilSize;
    }

    @Override
    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs) {
      for (EGLConfig config : configs) {
        int d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
        int s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);
        if ((d >= mDepthSize) && (s >= mStencilSize)) {
          int r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
          int g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
          int b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
          int a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);
          if ((r == mRedSize) && (g == mGreenSize) && (b == mBlueSize) && (a == mAlphaSize)) {
            return config;
          }
        }
      }
      return null;
    }

    private int findConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute, int defaultValue) {

      if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
        return mValue[0];
      }
      return defaultValue;
    }
  }

  private class SimpleEGLConfigChooser extends ComponentSizeChooser {
    public SimpleEGLConfigChooser() {
      super(5, 6, 5, 0, 0, 0);
    }
  }

}
