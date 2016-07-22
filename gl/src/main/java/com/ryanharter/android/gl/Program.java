package com.ryanharter.android.gl;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.v4.util.ArrayMap;

import java.util.Collections;
import java.util.Map;

import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform2fv;
import static android.opengl.GLES20.glUniform3f;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniform4fv;
import static android.opengl.GLES20.glUniformMatrix4fv;

/**
 * Created by rharter on 4/9/14.
 */
public class Program {

  private static final ArrayMap<String, Program> programs = new ArrayMap<>();

  private String name;
  private int program;
  private int vertexShader;
  private int fragmentShader;

  private final ArrayMap<String, Integer> uniforms = new ArrayMap<>();
  private final ArrayMap<String, Integer> attributes = new ArrayMap<>();

  private boolean isValid;

  private Program(String name) {
    this.name = name;
  }

  public static Program get(String name) {
    return programs.get(name);
  }

  public static Program load(Context context, String asset, String name) {
    return load(context, asset, name, Collections.<String, String>emptyMap());
  }

  public static Program load(Context context, String asset, String name,
      Map<String, String> defines) {
    Program program = programs.get(name);
    if (program == null) {
      AssetManager assets = context.getAssets();
      String vs = Programs.readShader(assets, asset + ".vs", defines);
      String fs = Programs.readShader(assets, asset + ".fs", defines);
      return load(name, vs, fs, defines);
    }
    return program;
  }

  public static Program load(String name, String vertexSource, String fragmentSource) {
    return load(name, vertexSource, fragmentSource, Collections.<String, String>emptyMap());
  }

  public static Program load(String name, String vertexSource, String fragmentSource,
      Map<String, String> defines) {
    Program program = programs.get(name);
    if (program == null) {
      program = new Program(name);
      program.compile(name, vertexSource, fragmentSource);

      programs.put(name, program);
    }
    return program;
  }

  private void compile(String name, String vs, String fs) {
    if ((vertexShader = Programs.loadShader(GL_VERTEX_SHADER, vs)) == 0) {
      GlUtil.logger.log(String.format("Couldn't compile vertex shader: %s", name));
      return;
    }
    if ((fragmentShader = Programs.loadShader(GL_FRAGMENT_SHADER, fs)) == 0) {
      GlUtil.logger.log(String.format("Couldn't compile fragment shader: %s", name));
      glDeleteShader(vertexShader);
      return;
    }
    program = Programs.linkProgram(vertexShader, fragmentShader);

    isValid = program != 0;
  }

  /**
   * Whether the program is valid.
   * @return true if the program is valid.
   */
  public boolean isValid() {
    return isValid;
  }

  /**
   * Enables the program with glUseProgram().
   */
  public void use() {
    if (isValid()) {
      GLState.useProgram(program);
    }
  }

  /**
   * Destroys the program and it's shaders.
   */
  public void destroy() {
    if (isValid()) {
      Programs.destroy(program, vertexShader, fragmentShader);
    }
  }

  /**
   * Returns the OpenGL name (int location) of the program.
   * @return The OpenGL name of the program.
   */
  public int getName() {
    return program;
  }

  /**
   * Retrieves a uniform location by name.
   *
   * @param name The name of the uniform in the shader.
   * @return The index of the uniform.
   */
  public int uniformLocation(String name) {
    Integer loc = uniforms.get(name);
    if (loc == null) {
      loc = glGetUniformLocation(program, name);
      if (loc == -1) {
        GlUtil.logger.log(String.format("%s: Unknown uniform %s", getTag(), name));
        return -1;
      }
      uniforms.put(name, loc);
    }
    return loc;
  }

  /**
   * Retrieves an attribute location by name.
   *
   * @param name The name of the attribute in the shader.
   * @return The index of the attribute.
   */
  public int attribLocation(String name) {
    Integer loc = attributes.get(name);
    if (loc == null) {
      loc = glGetAttribLocation(program, name);
      if (loc == -1) {
        GlUtil.logger.log(String.format("%s: Unknown attribute %s", getTag(), name));
        return -1;
      }
      attributes.put(name, loc);
    }
    return loc;
  }

  public void bindInt(int location, int v) {
    if (location < 0) {
      GlUtil.logger.log(String.format("%s: Invalid uniform location: %d", getTag(), location));
      return;
    }
    glUniform1i(location, v);
  }

  public void bindFloat(int location, float v) {
    if (location < 0) {
      GlUtil.logger.log(String.format("%s: Invalid uniform location: %d", getTag(), location));
      return;
    }
    glUniform1f(location, v);
  }

  public void bindFloat3(int location, float x, float y, float z) {
    if (location < 0) {
      GlUtil.logger.log(String.format("%s: Invalid uniform location: %d", getTag(), location));
      return;
    }
    glUniform3f(location, x, y, z);
  }

  public void bindFloat4(int location, float x, float y, float z, float w) {
    if (location < 0) {
      GlUtil.logger.log(String.format("%s: Invalid uniform location: %d", getTag(), location));
      return;
    }
    glUniform4f(location, x, y, z, w);
  }

  public void bindMatrix(int location, float[] m) {
    if (location < 0) {
      GlUtil.logger.log(String.format("%s: Invalid uniform location: %d", getTag(), location));
      return;
    }
    glUniformMatrix4fv(location, 1, false, m, 0);
  }

  public void bindFloat2Array(int location, float[] v) {
    if (location < 0) {
      GlUtil.logger.log(String.format("%s: Invalid uniform location: %d", getTag(), location));
      return;
    }
    glUniform2fv(location, 1, v, 0);
  }

  public void bindFloat4Array(int location, float[] v) {
    if (location < 0) {
      GlUtil.logger.log(String.format("%s: Invalid uniform location: %d", getTag(), location));
      return;
    }
    glUniform4fv(location, 1, v, 0);
  }

  private String getTag() {
    return String.format("Program:%s", name);
  }
}
