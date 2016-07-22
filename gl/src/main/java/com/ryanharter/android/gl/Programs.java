package com.ryanharter.android.gl;

import android.content.res.AssetManager;
import android.opengl.GLES20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FALSE;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glDetachShader;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;

final class Programs {

  private Programs() { }

  static int linkProgram(int... shaders) {
    int program = glCreateProgram();

    for (int shader : shaders) {
      glAttachShader(program, shader);
    }

    glLinkProgram(program);

    int[] status = new int[1];
    glGetProgramiv(program, GL_LINK_STATUS, status, 0);
    if (status[0] == GL_FALSE) {
      destroy(program, shaders);
      return 0;
    }

    return program;
  }

  /**
   * Compiles the provided shader source.
   *
   * @param type The type of shader, either {@link GLES20#GL_FRAGMENT_SHADER}
   *        or {@link GLES20#GL_VERTEX_SHADER}.
   * @param source The source code of the shader to load.
   * @return A handle to the shader, or 0 on failure.
   */
  static int loadShader(int type, String source) {
    int shader = glCreateShader(type);
    glShaderSource(shader, source);
    glCompileShader(shader);

    int[] compiled = new int[1];
    glGetShaderiv(shader, GL_COMPILE_STATUS, compiled, 0);
    if (compiled[0] == 0) {
      GlUtil.logger.log(String.format("Could not compile shader: %s", glGetShaderInfoLog(shader)));
      glDeleteShader(shader);
      shader = 0;
    }

    return shader;
  }

  static String readShader(AssetManager assetManager, String name, Map<String, String> defines) {
    InputStream in = null;
    try {
      in = assetManager.open(name);
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));

      String line;
      StringBuilder source = new StringBuilder();
      while ((line = reader.readLine()) != null) {
        source.append(line).append('\n');
        if (line.startsWith("#version ")) {
          boolean first = true;
          for (Map.Entry<String, String> define : defines.entrySet()) {
            if (first) {
              source.append('\n');
              first = false;
            }
            source.append("#define ");
            source.append(define.getKey());
            source.append(' ');
            source.append(define.getValue());
            source.append('\n');
          }
          if (!first) {
            source.append("#line ").append(2).append('\n');
          }
        }
      }

      return source.toString();
    } catch (IOException e) {
      return null;
    } finally {
      try {
        if (in != null) {
          in.close();
        }
      } catch (IOException e) {
        // ignore
      }
    }
  }

  public static void destroy(int program, int... shaders) {
    for (int shader : shaders) {
      glDetachShader(program, shader);
      glDeleteShader(shader);
    }
    glDeleteProgram(program);
  }
}
