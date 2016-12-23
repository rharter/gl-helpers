package com.ryanharter.android.gl.exceptions;

import java.util.List;

public class GLCompositeException extends GLException {

  private final List<GLException> exceptions;

  public GLCompositeException(List<GLException> exceptions) {
    super("There were multiple gl errors", -1);
    this.exceptions = exceptions;
  }

  @Override public String getMessage() {
    StringBuilder message = new StringBuilder("Multiple GL Errors:\n");
    for (GLException exception : exceptions) {
      message.append("  0x")
          .append(Integer.toHexString(exception.getValue()))
          .append(": ")
          .append(exception.getMessage())
          .append('\n');
    }
    return message.toString();
  }
}
