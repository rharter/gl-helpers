package com.ryanharter.android.gl

import android.graphics.Canvas
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20.GL_CLAMP_TO_EDGE
import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_TEXTURE_MAG_FILTER
import android.opengl.GLES20.GL_TEXTURE_MIN_FILTER
import android.opengl.GLES20.GL_TEXTURE_WRAP_S
import android.opengl.GLES20.GL_TEXTURE_WRAP_T
import android.opengl.GLES20.glActiveTexture
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glTexParameteri
import android.view.Surface

open class CanvasTexture : Texture() {

  private var surfaceTexture: SurfaceTexture? = null
  private var surface: Surface? = null
  private var bindUnit = -1

  private var cachedCanvas: Canvas? = null

  init {
    glActiveTexture(0)
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, name)

    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

    glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0)
  }

  override fun bind(unit: Int) {
    bindUnit = unit
    GLState.bindTexture(unit, GL_TEXTURE_EXTERNAL_OES, name)
  }

  override fun unbind() {
    if (bindUnit != -1) {
      GLState.bindTexture(bindUnit, GL_TEXTURE_EXTERNAL_OES, 0)
      bindUnit = -1
    }
  }

  fun draw(width: Int, height: Int, body: Canvas.() -> Unit) {
    val cached = cachedCanvas
    val canvas = if (cached != null && cached.width == width && cached.height == height) {
      cached
    } else {
      beginDrawing(width, height).also {
        cachedCanvas = it
      }
    }

    canvas.body()
    endDrawing(canvas)
  }

  fun beginDrawing(width: Int, height: Int): Canvas {
    surfaceTexture = SurfaceTexture(name).apply { setDefaultBufferSize(width, height) }
    surface = Surface(surfaceTexture)
    return surface!!.lockCanvas(null)
  }

  fun endDrawing(canvas: Canvas) {
    surface?.unlockCanvasAndPost(canvas)
    surfaceTexture?.updateTexImage()
    release()
  }

  fun release() {
    cachedCanvas = null
    surfaceTexture?.release()
    surfaceTexture = null
    surface?.release()
    surface = null
  }
}