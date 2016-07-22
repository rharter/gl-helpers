package com.pixite.common.sample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.ryanharter.android.gl.BitmapTexture;
import com.ryanharter.android.gl.GLState;
import com.ryanharter.android.gl.Program;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getSimpleName();
  GLSurfaceView surface;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    surface = (GLSurfaceView) findViewById(R.id.surface);

    surface.setEGLContextClientVersion(2);
    surface.setRenderer(new Renderer(this));
    surface.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
  }

  @Override protected void onResume() {
    super.onResume();
    surface.requestRender();
  }

  private class Renderer implements GLSurfaceView.Renderer {

    Context context;
    SimpleProgram simpleProgram;
    BitmapTexture image;

    private int width, height;

    public Renderer(Context context) {
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
      glClearColor(1, 1, 1, 1);
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
      if (image == null) {
        return;
      }

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
      } else {
        Matrix.orthoM(mvpMatrix, 0, -imageAspect, imageAspect, -1, 1, -1, 1);
      }

      // flip
      Matrix.scaleM(mvpMatrix, 0, 1, -1, 1);

      // add some padding
      Matrix.scaleM(mvpMatrix, 0, .8f, .8f, 1);

      simpleProgram.bindMvpMatrix(mvpMatrix);

      image.bind(0);
      simpleProgram.bindImage(0);

      image.render();
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
