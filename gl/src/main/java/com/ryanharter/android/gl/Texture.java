package com.ryanharter.android.gl;

import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glGenTextures;

/**
 * Created by rharter on 4/9/14.
 */
public class Texture {

  private int bindUnit = -1;
  private int[] textureId = new int[1];

  public Texture() {
    glGenTextures(1, textureId, 0);
  }

  public void bind(int unit) {
    bindUnit = unit;
    GLState.bindTexture(unit, GL_TEXTURE_2D, textureId[0]);
  }

  public void unbind() {
    if (bindUnit > -1) {
      GLState.bindTexture(bindUnit, GL_TEXTURE_2D, 0);
    }
  }

  @Deprecated public int getTextureId() {
    return getName();
  }

  public int getName() {
    return textureId[0];
  }

  public void destroy() {
    glDeleteTextures(1, textureId, 0);
  }

}
