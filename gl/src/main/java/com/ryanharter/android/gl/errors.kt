package com.ryanharter.android.gl

import android.opengl.GLES20
import android.opengl.GLES20.GL_INVALID_ENUM
import android.opengl.GLES20.GL_INVALID_FRAMEBUFFER_OPERATION
import android.opengl.GLES20.GL_INVALID_OPERATION
import android.opengl.GLES20.GL_INVALID_VALUE
import android.opengl.GLES20.GL_NO_ERROR
import android.opengl.GLES20.GL_OUT_OF_MEMORY
import android.opengl.GLES20.glGetError

fun glErrorString(error: Int) = when (error) {
  GL_INVALID_ENUM -> "GL_INVALID_ENUM"
  GL_INVALID_VALUE -> "GL_INVALID_VALUE"
  GL_INVALID_OPERATION -> "GL_INVALID_OPERATION"
  GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY"
  GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION"
  else -> "Unknown 0x${error.toString(16)}"
}

/**
 * Checks for OpenGL errors using [glGetError] and throws a [RuntimeException] if any exist.
 *
 * Use [name] to identify the location or calls for which you are checking errors.
 */
fun glCheckError(name: () -> String) {
  var errorString = ""
  var error = glGetError()
  while (error != GL_NO_ERROR) {
    val e = glErrorString(error)

    if (errorString.isNotEmpty()) {
      errorString += ", "
    }
    errorString += e

    error = glGetError()
  }
  if (errorString.isNotEmpty()) {
    val e = RuntimeException(
        "GL Error [$errorString]: ${name()}"
    )
    e.stackTrace = e.stackTrace.drop(1).toTypedArray()
    throw e
  }
}
