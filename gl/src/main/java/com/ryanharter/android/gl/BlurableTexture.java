package com.ryanharter.android.gl;

/**
 * Created by rharter on 2/7/15.
 */
public class BlurableTexture extends WritableTexture {

  private BlurProgram blurProgram;

  private final float[] texelOffset = new float[2];
  private WritableTexture intTexture;

  public BlurableTexture(int width, int height, boolean hasDepth) {
    super(width, height, hasDepth, false);
    intTexture = new WritableTexture(width, height, false, false);

    switch (GLState.getGlVersion()) {
      case GLES_20:
        blurProgram = new GLES2BlurProgram();
        break;
      case GLES_30:
        blurProgram = new GLES3BlurProgram();
        break;
    }
  }

  /**
   * Blurs the image contained in the texture.
   *
   * @param amount The amount of blur to apply, from 0.0 to 1.0
   * @param quality The quality of the blur.
   */
  public void blur(float amount, int quality) {
    amount *= 0.01;

    final float aspect = (float) getWidth() / getHeight();
    float incrementAmount = amount;
    for (int i = 0; i < quality; i++) {
      // bind the intermediate texture
      intTexture.bindFramebuffer();

      // No blending since we're overwriting the texture
      GLState.setBlend(false, false);

      // draw to the intermediate texture, blurring
      // in the y direction
      blurProgram.program().use();

      bind(0);
      blurProgram.bindImage(0);

      texelOffset[0] = 0;
      texelOffset[1] = incrementAmount * aspect;
      blurProgram.bindTexelOffset(texelOffset);

      GLState.render();

      intTexture.unbindFramebuffer(true);

      // now draw intermediate texture back into this,
      // blurring in x direction
      bindFramebuffer();

      intTexture.bind(0);
      blurProgram.bindImage(0);

      texelOffset[0] = incrementAmount;
      texelOffset[1] = 0;
      blurProgram.bindTexelOffset(texelOffset);

      GLState.render();
      unbindFramebuffer(true);

      incrementAmount = amount / quality;
    }
  }

  private interface BlurProgram {
    Program program();
    void bindImage(int texture);
    void bindTexelOffset(float[] texelOffset);
  }

  private static class GLES2BlurProgram implements BlurProgram {

    private static final String VERTEX_SHADER = ""
        + "attribute vec4 vertexAttribPosition;\n"
        + "varying highp vec2 v_textureCoordinate;\n"
        + "void main()\n"
        + "{\n"
        + "    v_textureCoordinate = vertexAttribPosition.xy * 0.5 + 0.5;\n"
        + "    gl_Position = vertexAttribPosition;\n"
        + "}\n";

    private static final String FRAGMENT_SHADER = ""
        + "uniform sampler2D inputImageTexture;\n"
        + "uniform vec2 texelOffset;\n"
        + "varying highp vec2 v_textureCoordinate;\n"
        + "void main()\n"
        + "{\n"
        + "    lowp vec4 sum = vec4(0.0);\n"
        + "    sum += texture2D(inputImageTexture, v_textureCoordinate) * 0.1642;\n"
        + "    sum += texture2D(inputImageTexture, v_textureCoordinate + texelOffset) * 0.1531;\n"
        + "    sum += texture2D(inputImageTexture, v_textureCoordinate - texelOffset) * 0.1531;\n"
        + "    sum += texture2D(inputImageTexture, v_textureCoordinate + texelOffset * 1.8) * 0.1224;\n"
        + "    sum += texture2D(inputImageTexture, v_textureCoordinate - texelOffset * 1.8) * 0.1224;\n"
        + "    sum += texture2D(inputImageTexture, v_textureCoordinate + texelOffset * 2.2) * 0.0918;\n"
        + "    sum += texture2D(inputImageTexture, v_textureCoordinate - texelOffset * 2.2) * 0.0918;\n"
        + "    sum += texture2D(inputImageTexture, v_textureCoordinate + texelOffset * 2.6) * 0.0510;\n"
        + "    sum += texture2D(inputImageTexture, v_textureCoordinate - texelOffset * 2.6) * 0.0510;\n"
        + "    gl_FragColor = sum;\n"
        + "}";

    private final Program program;

    private GLES2BlurProgram() {
      this.program = Program.load(GLES2BlurProgram.class.getSimpleName(), VERTEX_SHADER, FRAGMENT_SHADER);
    }

    @Override public Program program() {
      return program;
    }

    @Override public void bindImage(int texture) {
      program.bindInt(program.uniformLocation("inputImageTexture"), texture);
    }

    @Override public void bindTexelOffset(float[] texelOffset) {
      program.bindFloat2Array(program.uniformLocation("texelOffset"), texelOffset);
    }
  }

  private static class GLES3BlurProgram implements BlurProgram {

    private static final String VERTEX_SHADER = ""
        + "#version 300 es\n"
        + "layout(location = 0) in vec4 vertexAttribPosition;\n"
        + "layout(location = 0) uniform vec2 texelOffset;\n"
        + "out highp vec2 v_textureCoordinate;\n"
        + "out vec2 blurCoordinates[9];\n"
        + "void main()\n"
        + "{\n"
        + "    vec2 inputTextureCoordinate = vertexAttribPosition.xy * 0.5 + 0.5;\n"
        + "    blurCoordinates[0] = inputTextureCoordinate.xy;\n"
        + "    blurCoordinates[1] = inputTextureCoordinate.xy + texelOffset;\n"
        + "    blurCoordinates[2] = inputTextureCoordinate.xy - texelOffset;\n"
        + "    blurCoordinates[3] = inputTextureCoordinate.xy + texelOffset * 2.0;\n"
        + "    blurCoordinates[4] = inputTextureCoordinate.xy - texelOffset * 2.0;\n"
        + "    blurCoordinates[5] = inputTextureCoordinate.xy + texelOffset * 3.0;\n"
        + "    blurCoordinates[6] = inputTextureCoordinate.xy - texelOffset * 3.0;\n"
        + "    blurCoordinates[7] = inputTextureCoordinate.xy + texelOffset * 4.0;\n"
        + "    blurCoordinates[8] = inputTextureCoordinate.xy - texelOffset * 4.0;\n"
        + "    gl_Position = vertexAttribPosition;\n"
        + "}\n";

    private static final String FRAGMENT_SHADER = ""
        + "#version 300 es\n"
        + "layout(location = 1) uniform sampler2D inputImageTexture;\n"
        + "in vec2 blurCoordinates[9];\n"
        + "out vec4 fragmentColor;\n"
        + "void main()\n"
        + "{\n"
        + "    lowp vec4 sum = vec4(0.0);\n"
        + "    sum += texture(inputImageTexture, blurCoordinates[0]) * 0.1642;\n"
        + "    sum += texture(inputImageTexture, blurCoordinates[1]) * 0.1531;\n"
        + "    sum += texture(inputImageTexture, blurCoordinates[2]) * 0.1531;\n"
        + "    sum += texture(inputImageTexture, blurCoordinates[3]) * 0.1224;\n"
        + "    sum += texture(inputImageTexture, blurCoordinates[4]) * 0.1224;\n"
        + "    sum += texture(inputImageTexture, blurCoordinates[3]) * 0.0918;\n"
        + "    sum += texture(inputImageTexture, blurCoordinates[4]) * 0.0918;\n"
        + "    sum += texture(inputImageTexture, blurCoordinates[3]) * 0.0510;\n"
        + "    sum += texture(inputImageTexture, blurCoordinates[4]) * 0.0510;\n"
        + "    fragmentColor = sum;\n"
        + "}";

    private final Program program;

    private GLES3BlurProgram() {
      this.program = Program.load(GLES3BlurProgram.class.getSimpleName(), VERTEX_SHADER, FRAGMENT_SHADER);
    }

    @Override public Program program() {
      return program;
    }

    @Override public void bindImage(int texture) {
      program.bindInt(1, texture);
    }

    @Override public void bindTexelOffset(float[] texelOffset) {
      program.bindFloat2Array(0, texelOffset);
    }
  }
}
