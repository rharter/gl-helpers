package com.ryanharter.android.gl

import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.glDeleteTextures
import android.opengl.GLES20.glGenTextures

/**
 * Created by rharter on 4/9/14.
 */
open class Texture {

  private var bindUnit = -1

  val name: Int

  init {
    val tmp = IntArray(1)
    glGenTextures(1, tmp, 0)
    name = tmp[0]
  }

  open fun bind(unit: Int) {
    bindUnit = unit
    GLState.bindTexture(unit, GL_TEXTURE_2D, name)
  }

  open fun unbind() {
    if (bindUnit > -1) {
      GLState.bindTexture(bindUnit, GL_TEXTURE_2D, 0)
      bindUnit = -1
    }
  }

  open fun destroy() {
    glDeleteTextures(1, intArrayOf(name), 0)
  }

}
