package com.ryanharter.android.gl;

import android.graphics.Bitmap;
import android.opengl.GLES11Ext;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glVertexAttribPointer;

/**
 * Created by rharter on 4/9/14.
 */
public class Texture {

  private final TextureRenderer renderer;
  private int bindUnit = -1;
  private int[] textureId = new int[1];

  public Texture() {
    glGenTextures(1, textureId, 0);

    renderer = new GL2TextureRenderer();
  }

  public void render() {
    renderer.render();
  }

  /**
   * Creates a new Bitmap based on the current Texture.
   */
  public Bitmap getBitmap(int width, int height) {
    ByteBuffer buffer = ByteBuffer.allocateDirect(width * width * 4);
    buffer.order(ByteOrder.nativeOrder());

    bind(0);
    GLES11Ext.glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, buffer);

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    bitmap.copyPixelsFromBuffer(buffer.rewind());
    return bitmap;
  }

  public void bind(int unit) {
    GLState.bindTexture(unit, GL_TEXTURE_2D, textureId[0]);
  }

  public void unbind() {
    if (bindUnit > -1) {
      GLState.bindTexture(bindUnit, GL_TEXTURE_2D, 0);
    }
  }

  @Deprecated public int getTextureId() {
    return getName();
  }

  public int getName() {
    return textureId[0];
  }

  public void destroy() {
    glDeleteTextures(1, textureId, 0);
  }

  private interface TextureRenderer {
    void render();
  }

  private static class GL2TextureRenderer implements TextureRenderer {

    private static final FloatBuffer QUAD_VERTICES = GlUtil.createFloatBuffer(new float[] {
        1, -1, 1, 1, -1, -1, -1, 1
    });

    @Override public void render() {
      GLState.setAttributeEnabled(0, true);
      glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, QUAD_VERTICES.rewind());
      glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
      GLState.setAttributeEnabled(0, false);
    }
  }
}
