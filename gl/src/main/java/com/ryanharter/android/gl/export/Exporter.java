package com.ryanharter.android.gl.export;

import android.graphics.Bitmap;
import com.ryanharter.android.gl.GLState;

/**
 * Exports GL state to bitmaps.
 */
public interface Exporter {

  /**
   * Begins the recording sequence.  Draw calls between <code>begin()</code>
   * and {@link #export()} will be drawn to an internal framebuffer to be
   * exported.
   */
  void begin();

  /**
   * Returns a new Bitmap containing the state that has been drawn since {@link #begin()}.
   *
   * This ends the current recording sequence.  Call {@link #begin()} to start a new one.
   */
  Bitmap export();

  /**
   * Writes the state that has been drawn since {@link #begin()} to <code>result</code>.
   *
   * This ends the current recording sequence.  Call {@link #begin()} to start a new one.
   */
  void export(Bitmap result);

  /**
   * Destroys all internal state of this exporter.  It cannot be reused
   * after this operation.
   */
  void destroy();

  class Factory {
    public static Exporter createExporter(int width, int height) {
      switch (GLState.getGlVersion()) {
        case GLES_30:
          return new PBOExporter(width, height);
        case GLES_20:
          return new GLES2Exporter(width, height);
        case GL_UNKNOWN:
        default:
          throw new IllegalStateException("Unsupported OpenGL version.");
      }
    }
  }

}
