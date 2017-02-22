package com.ryanharter.android.gl.exceptions;

import android.opengl.GLU;
import java.util.ArrayList;
import java.util.List;

import static android.opengl.GLES20.GL_INVALID_ENUM;
import static android.opengl.GLES20.GL_INVALID_OPERATION;
import static android.opengl.GLES20.GL_INVALID_VALUE;
import static android.opengl.GLES20.GL_NO_ERROR;
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
    List<GLException> exceptions = new ArrayList<>();

    int error = glGetError();
    while (error != GL_NO_ERROR) {
      switch (error) {
        case GL_INVALID_ENUM:
          exceptions.add(new GLInvalidEnumException(message, error));
        case GL_INVALID_VALUE:
          exceptions.add(new GLInvalidValueException(message, error));
        case GL_INVALID_OPERATION:
          exceptions.add(new GLInvalidOperationException(message, error));
        case GL_OUT_OF_MEMORY:
          exceptions.add(new GLOutOfMemoryException(message, error));
        default:
          exceptions.add(new GLException(message, error));
      }

      error = glGetError();
    }

    if (exceptions.isEmpty()) {
      return null;
    } else if (exceptions.size() == 1) {
      return exceptions.get(0);
    } else {
      return new GLCompositeException(exceptions);
    }
  }
}
