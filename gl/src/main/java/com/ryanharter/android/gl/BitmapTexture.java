package com.ryanharter.android.gl;

import android.graphics.Bitmap;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_OUT_OF_MEMORY;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.glGenerateMipmap;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLUtils.texImage2D;

/**
 * Creates a GL texture and uploads the supplied Bitmap.
 */
public class BitmapTexture extends Texture {

  private int width, height;

  public BitmapTexture(Bitmap bitmap) {
    this(bitmap, true);
  }

  public BitmapTexture(Bitmap bitmap, boolean mipmap) {
    this(bitmap, GL_LINEAR, GL_CLAMP_TO_EDGE, GL_RGBA, mipmap);
  }

  public BitmapTexture(Bitmap bitmap, int filter, int wrap, int format, boolean mipmap) {
    super();
    bind(0);

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrap);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrap);

    // attempt to load the bitmap with backouts
    int attempts = 0;
    boolean loaded = false;
    while (!loaded && attempts < 3) {
      texImage2D(GL_TEXTURE_2D, 0, format, bitmap, 0);

      if (mipmap) {
        glGenerateMipmap(GL_TEXTURE_2D);
      }

      int error = glGetError();
      if (error == GL_OUT_OF_MEMORY) {
        GlUtil.logger.log(String.format("Received out of memory error loading bitmap of size[%dx%d]",
            bitmap.getWidth(), bitmap.getHeight()));

        // shrink the image dimensions by 2
        int h = bitmap.getHeight() / 2;
        int w = bitmap.getWidth() / 2;
        bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
        System.gc();
        ++attempts;
      } else {
        loaded = true;
        width = bitmap.getWidth();
        height = bitmap.getHeight();
      }
    }
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }

}
