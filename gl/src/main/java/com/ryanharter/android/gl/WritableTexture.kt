package com.ryanharter.android.gl

import android.graphics.Bitmap
import android.opengl.GLES20.GL_CLAMP_TO_EDGE
import android.opengl.GLES20.GL_COLOR_ATTACHMENT0
import android.opengl.GLES20.GL_COLOR_BUFFER_BIT
import android.opengl.GLES20.GL_DEPTH_ATTACHMENT
import android.opengl.GLES20.GL_DEPTH_BUFFER_BIT
import android.opengl.GLES20.GL_DEPTH_COMPONENT16
import android.opengl.GLES20.GL_FLOAT
import android.opengl.GLES20.GL_FRAMEBUFFER
import android.opengl.GLES20.GL_FRAMEBUFFER_BINDING
import android.opengl.GLES20.GL_FRAMEBUFFER_COMPLETE
import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_RENDERBUFFER
import android.opengl.GLES20.GL_RGB
import android.opengl.GLES20.GL_RGBA
import android.opengl.GLES20.GL_STENCIL_ATTACHMENT
import android.opengl.GLES20.GL_STENCIL_BUFFER_BIT
import android.opengl.GLES20.GL_STENCIL_INDEX8
import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.GL_TEXTURE_MAG_FILTER
import android.opengl.GLES20.GL_TEXTURE_MIN_FILTER
import android.opengl.GLES20.GL_TEXTURE_WRAP_S
import android.opengl.GLES20.GL_TEXTURE_WRAP_T
import android.opengl.GLES20.GL_UNSIGNED_BYTE
import android.opengl.GLES20.GL_VIEWPORT
import android.opengl.GLES20.glBindBuffer
import android.opengl.GLES20.glBindFramebuffer
import android.opengl.GLES20.glBindRenderbuffer
import android.opengl.GLES20.glBufferData
import android.opengl.GLES20.glCheckFramebufferStatus
import android.opengl.GLES20.glClear
import android.opengl.GLES20.glClearColor
import android.opengl.GLES20.glClearStencil
import android.opengl.GLES20.glDeleteBuffers
import android.opengl.GLES20.glFramebufferRenderbuffer
import android.opengl.GLES20.glFramebufferTexture2D
import android.opengl.GLES20.glGenBuffers
import android.opengl.GLES20.glGenFramebuffers
import android.opengl.GLES20.glGenRenderbuffers
import android.opengl.GLES20.glGetError
import android.opengl.GLES20.glGetIntegerv
import android.opengl.GLES20.glReadPixels
import android.opengl.GLES20.glRenderbufferStorage
import android.opengl.GLES20.glTexImage2D
import android.opengl.GLES20.glTexParameteri
import android.opengl.GLES20.glViewport
import android.opengl.GLES30.GL_DYNAMIC_READ
import android.opengl.GLES30.GL_HALF_FLOAT
import android.opengl.GLES30.GL_PIXEL_PACK_BUFFER
import android.opengl.GLES30.GL_R16F
import android.opengl.GLES30.GL_R32F
import android.opengl.GLES30.GL_R8
import android.opengl.GLES30.GL_R8_SNORM
import android.opengl.GLES30.GL_RED
import android.opengl.GLES30.GL_RG
import android.opengl.GLES30.GL_RG16F
import android.opengl.GLES30.GL_RG32F
import android.opengl.GLES30.GL_RG8
import android.opengl.GLES30.GL_RG8_SNORM
import android.opengl.GLES30.GL_RGB16F
import android.opengl.GLES30.GL_RGB32F
import android.opengl.GLES30.GL_RGB8
import android.opengl.GLES30.GL_RGB8_SNORM
import android.opengl.GLES30.GL_RGBA16F
import android.opengl.GLES30.GL_RGBA32F
import android.opengl.GLES30.GL_RGBA8
import android.opengl.GLES30.GL_RGBA8_SNORM
import android.opengl.GLES30.GL_SRGB8
import android.opengl.GLES30.GL_SRGB8_ALPHA8
import android.opengl.GLES30.glMapBufferRange
import android.opengl.GLES30.glReadBuffer
import android.opengl.GLES30.glUnmapBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

private fun format(internalFormat: Int) = when (internalFormat) {
  GL_R8, GL_R8_SNORM, GL_R16F, GL_R32F -> GL_RED
  GL_RG8, GL_RG8_SNORM, GL_RG16F, GL_RG32F -> GL_RG
  GL_RGB8, GL_SRGB8, GL_RGB8_SNORM, GL_RGB16F, GL_RGB32F -> GL_RGB
  GL_RGBA8, GL_SRGB8_ALPHA8, GL_RGBA8_SNORM, GL_RGBA16F, GL_RGBA32F -> GL_RGBA
  else -> internalFormat
}

private fun type(internalFormat: Int) = when (internalFormat) {
  GL_R16F, GL_RG16F, GL_RGB16F, GL_RGBA16F -> GL_HALF_FLOAT
  GL_R32F, GL_RG32F, GL_RGB32F, GL_RGBA32F -> GL_FLOAT
  else -> GL_UNSIGNED_BYTE
}

/**
 * An OpenGL texture that also has the appropriate framebuffers so that
 * it can be written to, as well as read from.
 */
open class WritableTexture @JvmOverloads constructor(
    protected val width: Int,
    protected val height: Int,
    hasDepth: Boolean = false,
    hasStencil: Boolean = false,
    internalFormat: Int = GL_RGBA,
    format: Int = format(internalFormat),
    type: Int = type(internalFormat)
) : Texture() {

  private val temp = IntArray(16)

  private val buffers = IntArray(3)

  private var defaultFramebufferId: Int = 0
  private val defaultViewportSize = IntArray(4)

  /**
   * In order to get the Bitmap for a scene, you need to first bind the framebuffer with
   * the desired size, render your scene, call getBitmap(), then unbind hte framebuffer.
   *
   * <pre>`
   * exportTexture.bindFramebuffer();
   * renderScene();
   * Bitmap bitmap = exportTexture.getBitmap();
   * exportTexture.unbindFramebuffer();
  `</pre> *
   *
   * @return The bitmap that was rendered to the texture.
   */
  val bitmap: Bitmap
    get() {
      val buffer = ByteBuffer.allocateDirect(width * height * 4)
      glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer)

      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      bitmap.copyPixelsFromBuffer(buffer.rewind())

      return bitmap
    }

  init {
    // record the old values
    glGetIntegerv(GL_VIEWPORT, defaultViewportSize, 0)
    glGetIntegerv(GL_FRAMEBUFFER_BINDING, temp, 0)
    defaultFramebufferId = temp[0]

    // generate the fbo and texture
    glGenFramebuffers(1, buffers, 0)

    GLState.bindTexture(0, GL_TEXTURE_2D, name)

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

    // create the texture in memory
    glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, null)
    glCheckError { "glTexImage2D(GL_TEXTURE_2D, 0, $internalFormat, $width, $height, 0, $format, $type, null)" }

    // unbind the texture before attaching it to the framebuffer
    GLState.bindTexture(0, GL_TEXTURE_2D, 0)

    // create the framebuffer
    glBindFramebuffer(GL_FRAMEBUFFER, buffers[0])

    // attach the texture buffer to color
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, name, 0)
    glCheckError { "glFramebufferTexture2D" }

    if (hasDepth) {
      glGenRenderbuffers(1, buffers, 1)

      // create and bind the depth buffer
      glBindRenderbuffer(GL_RENDERBUFFER, buffers[1])
      glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, width, height)
      glBindRenderbuffer(GL_RENDERBUFFER, 0)
      glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, buffers[1])
    } else {
      buffers[1] = -1
    }

    if (hasStencil) {
      glGenRenderbuffers(1, buffers, 2)
      glBindRenderbuffer(GL_RENDERBUFFER, buffers[2])
      glRenderbufferStorage(GL_RENDERBUFFER, GL_STENCIL_INDEX8, width, height)
      glBindRenderbuffer(GL_RENDERBUFFER, 0)
      glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, buffers[2])
    } else {
      buffers[2] = -1
    }

    val error = glCheckFramebufferStatus(GL_FRAMEBUFFER)
    if (error != GL_FRAMEBUFFER_COMPLETE) {
      GLState.logger.log(String.format("Failed to make complete Framebuffer: 0x%s", Integer.toHexString(error)))
    } else {
      // clear the texture
      glClearColor(0f, 0f, 0f, 0f)
      glClearStencil(0)
      glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)

      // reset the old framebuffer and viewport
      glBindFramebuffer(GL_FRAMEBUFFER, defaultFramebufferId)
      glViewport(defaultViewportSize[0], defaultViewportSize[1], defaultViewportSize[2],
          defaultViewportSize[3])
    }
  }

  fun getBitmapPbo(width: Int, height: Int): Bitmap {
    val buffers = IntArray(1)
    glGenBuffers(1, buffers, 0)
    glBindBuffer(GL_PIXEL_PACK_BUFFER, buffers[0])
    glBufferData(GL_PIXEL_PACK_BUFFER, width * height * 4, null, GL_DYNAMIC_READ)
    glBindBuffer(GL_PIXEL_PACK_BUFFER, 0)

    val out = WritableTexture(width, height)
    out.bindFramebuffer()
    GLState.render()
    out.unbindFramebuffer()

    glReadBuffer(GL_COLOR_ATTACHMENT0)
    glGetError()
    glBindBuffer(GL_PIXEL_PACK_BUFFER, buffers[0])
    glGetError()
    val pboBuffer = ByteBuffer.allocateDirect(4 * width * height)
    pboBuffer.order(ByteOrder.nativeOrder())
    glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pboBuffer)
    glGetError()
    val buffer = glMapBufferRange(GL_PIXEL_PACK_BUFFER, 0, width * height * 4, GL_DYNAMIC_READ) as ByteBuffer
    glGetError()
    glUnmapBuffer(GL_PIXEL_PACK_BUFFER)
    glBindBuffer(GL_PIXEL_PACK_BUFFER, 0)
    glGetError()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(buffer.rewind())

    return bitmap
  }

  /**
   * Executes all drawing commands to the current framebuffer.
   */
  fun draw(body: () -> Unit) {
    bindFramebuffer()
    body()
    unbindFramebuffer()
  }

  /**
   * Binds the frame buffer of this texture for writing.
   */
  fun bindFramebuffer() {
    // get the old values
    GLState.getViewport(defaultViewportSize)
    glGetIntegerv(GL_FRAMEBUFFER_BINDING, temp, 0)
    defaultFramebufferId = temp[0]

    // bind the framebuffer
    GLState.bindFramebuffer(buffers[0])
    GLState.setViewport(0, 0, width, height)
  }

  /**
   * Unbinds the current framebuffer.
   *
   * @param restoreState True to restore the previous viewport/framebuffer state, false to
   * restore the default framebuffer;
   */
  @JvmOverloads
  fun unbindFramebuffer(restoreState: Boolean = true) {
    if (restoreState) {
      GLState.bindFramebuffer(defaultFramebufferId)
      GLState.setViewport(defaultViewportSize[0], defaultViewportSize[1], defaultViewportSize[2],
          defaultViewportSize[3])
    } else {
      GLState.bindFramebuffer(0)
    }
  }

  override fun destroy() {
    super.destroy()
    glDeleteBuffers(2, buffers, 0)
  }
}