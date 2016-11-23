package com.pixite.common.sample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.ryanharter.android.gl.BitmapTexture;
import com.ryanharter.android.gl.GLState;
import com.ryanharter.android.gl.Program;
import com.ryanharter.android.gl.exceptions.GLException;
import com.ryanharter.android.gl.export.Exporter;
import com.ryanharter.android.gl.export.ExporterCreator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getSimpleName();
  GLSurfaceView surface;
  Renderer renderer;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    surface = (GLSurfaceView) findViewById(R.id.surface);

    surface.setEGLContextClientVersion(2);
    surface.setEGLConfigChooser(new GLSurfaceView.EGLConfigChooser() {
      @Override
      public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
        int attribs[] = {
            EGL14.EGL_RENDERABLE_TYPE, 4,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 24,
            EGL14.EGL_SAMPLE_BUFFERS, 1,
            EGL14.EGL_SAMPLES, 4,
            EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] configCounts = new int[1];
        egl.eglChooseConfig(display, attribs, configs, 1, configCounts);

        if (configCounts[0] == 0) {
          return null;
        } else {
          return configs[0];
        }
      }
    });
    surface.setRenderer(renderer = new Renderer(this));
    surface.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
  }

  @Override protected void onResume() {
    super.onResume();
    surface.requestRender();
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_main, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.export_default:
        surface.queueEvent(new Runnable() {
          @Override public void run() {
            exportDefault();
          }
        });
        return true;
      case R.id.export_read_pixels:
        surface.queueEvent(new Runnable() {
          @Override public void run() {
            exportReadPixels();
          }
        });
        return true;
      case R.id.export_pbo:
        surface.queueEvent(new Runnable() {
          @Override public void run() {
            exportPBO();
          }
        });
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void exportDefault() {
    long start = System.currentTimeMillis();
    Exporter exporter = Exporter.Factory.createExporter(4096, 4096);
    export(exporter, "exporter_auto", renderer.width, renderer.height);
    Log.d(TAG, "Exported image using automatic exporter (" + exporter.getClass().getSimpleName() + ") in " + (System.currentTimeMillis() - start) + "ms");
  }

  private void exportReadPixels() {
    long start = System.currentTimeMillis();
    Exporter exporter = ExporterCreator.createGLES2Exporter(4096, 4096);
    export(exporter, "exporter_read_pixels", renderer.width, renderer.height);
    Log.d(TAG, "Exported image using ReadPixels in " + (System.currentTimeMillis() - start) + "ms");
  }

  void exportPBO() {
    long start = System.currentTimeMillis();
    Exporter exporter = Exporter.Factory.createExporter(4096, 4096);
    export(exporter, "exporter_pbo", renderer.width, renderer.height);
    Log.d(TAG, "Exported image using PBO in " + (System.currentTimeMillis() - start) + "ms");
  }

  private void export(Exporter exporter, String name, int width, int height) {
    exporter.begin();

    int[] origViewport = GLState.getViewport();
    GLState.setViewport(0, 0, 4096, 4096);
    renderer.onSurfaceChanged(null, 4096, 4096);
    renderer.render(true);

    try {
      Bitmap bitmap = exporter.export();
      writeBitmap(name, bitmap);
    } catch (GLException e) {
      e.printStackTrace();
    }

    exporter.destroy();
    GLState.setViewport(origViewport[0], origViewport[1], origViewport[2], origViewport[3]);
    renderer.onSurfaceChanged(null, origViewport[2], origViewport[3]);
  }

  private void writeBitmap(String prefix, Bitmap bitmap) {
    OutputStream out = null;
    try {
      File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
          prefix + ".jpg");
      out = new FileOutputStream(file);
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          // no op
        }
      }
    }
  }

  private class Renderer implements GLSurfaceView.Renderer {

    Context context;
    SimpleProgram simpleProgram;
    BitmapTexture image;

    int width, height;

    Renderer(Context context) {
      this.context = context.getApplicationContext();
    }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
      initGL();
    }

    private void initGL() {
      intiGLState();
      initTextures();

      simpleProgram = ProgramFactory.getProgram();
    }

    private void intiGLState() {
      GLState.reset();
      GLState.setAttributeEnabled(0, true);
    }

    private void initTextures() {
      try {
        InputStream in = context.getAssets().open("images/pixite_logo.png");
        Bitmap image = BitmapFactory.decodeStream(in);
        this.image = new BitmapTexture(image);
      } catch (IOException e) {
        Log.e(TAG, "Failed to load image.", e);
      }
    }

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
      this.width = width;
      this.height = height;
    }

    @Override public void onDrawFrame(GL10 gl) {
      render(false);
    }

    public void render(boolean export) {
      if (image == null) {
        return;
      }

      glClearColor(1, 1, 1, 1);
      glClear(GL_COLOR_BUFFER_BIT);
      GLState.setBlend(true, true);

      simpleProgram.use();

      float[] mvpMatrix = new float[16];
      Matrix.setIdentityM(mvpMatrix, 0);

      // scale
      float imageAspect = (float) image.width() / image.height();
      float windowAspect = (float) width / height;
      if (imageAspect > windowAspect) {
        // image is wider
        Matrix.orthoM(mvpMatrix, 0, -1, 1, -1 / imageAspect, 1 / imageAspect, -1, 1);
      } else if (imageAspect < windowAspect) {
        Matrix.orthoM(mvpMatrix, 0, -windowAspect / imageAspect, windowAspect / imageAspect, -1, 1, -1, 1);
      }

      // flip
      if (!export) {
        Matrix.scaleM(mvpMatrix, 0, 1, -1, 1);
      }

      // add some padding
      Matrix.scaleM(mvpMatrix, 0, .8f, .8f, 1);

      simpleProgram.bindMvpMatrix(mvpMatrix);

      image.bind(0);
      simpleProgram.bindImage(0);

      GLState.render();
    }
  }

  private static class ProgramFactory {
    static SimpleProgram getProgram() {
      switch (GLState.getGlVersion()) {
        case GLES_30:
          return new GLES3SimpleProgram();
        default:
          return new GLES2SimpleProgram();
      }
    }
  }

  private interface SimpleProgram {
    Program getProgram();
    void use();
    void bindMvpMatrix(float[] matrix);
    void bindImage(int texture);
  }

  private static class GLES2SimpleProgram implements SimpleProgram {

    private static final String SIMPLE_VERTEX = ""
        + "attribute vec4 position;\n"
        + "uniform mat4 mvpMatrix;\n"
        + "varying highp vec2 texCoord;\n"
        + "void main() {\n"
        + "  texCoord = position.xy * 0.5 + 0.5;\n"
        + "  gl_Position = position * mvpMatrix;\n"
        + "}";

    private static final String SIMPLE_FRAGMENT = ""
        + "varying highp vec2 texCoord;\n"
        + "uniform sampler2D image;\n"
        + "void main() {\n"
        + "  gl_FragColor = texture2D(image, texCoord);\n"
        + "}";

    final Program delegate;

    public GLES2SimpleProgram() {
      delegate = Program.load("simple2", SIMPLE_VERTEX, SIMPLE_FRAGMENT);
    }

    @Override public Program getProgram() {
      return delegate;
    }

    @Override public void use() {
      delegate.use();
    }

    @Override public void bindMvpMatrix(float[] matrix) {
      delegate.bindMatrix(delegate.uniformLocation("mvpMatrix"), matrix);
    }

    @Override public void bindImage(int texture) {
      delegate.bindInt(delegate.uniformLocation("image"), texture);
    }
  }

  private static class GLES3SimpleProgram implements SimpleProgram {

    private static final String SIMPLE_VERTEX = ""
        + "#version 300 es\n"
        + "layout(location = 0) in vec4 position;\n"
        + "layout(location = 0) uniform mat4 mvpMatrix;\n"
        + "out highp vec2 texCoord;\n"
        + "invariant gl_Position;\n"
        + "void main() {\n"
        + "  texCoord = position.xy * 0.5 + 0.5;\n"
        + "  gl_Position = position * mvpMatrix;\n"
        + "}";

    private static final String SIMPLE_FRAGMENT = ""
        + "#version 300 es\n"
        + "in highp vec2 texCoord;\n"
        + "layout(location = 1) uniform sampler2D image;\n"
        + "layout(location = 0) out vec4 fragColor;\n"
        + "void main() {\n"
        + "  fragColor = texture(image, texCoord);\n"
        + "}";

    private final Program delegate;

    public GLES3SimpleProgram() {
      delegate = Program.load("simple3", SIMPLE_VERTEX, SIMPLE_FRAGMENT);
    }

    @Override public Program getProgram() {
      return delegate;
    }

    @Override public void use() {
      delegate.use();
    }

    @Override public void bindMvpMatrix(float[] matrix) {
      delegate.bindMatrix(0, matrix);
    }

    @Override public void bindImage(int texture) {
      delegate.bindInt(1, texture);
    }
  }
}
