package com.ryanharter.android.gl;

import android.graphics.Bitmap;
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
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glDeleteFramebuffers;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glReadPixels;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;

/**
 * Helps export the rendered GL context to a Bitmap.
 *
 * After {@link #begin()} has been called, the exporter will be the active Framebuffer, so anything
 * rendered will be rendered into the Exporter, as opposed to the screen. This means that
 *
 * <code><pre>
 * Exporter exporter = new Exporter(150, 150);
 * exporter.begin();
 *
 * glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
 * glClear(GL_COLOR_BUFFER_BIT);
 *
 * GLState.render();
 *
 * Bitmap image = exporter.bitmap();
 * exporter.destroy();
 * </pre></code>
 *
 * The exporter could be used to write multiple frames, but should be destroyed when it is no
 * longer needed.
 */
public class Exporter {

  private final int width;
  private final int height;
  private final int[] ids = new int[2];

  public Exporter(int width, int height) {
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
  }

  public void begin() {
    glBindFramebuffer(GL_FRAMEBUFFER, ids[0]);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, ids[1], 0);
  }

  public void destroy() {
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    glDeleteFramebuffers(1, ids, 0);
    glDeleteTextures(1, ids, 1);
  }

  public Bitmap bitmap() {
    ByteBuffer buffer = ByteBuffer.allocate(width * height * 4).order(ByteOrder.nativeOrder());
    glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    bitmap.copyPixelsFromBuffer(buffer);
    return bitmap;
  }
}
