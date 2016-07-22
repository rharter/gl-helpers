package com.ryanharter.android.gl;

import android.opengl.GLES20;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_VERSION;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES30.glBindVertexArray;

public final class GLState {

  private static boolean blend = false;
  private static int program = -1;
  private static int textureUnit = -1;
  private static int arrayBuffer = -1;
  private static int elementArrayBuffer = -1;
  private static int vertexArray = -1;
  private static SparseIntArray textures = new SparseIntArray();
  private static SparseBooleanArray attributes = new SparseBooleanArray();

  private GLState() { }

  public enum GLVersion {
    GLES_20, GLES_30, GL_UNKNOWN
  }

  public static GLVersion getGlVersion() {
    String version = GLES20.glGetString(GL_VERSION);
    if (version.startsWith("OpenGL ES 2")) {
      return GLVersion.GLES_20;
    } else if (version.startsWith("OpenGL ES 3")) {
      return GLVersion.GLES_30;
    } else {
      return GLVersion.GL_UNKNOWN;
    }
  }

  public static void reset() {
    blend = false;
    program = -1;
    textureUnit = -1;
    arrayBuffer = -1;
    elementArrayBuffer = -1;
    vertexArray = -1;
    textures.clear();
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

  public static boolean bindVertexArray(int array) {
    if (vertexArray != array) {
      glBindVertexArray(array);
      vertexArray = array;
      return true;
    }
    return false;
  }
}
