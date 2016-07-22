package com.ryanharter.android.gl;

import android.graphics.Bitmap;

import static android.opengl.GLES20.GL_OUT_OF_MEMORY;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glGenerateMipmap;
import static android.opengl.GLES20.glGetError;
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
    super();
    bind(0);

    // attempt to load the bitmap with backouts
    int attempts = 0;
    boolean loaded = false;
    while (!loaded && attempts < 3) {
      texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);

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
