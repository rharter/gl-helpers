package com.ryanharter.android.gl.exceptions;

import android.opengl.GLU;

import static android.opengl.GLES20.GL_INVALID_ENUM;
import static android.opengl.GLES20.GL_INVALID_OPERATION;
import static android.opengl.GLES20.GL_INVALID_VALUE;
import static android.opengl.GLES20.GL_OUT_OF_MEMORY;
import static android.opengl.GLES20.glGetError;

public class GLException extends Exception {

  private final int error;

  public GLException(String message, int error) {
    super(message);
    this.error = error;
  }

  public int getValue() {
    return error;
  }

  @Override public String getMessage() {
    String errorString = GLU.gluErrorString(error);
    if ( errorString == null ) {
      errorString = "Unknown error 0x" + Integer.toHexString(error);
    }
    return String.format("%s\n%s", super.getMessage(), errorString);
  }

  public static GLException getGlError(String message) {
    int error = glGetError();
    switch (error) {
      case GL_INVALID_ENUM:
        return new GLInvalidEnumException(message, error);
      case GL_INVALID_VALUE:
        return new GLInvalidValueException(message, error);
      case GL_INVALID_OPERATION:
        return new GLInvalidOperationException(message, error);
      case GL_OUT_OF_MEMORY:
        return new GLOutOfMemoryException(message, error);
      default:
        return new GLException(message, error);
    }
  }
}
