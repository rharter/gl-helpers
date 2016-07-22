package com.ryanharter.android.gl;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_LINEAR_MIPMAP_LINEAR;
import static android.opengl.GLES20.GL_MIRRORED_REPEAT;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_REPEAT;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES30.glBindSampler;
import static android.opengl.GLES30.glDeleteSamplers;
import static android.opengl.GLES30.glGenSamplers;
import static android.opengl.GLES30.glSamplerParameteri;

public class Sampler {
  public static final int FLAG_LINEAR = 1;
  public static final int FLAG_MIPMAP = 2;
  public static final int FLAG_CLAMP = 3;
  public static final int FLAG_REPEAT = 4;
  public static final int FLAG_MIRROR = 5;

  private final int[] sampler = new int[1];

  public Sampler() {
    this(FLAG_LINEAR | FLAG_MIPMAP | FLAG_REPEAT);
  }

  public Sampler(int flags) {
    boolean mipmap = (flags & FLAG_MIPMAP) == FLAG_MIPMAP;
    int linear = (flags & FLAG_LINEAR) == FLAG_LINEAR ? GL_LINEAR : GL_NEAREST;
    int repeat = GL_REPEAT;
    if ((flags & FLAG_MIRROR) == FLAG_MIRROR) {
      repeat = GL_MIRRORED_REPEAT;
    } else if ((flags & FLAG_CLAMP) == FLAG_CLAMP) {
      repeat = GL_CLAMP_TO_EDGE;
    }

    glGenSamplers(1, sampler, 0);

    glSamplerParameteri(sampler[0], GL_TEXTURE_MIN_FILTER, mipmap ? GL_LINEAR_MIPMAP_LINEAR : linear);
    glSamplerParameteri(sampler[0], GL_TEXTURE_MAG_FILTER, linear);

    glSamplerParameteri(sampler[0], GL_TEXTURE_WRAP_S, repeat);
    glSamplerParameteri(sampler[0], GL_TEXTURE_WRAP_T, repeat);
  }

  public void use(int unit) {
    glBindSampler(unit, sampler[0]);
  }

  public void destroy() {
    glDeleteSamplers(1, sampler, 0);
  }
}
