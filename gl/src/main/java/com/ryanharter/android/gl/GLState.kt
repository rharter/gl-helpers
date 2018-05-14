package com.ryanharter.android.gl

import android.opengl.GLES20
import android.opengl.GLES20.GL_ARRAY_BUFFER
import android.opengl.GLES20.GL_BLEND
import android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER
import android.opengl.GLES20.GL_FRAMEBUFFER
import android.opengl.GLES20.GL_MAX_TEXTURE_SIZE
import android.opengl.GLES20.GL_ONE
import android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA
import android.opengl.GLES20.GL_TEXTURE0
import android.opengl.GLES20.GL_VERSION
import android.opengl.GLES20.GL_VIEWPORT
import android.opengl.GLES20.glActiveTexture
import android.opengl.GLES20.glBindBuffer
import android.opengl.GLES20.glBindFramebuffer
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glBlendFunc
import android.opengl.GLES20.glDisable
import android.opengl.GLES20.glDisableVertexAttribArray
import android.opengl.GLES20.glEnable
import android.opengl.GLES20.glEnableVertexAttribArray
import android.opengl.GLES20.glGetIntegerv
import android.opengl.GLES20.glUseProgram
import android.opengl.GLES20.glViewport
import android.opengl.GLES30.glBindVertexArray
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.SparseArray
import android.util.SparseBooleanArray
import android.util.SparseIntArray
import java.util.Arrays

object GLState {

  var logger: Logger = Logger.VoidLogger()

  // TODO choose the best renderer based on env
  private val renderer = GLES2Renderer()

  private var glVersion = GLVersion.GL_UNKNOWN
  private var maxTextureSize = -1
  private var viewport = IntArray(4)
  private var blend = false
  private var program = -1
  private var textureUnit = -1
  private var framebuffer = -1
  private var arrayBuffer = -1
  private var elementArrayBuffer = -1
  private var vertexArray = -1
  private val textures = SparseArray<SparseIntArray>()
  private val attributes = SparseBooleanArray()
  private val resetListeners = mutableSetOf<() -> Unit>()

  private val tempInt = IntArray(16)

  fun addResetListener(l: () -> Unit) {
    resetListeners.add(l)
  }

  fun removeResetListener(l: () -> Unit) {
    resetListeners.remove(l)
  }

  enum class GLVersion {
    GLES_20, GLES_30, GL_UNKNOWN
  }

  fun getGlVersion(): GLVersion {
    if (glVersion == GLVersion.GL_UNKNOWN) {
      val version = GLES20.glGetString(GL_VERSION)
      return if (version != null && version.startsWith("OpenGL ES 2.")) {
        GLVersion.GLES_20
      } else if (version != null && version.startsWith("OpenGL ES 3.")) {
        GLVersion.GLES_30
      } else {
        GLVersion.GL_UNKNOWN
      }
    }
    return glVersion
  }

  fun getMaxTextureSize(): Int {
    if (maxTextureSize < 0) {
      glGetIntegerv(GL_MAX_TEXTURE_SIZE, tempInt, 0)
      maxTextureSize = tempInt[0]
    }
    return maxTextureSize
  }

  fun getViewport(viewport: IntArray) {
    if (GLState.viewport[0] == 0 && GLState.viewport[1] == 0 && GLState.viewport[2] == 0 && GLState.viewport[3] == 0) {
      glGetIntegerv(GL_VIEWPORT, viewport, 0)
    } else {
      viewport[0] = GLState.viewport[0]
      viewport[1] = GLState.viewport[1]
      viewport[2] = GLState.viewport[2]
      viewport[3] = GLState.viewport[3]
    }
  }

  fun getViewport(): IntArray {
    if (viewport[0] == 0 && viewport[1] == 0 && viewport[2] == 0 && viewport[3] == 0) {
      glGetIntegerv(GL_VIEWPORT, viewport, 0)
    }
    return viewport
  }

  fun setViewport(x: Int, y: Int, w: Int, h: Int) {
    viewport = intArrayOf(x, y, w, h)
    glViewport(x, y, w, h)
  }

  fun reset() {
    logger.log("Resetting state.")
    glVersion = GLVersion.GL_UNKNOWN
    maxTextureSize = -1
    blend = false
    program = -1
    textureUnit = -1
    framebuffer = -1
    arrayBuffer = -1
    elementArrayBuffer = -1
    vertexArray = -1
    textures.clear()
    attributes.clear()
    Arrays.fill(viewport, 0)
    Program.programs.clear()
    resetListeners.forEach { it() }
  }

  /**
   * Renders the current GL state to the active framebuffer.
   */
  fun render() {
    renderer.render()
  }

  fun useProgram(program: Int) {
    if (program != GLState.program) {
      glUseProgram(program)
      GLState.program = program
    }
  }

  fun setTextureUnit(textureUnit: Int) {
    if (textureUnit != GLState.textureUnit) {
      glActiveTexture(GL_TEXTURE0 + textureUnit)
      GLState.textureUnit = textureUnit
    }
  }

  fun bindTexture(unit: Int, target: Int, texture: Int) {
    var cache: SparseIntArray? = textures.get(target)
    if (cache == null) {
      cache = SparseIntArray()
      textures.put(target, cache)
    }

    if (cache.get(unit) != texture) {
      setTextureUnit(unit)
      glBindTexture(target, texture)
      cache.put(unit, texture)
    }
  }

  fun bindFramebuffer(framebuffer: Int) {
    if (GLState.framebuffer != framebuffer) {
      glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)
      GLState.framebuffer = framebuffer
    }
  }

  fun setBlend(blend: Boolean, translucent: Boolean) {
    if (blend != GLState.blend) {
      if (blend) {
        glEnable(GL_BLEND)
        if (translucent) {
          glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
        } else {
          glBlendFunc(GL_ONE, GL_ONE)
        }
      } else {
        glDisable(GL_BLEND)
      }
      GLState.blend = blend
    }
  }

  fun setAttributeEnabled(index: Int, enabled: Boolean) {
    if (attributes.get(index) != enabled) {
      if (enabled) {
        glEnableVertexAttribArray(index)
      } else {
        glDisableVertexAttribArray(index)
      }
      attributes.put(index, enabled)
    }
  }

  fun bindArrayBuffer(buffer: Int): Boolean {
    if (arrayBuffer != buffer) {
      glBindBuffer(GL_ARRAY_BUFFER, buffer)
      arrayBuffer = buffer
      return true
    }
    return false
  }

  fun bindElementArrayBuffer(buffer: Int): Boolean {
    if (elementArrayBuffer != buffer) {
      glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffer)
      elementArrayBuffer = buffer
      return true
    }
    return false
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  fun bindVertexArray(array: Int): Boolean {
    if (vertexArray != array) {
      glBindVertexArray(array)
      vertexArray = array
      return true
    }
    return false
  }
}
