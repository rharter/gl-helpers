package com.ryanharter.android.gl.exceptions;

import android.opengl.GLU;

import static android.opengl.GLES20.GL_INVALID_ENUM;
import static android.opengl.GLES20.GL_INVALID_OPERATION;
import static android.opengl.GLES20.GL_INVALID_VALUE;
import static android.opengl.GLES20.GL_OUT_OF_MEMORY;
import static android.opengl.GLES20.glGetError;

public class GLException extends Exception {

  private final int error;

  public GLException(int error) {
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
    return errorString;
  }

  public static void throwGlError() throws GLInvalidEnumException,
      GLInvalidValueException,
      GLInvalidOperationException,
      GLOutOfMemoryException {
    int error = glGetError();
    switch (error) {
      case GL_INVALID_ENUM:
        throw new GLInvalidEnumException(error);
      case GL_INVALID_VALUE:
        throw new GLInvalidValueException(error);
      case GL_INVALID_OPERATION:
        throw new GLInvalidOperationException(error);
      case GL_OUT_OF_MEMORY:
        throw new GLOutOfMemoryException(error);
      default:
        // no op
    }
  }
}
