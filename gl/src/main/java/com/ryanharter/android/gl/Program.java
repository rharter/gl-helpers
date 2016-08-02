package com.ryanharter.android.gl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.support.v4.util.ArrayMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
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
 * Represents a GL Program, with vertex and fragment shader, that has been compiled and linked.
 *
 * Call {@link #use()} to use the program, then uniform values can be set with the
 * <code>bind*</code> methods.
 */
@SuppressLint("DefaultLocale")
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

  /**
   * Gets an already linked and compiled program by name.
   * @param name The name of the program to get.
   * @return The compiled and linked program, or null.
   */
  public static Program get(String name) {
    return programs.get(name);
  }

  /**
   * Loads a program from the Assets directory.
   *
   * <code>asset</code> should contain the path to the shader source, with the vertex shader
   * having the <code>.vs</code> extension and the fragment shader having the <code>.fs</code>
   * extension.
   *
   * @param context The context used to load the AssetManager.
   * @param asset The path to the shader source files in the assets directory.
   * @return The compiled and linked program.
   */
  public static Program load(Context context, String asset) {
    return load(context, asset, Collections.<String, String>emptyMap());
  }

  /**
   * Loads a program from the Assets directory, adding the supplied defines to each shader.
   *
   * <code>asset</code> should contain the path to the shader source, with the vertex shader
   * having the <code>.vs</code> extension and the fragment shader having the <code>.fs</code>
   * extension.
   *
   * @param context The context used to load the AssetManager.
   * @param asset The path to the shader source files in the assets directory.
   * @param defines The values to be defined in each of the shaders.
   * @return The compiled and linked program.
   */
  public static Program load(Context context, String asset, Map<String, String> defines) {
    Program program = programs.get(asset);
    if (program == null) {
      AssetManager assets = context.getAssets();
      String vs = Programs.readShader(assets, asset + ".vs", defines);
      String fs = Programs.readShader(assets, asset + ".fs", defines);
      return load(asset, vs, fs, defines);
    }
    return program;
  }

  /**
   * Loads a program using the supplied source for Vertex and Fragment shaders.
   *
   * If a program named <code>name</code> has already been loaded it will be returned to avoid
   * creating identical programs.
   *
   * @param name The name of the program to load.
   * @param vertexSource The GLSL source of the vertex shader.
   * @param fragmentSource The GLSL source of the fragment shader.
   * @return The compiled and linked program.
   */
  public static Program load(String name, String vertexSource, String fragmentSource) {
    return load(name, vertexSource, fragmentSource, Collections.<String, String>emptyMap());
  }

  /**
   * Loads a program using the supplied source for Vertex and Fragment shaders, including the
   * defines in each.
   *
   * If a program named <code>name</code> has already been loaded it will be returned to avoid
   * creating identical programs.
   *
   * @param name The name of the program to load.
   * @param vertexSource The GLSL source of the vertex shader.
   * @param fragmentSource The GLSL source of the fragment shader.
   * @param defines The values to be defined in each of the shaders.
   * @return The compiled and linked program.
   */
  public static Program load(String name, String vertexSource, String fragmentSource,
      Map<String, String> defines) {
    Program program = programs.get(name);
    if (program == null) {
      program = new Program(name);
      program.compile(name, assembleSource(vertexSource, defines),
          assembleSource(fragmentSource, defines));

      programs.put(name, program);
    }
    return program;
  }

  private static String assembleSource(String source, Map<String, String> defines) {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new StringReader(source));

      boolean added = false;
      String line;
      StringBuilder out = new StringBuilder();
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("#version ")) {
          out.append(line).append('\n');
        }

        if (!added) {
          for (Map.Entry<String, String> define : defines.entrySet()) {
            if (!added) {
              out.append('\n');
              added = true;
            }
            out.append("#define ");
            out.append(define.getKey());
            out.append(' ');
            out.append(define.getValue());
            out.append('\n');
          }
          out.append("#line 2").append('\n');
          added = true;
        }
        if (!line.startsWith("#version ")) {
          out.append(line).append('\n');
        }
      }
      return out.toString();
    } catch (IOException e) {
      return null;
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) { }
      }
    }
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
   * @param name The name of the uniform in the shader.
   * @return The location of the uniform.
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
   * @param name The name of the attribute in the shader.
   * @return The location of the attribute.
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

  /**
   * Binds an integer to the uniform named <code>name</code>.
   * @param name The name of the uniform to bind.
   * @param v The value to bind to the uniform.
   */
  public void bindInt(String name, int v) {
    bindInt(uniformLocation(name), v);
  }

  /**
   * Binds an integer to the uniform at <code>location</code>.
   * @param location The location of the uniform to bind.
   * @param v The value to bind to the uniform.
   */
  public void bindInt(int location, int v) {
    if (location < 0) {
      GlUtil.logger.log(String.format("%s: Invalid uniform location: %d", getTag(), location));
      return;
    }
    glUniform1i(location, v);
  }

  /**
   * Binds a float to the uniform named <code>name</code>.
   * @param name The name of hte uniform to bind.
   * @param v The value to bind to the uniform.
   */
  public void bindFloat(String name, float v) {
    bindFloat(uniformLocation(name), v);
  }

  /**
   * Binds a float to the uniform at <code>location</code>.
   * @param location The location of the uniform to bind.
   * @param v the value to bind to the uniform.
   */
  public void bindFloat(int location, float v) {
    if (location < 0) {
      GlUtil.logger.log(String.format("%s: Invalid uniform location: %d", getTag(), location));
      return;
    }
    glUniform1f(location, v);
  }

  /**
   * Binds 3 floats to the vec3 uniform named <code>name</code>.
   * @param name The name of the uniform to bind.
   * @param x The x value to bind to the uniform.
   * @param y The y value to bind to the uniform.
   * @param z The z value to bind to the uniform.
   */
  public void bindFloat3(String name, float x, float y, float z) {
    bindFloat3(uniformLocation(name), x, y, z);
  }

  /**
   * Binds 3 floats to the vec3 uniform at <code>location</code>.
   * @param location The location of the uniform to bind.
   * @param x The x value to bind to the uniform.
   * @param y The y value to bind to the uniform.
   * @param z The z value to bind to the uniform.
   */
  public void bindFloat3(int location, float x, float y, float z) {
    if (location < 0) {
      GlUtil.logger.log(String.format("%s: Invalid uniform location: %d", getTag(), location));
      return;
    }
    glUniform3f(location, x, y, z);
  }

  /**
   * Binds 4 floats to the vec4 uniform named <code>name</code>.
   * @param name The name of the uniform to bind.
   * @param x The x value to bind to the uniform.
   * @param y The y value to bind to the uniform.
   * @param z The z value to bind to the uniform.
   * @param w The w value to bind to the uniform.
   */
  public void bindFloat4(String name, float x, float y, float z, float w) {
    bindFloat4(uniformLocation(name), x, y, z, w);
  }

  /**
   * Binds 4 floats to the vec4 uniform at <code>location</code>.
   * @param location The location of the uniform to bind.
   * @param x The x value to bind to the uniform.
   * @param y The y value to bind to the uniform.
   * @param z The z value to bind to the uniform.
   * @param w The w value to bind to the uniform.
   */
  public void bindFloat4(int location, float x, float y, float z, float w) {
    if (location < 0) {
      GlUtil.logger.log(String.format("%s: Invalid uniform location: %d", getTag(), location));
      return;
    }
    glUniform4f(location, x, y, z, w);
  }

  /**
   * Binds a matrix to the uniform named <code>name</code>.
   * @param name The name of the uniform to bind.
   * @param m The matrix to bind to the uniform.
   */
  public void bindMatrix(String name, float[] m) {
    bindMatrix(uniformLocation(name), m);
  }

  /**
   * Binds a matrix to the uniform at <code>location</code>.
   * @param location The location of the uniform to bind.
   * @param m The matrix to bind to the uniform.
   */
  public void bindMatrix(int location, float[] m) {
    if (location < 0) {
      GlUtil.logger.log(String.format("%s: Invalid uniform location: %d", getTag(), location));
      return;
    }
    glUniformMatrix4fv(location, 1, false, m, 0);
  }

  /**
   * Binds a 2 float array to the uniform named <code>name</code>.
   * @param name The name of the uniform to bind.
   * @param v The 2 float array to bind to the uniform.
   */
  public void bindFloat2Array(String name, float[] v) {
    bindFloat2Array(uniformLocation(name), v);
  }

  /**
   * Binds a 2 float array to the uniform at <code>location</code>.
   * @param location The location of the uniform to bind.
   * @param v The 2 float array to bind to the uniform.
   */
  public void bindFloat2Array(int location, float[] v) {
    if (location < 0) {
      GlUtil.logger.log(String.format("%s: Invalid uniform location: %d", getTag(), location));
      return;
    }
    glUniform2fv(location, 1, v, 0);
  }

  /**
   * Binds a 4 float array to the uniform named <code>name</code>.
   * @param name The name of the uniform to bind.
   * @param v The 4 float array to bind to the uniform.
   */
  public void bindFloat4Array(String name, float[] v) {
    bindFloat4Array(uniformLocation(name), v);
  }

  /**
   * Binds a 4 float array to the uniform at <code>location</code>.
   * @param location The location of the uniform to bind.
   * @param v The 4 float array to bind to the uniform.
   */
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
