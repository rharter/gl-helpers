package com.ryanharter.android.gl;

import android.opengl.GLES20;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import java.util.Arrays;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_MAX_TEXTURE_SIZE;
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_VERSION;
import static android.opengl.GLES20.GL_VIEWPORT;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetIntegerv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glViewport;
import static android.opengl.GLES30.glBindVertexArray;

public final class GLState {

  static Logger logger = new Logger.VoidLogger();

  // TODO choose the best renderer based on env
  private static Renderer renderer = new GLES2Renderer();

  private static GLVersion glVersion = GLVersion.GL_UNKNOWN;
  private static int maxTextureSize = -1;
  private static int[] viewport = new int[4];
  private static boolean blend = false;
  private static int program = -1;
  private static int textureUnit = -1;
  private static int framebuffer = -1;
  private static int arrayBuffer = -1;
  private static int elementArrayBuffer = -1;
  private static int vertexArray = -1;
  private static SparseIntArray textures = new SparseIntArray();
  private static SparseBooleanArray attributes = new SparseBooleanArray();

  private static int[] tempInt = new int[16];

  public static void setLogger(Logger logger) {
    GlUtil.logger = logger;
  }

  private GLState() { }

  public enum GLVersion {
    GLES_20, GLES_30, GL_UNKNOWN
  }

  public static GLVersion getGlVersion() {
    if (glVersion == GLVersion.GL_UNKNOWN) {
      String version = GLES20.glGetString(GL_VERSION);
      if (version != null && version.startsWith("OpenGL ES 2.")) {
        return GLVersion.GLES_20;
      } else if (version != null && version.startsWith("OpenGL ES 3.")) {
        return GLVersion.GLES_30;
      } else {
        return GLVersion.GL_UNKNOWN;
      }
    }
    return glVersion;
  }

  public static int getMaxTextureSize() {
    if (maxTextureSize < 0) {
      glGetIntegerv(GL_MAX_TEXTURE_SIZE, tempInt, 0);
      maxTextureSize = tempInt[0];
    }
    return maxTextureSize;
  }

  public static int[] getViewport() {
    if (viewport[0] == 0 && viewport[1] == 0 && viewport[2] == 0 && viewport[3] == 0) {
      glGetIntegerv(GL_VIEWPORT, viewport, 0);
    }
    return viewport;
  }

  public static void setViewport(int x, int y, int w, int h) {
    viewport = new int[] {x, y, w, h};
    glViewport(x, y, w, h);
  }

  public static void reset() {
    glVersion = GLVersion.GL_UNKNOWN;
    maxTextureSize = -1;
    blend = false;
    program = -1;
    textureUnit = -1;
    framebuffer = -1;
    arrayBuffer = -1;
    elementArrayBuffer = -1;
    vertexArray = -1;
    textures.clear();
    attributes.clear();
    Arrays.fill(viewport, 0);
    Program.programs.clear();
  }

  /**
   * Renders the current GL state to the active framebuffer.
   */
  public static void render() {
    renderer.render();
  }

  public static void useProgram(int program) {
    if (program != GLState.program) {
      glUseProgram(program);
      GLState.program = program;
    }
  }

  public static void setTextureUnit(int textureUnit) {
    if (textureUnit != GLState.textureUnit) {
      glActiveTexture(GL_TEXTURE0 + textureUnit);
      GLState.textureUnit = textureUnit;
    }
  }

  public static void bindTexture(int unit, int target, int texture) {
    if (textures.get(unit) != texture) {
      setTextureUnit(unit);
      glBindTexture(target, texture);
      textures.put(unit, texture);
    }
  }

  public static void bindFramebuffer(int framebuffer) {
    if (GLState.framebuffer != framebuffer) {
      glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
      GLState.framebuffer = framebuffer;
    }
  }

  public static void setBlend(boolean blend, boolean translucent) {
    if (blend != GLState.blend) {
      if (blend) {
        glEnable(GL_BLEND);
        if (translucent) {
          glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        } else {
          glBlendFunc(GL_ONE, GL_ONE);
        }
      } else {
        glDisable(GL_BLEND);
      }
      GLState.blend = blend;
    }
  }

  public static void setAttributeEnabled(int index, boolean enabled) {
    if (attributes.get(index) != enabled) {
      if (enabled) {
        glEnableVertexAttribArray(index);
      } else {
        glDisableVertexAttribArray(index);
      }
      attributes.put(index, enabled);
    }
  }

  public static boolean bindArrayBuffer(int buffer) {
    if (arrayBuffer != buffer) {
      glBindBuffer(GL_ARRAY_BUFFER, buffer);
      arrayBuffer = buffer;
      return true;
    }
    return false;
  }

  public static boolean bindElementArrayBuffer(int buffer) {
    if (elementArrayBuffer != buffer) {
      glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffer);
      elementArrayBuffer = buffer;
      return true;
    }
    return false;
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  public static boolean bindVertexArray(int array) {
    if (vertexArray != array) {
      glBindVertexArray(array);
      vertexArray = array;
      return true;
    }
    return false;
  }
}
