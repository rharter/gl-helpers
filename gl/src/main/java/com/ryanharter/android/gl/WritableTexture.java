package com.ryanharter.android.gl;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_COLOR_ATTACHMENT0;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_ATTACHMENT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_COMPONENT16;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_FRAMEBUFFER_BINDING;
import static android.opengl.GLES20.GL_FRAMEBUFFER_COMPLETE;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_RENDERBUFFER;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.GL_VIEWPORT;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindRenderbuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glCheckFramebufferStatus;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDeleteBuffers;
import static android.opengl.GLES20.glFramebufferRenderbuffer;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGenRenderbuffers;
import static android.opengl.GLES20.glGetIntegerv;
import static android.opengl.GLES20.glRenderbufferStorage;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES20.glViewport;

/**
 * An OpenGL texture that also has the appropriate framebuffers so that
 * it can be written to, as well as read from.
 */
public class WritableTexture extends Texture {

  private final int[] temp = new int[16];

  private final int[] buffers = new int[2];
  protected final int width, height;

  private int defaultFramebufferId;
  private final int[] defaultViewportSize = new int[4];

  public WritableTexture(int width, int height) {
    this(width, height, false);
  }

  public WritableTexture(int width, int height, boolean hasDepth) {
    super();
    this.width = width;
    this.height = height;

    // record the old values
    glGetIntegerv(GL_VIEWPORT, defaultViewportSize, 0);
    glGetIntegerv(GL_FRAMEBUFFER_BINDING, temp, 0);
    defaultFramebufferId = temp[0];

    // generate the fbo and texture
    glGenFramebuffers(1, buffers, 0);

    bind(0);

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    // create the texture in memory
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);

    // unbind the texture before attaching it to the framebuffer
    glBindTexture(GL_TEXTURE_2D, 0);

    // create the framebuffer
    glBindFramebuffer(GL_FRAMEBUFFER, buffers[0]);

    // attach the texture buffer to color
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, getName(), 0);

    if (hasDepth) {
      glGenRenderbuffers(1, buffers, 1);

      // create and bind the depth buffer
      glBindRenderbuffer(GL_RENDERBUFFER, buffers[1]);
      glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, width, height);
      glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, buffers[1]);
    } else {
      buffers[1] = -1;
    }

    int error = glCheckFramebufferStatus(GL_FRAMEBUFFER);
    if (error != GL_FRAMEBUFFER_COMPLETE) {
      GlUtil.logger.log(String.format("Failed to make complete Framebuffer: 0x%s", Integer.toHexString(error)));
      return;
    }

    // clear the texture
    glClearColor(0, 0, 0, 0);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    // reset the old framebuffer and viewport
    glBindFramebuffer(GL_FRAMEBUFFER, defaultFramebufferId);
    glViewport(defaultViewportSize[0], defaultViewportSize[1], defaultViewportSize[2],
        defaultViewportSize[3]);
  }

  /**
   * Binds the frame buffer of this texture for writing.
   */
  public void bindFramebuffer() {
    // get the old values
    glGetIntegerv(GL_VIEWPORT, defaultViewportSize, 0);
    glGetIntegerv(GL_FRAMEBUFFER_BINDING, temp, 0);
    defaultFramebufferId = temp[0];

    // bind the framebuffer
    glBindFramebuffer(GL_FRAMEBUFFER, buffers[0]);
    glViewport(0, 0, width, height);
  }

  /**
   * Unbinds the current framebuffer and restores the previous viewport and framebuffer state.
   */
  public void unbindFramebuffer() {
    unbindFramebuffer(true);
  }

  /**
   * Unbinds the current framebuffer.
   *
   * @param restoreState True to restore the previous viewport/framebuffer state, false to
   * restore the default framebuffer;
   */
  public void unbindFramebuffer(boolean restoreState) {
    if (restoreState) {
      glBindFramebuffer(GL_FRAMEBUFFER, defaultFramebufferId);
      glViewport(defaultViewportSize[0], defaultViewportSize[1], defaultViewportSize[2],
          defaultViewportSize[3]);
    } else {
      glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
  }

  @Override public void destroy() {
    super.destroy();
    glDeleteBuffers(2, buffers, 0);
  }
}
