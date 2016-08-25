package com.ryanharter.android.gl;

public final class GLES2Fix {

  public static native void glReadPixelsPBO(int x, int y, int width, int height, int format, int type, int offset);

  static {
    System.loadLibrary("glhelper");
  }
}
