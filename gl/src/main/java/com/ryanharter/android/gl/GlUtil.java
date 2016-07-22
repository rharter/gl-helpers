package com.ryanharter.android.gl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_NO_ERROR;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;

/**
 * Some OpenGL utility functions.
 *
 * TODO Is this still needed?
 */
public class GlUtil {

    static Logger logger = GLState.logger;

    /** Identity matrix for general bind.  Don't modify or life will get weird. */
    public static final float[] IDENTITY_MATRIX = new float[] {
        1, 0, 0, 0,
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1,
    };

    private static final int SIZEOF_FLOAT = 4;


    private GlUtil() {}     // do not instantiate

    /**
     * Checks to see if a GLES error has been raised.
     */
    public static void checkError() {
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            // generate a stack trace one level before this.
            Exception e = new Exception("GL error: 0x" + Integer.toHexString(error));
            StackTraceElement[] st = e.getStackTrace();
            e.setStackTrace(Arrays.copyOfRange(st, 1, st.length));

            if (BuildConfig.DEBUG) {
                throw new RuntimeException(e);
            }

            logger.log(e.getMessage());
        }
    }

    /**
     * Creates a texture from raw data.
     *
     * @param data Image data, in a "direct" ByteBuffer.
     * @param width Texture width, in pixels (not bytes).
     * @param height Texture height, in pixels.
     * @param format Image data format (bind constant appropriate for glTexImage2D(), e.g. GL_RGBA).
     * @return Handle to texture.
     */
    public static int createImageTexture(ByteBuffer data, int width, int height, int format) {
        int[] textureHandles = new int[1];
        int textureHandle;

        glGenTextures(1, textureHandles, 0);
        textureHandle = textureHandles[0];
        checkError();

        // Bind the texture handle to the 2D texture target.
        glBindTexture(GL_TEXTURE_2D, textureHandle);

        // Configure min/mag filtering, i.e. what scaling method do we bind if what we're rendering
        // is smaller or larger than the source image.
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        checkError();

        // Load the data from the buffer into the texture handle.
        glTexImage2D(GL_TEXTURE_2D, /*level*/ 0, format,
                width, height, /*border*/ 0, format, GL_UNSIGNED_BYTE, data);
        checkError();

        return textureHandle;
    }

    public static float[] newIdentityMatrix() {
        return Arrays.copyOf(IDENTITY_MATRIX, 16);
    }

    /**
     * Allocates a direct float buffer, and populates it with the float array data.
     * @param coords The contents of the float buffer.
     * @return A newly created {@link FloatBuffer} set to position 0.
     */
    public static FloatBuffer createFloatBuffer(float[] coords) {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

}
