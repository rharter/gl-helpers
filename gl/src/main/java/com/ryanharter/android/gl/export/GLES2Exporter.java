package com.ryanharter.android.gl.export;

import android.graphics.Bitmap;
import com.ryanharter.android.gl.GLState;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_COLOR_ATTACHMENT0;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.glDeleteFramebuffers;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glReadPixels;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;

final class GLES2Exporter implements Exporter {


  private final int width;
  private final int height;
  private final int[] ids = new int[2];
  private boolean destroyed;

  public GLES2Exporter(int width, int height) {
    this.width = width;
    this.height = height;

    glGenFramebuffers(1, ids, 0);
    glGenTextures(1, ids, 1);

    GLState.bindTexture(0, GL_TEXTURE_2D, ids[1]);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
    GLState.bindTexture(0, GL_TEXTURE_2D, 0);

    GLState.bindFramebuffer(ids[0]);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, ids[1], 0);
    GLState.bindFramebuffer(0);
  }

  @Override public void begin() {
    GLState.bindFramebuffer(ids[0]);
  }

  @Override public Bitmap export() {
    if (destroyed) {
      throw new IllegalStateException("Exporter has already been destroyed.");
    }

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    export(bitmap);
    return bitmap;
  }

  @Override public void export(Bitmap result) {
    if (destroyed) {
      throw new IllegalStateException("Exporter has already been destroyed.");
    }

    if (result.getWidth() != width || result.getHeight() != height) {
      throw new IllegalArgumentException("Result bitmap must match exporter dimensions.");
    }

    if (result.getConfig() != Bitmap.Config.ARGB_8888) {
      throw new IllegalArgumentException("Result bitmap must have ARGB_8888 config.");
    }

    ByteBuffer buffer = ByteBuffer.allocate(width * height * 4).order(ByteOrder.nativeOrder());
    glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
    result.copyPixelsFromBuffer(buffer);

    GLState.bindFramebuffer(0);
  }

  public void destroy() {
    destroyed = true;
    GLState.bindFramebuffer(0);
    glDeleteFramebuffers(1, ids, 0);
    glDeleteTextures(1, ids, 1);
  }
}
