package com.ryanharter.android.gl.exceptions;

import android.opengl.GLU;
import java.util.List;

public class GLCompositeException extends GLException {

  private final List<GLException> exceptions;

  public GLCompositeException(List<GLException> exceptions) {
    super("There were multiple gl errors", -1);
    this.exceptions = exceptions;
  }

  @Override public String getMessage() {
    StringBuilder message = new StringBuilder("Multiple GL Errors: ");
    for (GLException exception : exceptions) {
      String errorString = GLU.gluErrorString(exception.getValue());
      if ( errorString == null ) {
        errorString = "Unknown error 0x" + Integer.toHexString(exception.getValue());
      }
      message.append(" ").append(errorString);
    }
    return message.toString();
  }
}
