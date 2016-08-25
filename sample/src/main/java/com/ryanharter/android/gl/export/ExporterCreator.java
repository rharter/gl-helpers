package com.ryanharter.android.gl.export;

/**
 * This class is simply a hack to allow the sample app to specifically
 * choose the exporter.  Don't do this, instead use
 * {@link Exporter.Factory#createExporter(int, int)} and let it
 * automatically choose the best option.
 */
public class ExporterCreator {

  public static Exporter createGLES2Exporter(int width, int height) {
    return new GLES2Exporter(width, height);
  }

  public static Exporter createPBOExporter(int width, int height) {
    return new PBOExporter(width, height);
  }

}
