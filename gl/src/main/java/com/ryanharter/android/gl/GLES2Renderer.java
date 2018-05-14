package com.ryanharter.android.gl;

import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glVertexAttribPointer;

final class GLES2Renderer implements Renderer {

  private static final FloatBuffer QUAD_VERTICES = GlUtil.createFloatBuffer(new float[] {
      1, -1, 1, 1, -1, -1, -1, 1
  });

  @Override public void render() {
    GLState.INSTANCE.setAttributeEnabled(0, true);
    glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, QUAD_VERTICES.rewind());
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    GLState.INSTANCE.setAttributeEnabled(0, false);
  }
}
