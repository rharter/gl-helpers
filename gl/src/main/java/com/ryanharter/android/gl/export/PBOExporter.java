package com.ryanharter.android.gl.export;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import com.ryanharter.android.gl.GLState;
import com.ryanharter.android.gl.exceptions.GLException;
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
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glDeleteBuffers;
import static android.opengl.GLES20.glDeleteFramebuffers;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES30.GL_DYNAMIC_READ;
import static android.opengl.GLES30.GL_MAP_READ_BIT;
import static android.opengl.GLES30.GL_PIXEL_PACK_BUFFER;
import static android.opengl.GLES30.glMapBufferRange;
import static android.opengl.GLES30.glReadBuffer;
import static android.opengl.GLES30.glUnmapBuffer;
import static com.ryanharter.android.gl.GLES2Fix.glReadPixelsPBO;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
final class PBOExporter implements Exporter {

  private final int width;
  private final int height;

  private int[] ids = new int[3];
  private boolean destroyed;

  public PBOExporter(int width, int height) {
    this.width = width;
    this.height = height;

    glGenBuffers(1, ids, 0);
    glBindBuffer(GL_PIXEL_PACK_BUFFER, ids[0]);
    glBufferData(GL_PIXEL_PACK_BUFFER, 4 * width * height, null, GL_DYNAMIC_READ);
    glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);

    glGenFramebuffers(1, ids, 1);
    glGenTextures(1, ids, 2);

    GLState.bindTexture(0, GL_TEXTURE_2D, ids[2]);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
    GLState.bindTexture(0, GL_TEXTURE_2D, 0);

    GLState.bindFramebuffer(ids[1]);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, ids[2], 0);
    GLState.bindFramebuffer(0);
  }

  @Override public void begin() {
    GLState.bindFramebuffer(ids[1]);
  }

  @Override public Bitmap export() throws GLException {
    if (destroyed) {
      throw new IllegalStateException("Exporter has already been destroyed.");
    }

    Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    export(result);
    return result;
  }

  @Override public void export(Bitmap result) throws GLException {
    if (destroyed) {
      throw new IllegalStateException("Exporter has already been destroyed.");
    }

    if (result.getWidth() != width || result.getHeight() != height) {
      throw new IllegalArgumentException("Result bitmap must match exporter dimensions.");
    }

    if (result.getConfig() != Bitmap.Config.ARGB_8888) {
      throw new IllegalArgumentException("Result bitmap must have ARGB_8888 config.");
    }

    glReadBuffer(GL_COLOR_ATTACHMENT0);
    glBindBuffer(GL_PIXEL_PACK_BUFFER, ids[0]);
    glReadPixelsPBO(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, 0);

    ByteBuffer buffer = (ByteBuffer) glMapBufferRange(GL_PIXEL_PACK_BUFFER, 0, 4 * width * height, GL_MAP_READ_BIT);
    if (buffer == null) {
      GLException exception = GLException.getGlError("Received null buffer for range [w=" + width + ", h=" + height + "]");
      glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
      throw exception;
    }

    buffer.order(ByteOrder.nativeOrder());
    result.copyPixelsFromBuffer(buffer);
    glUnmapBuffer(GL_PIXEL_PACK_BUFFER);
    glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
  }

  @Override public void destroy() {
    destroyed = true;
    GLState.bindFramebuffer(0);
    glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
    glDeleteBuffers(1, ids, 0);
    glDeleteFramebuffers(1, ids, 1);
    glDeleteTextures(1, ids, 2);
  }
}
