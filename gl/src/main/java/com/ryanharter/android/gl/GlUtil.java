package com.ryanharter.android.gl;

import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Build;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_NO_ERROR;
import static android.opengl.GLES20.GL_RENDERER;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TRUE;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.GL_VENDOR;
import static android.opengl.GLES20.GL_VERSION;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glGetString;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;

/**
 * Some OpenGL utility functions.
 */
public class GlUtil {

    static Logger logger = new Logger.VoidLogger();

    public static void setLogger(Logger logger) {
        GlUtil.logger = logger;
    }

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
     * Creates a new program from the supplied vertex and fragment shaders.
     *
     * @param vertexSource The source string of for vertex shader.
     * @param fragmentSource The source string for the fragment shader
     * @return A handle to the program, or 0 on failure.
     */
    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentSource);

        int program = glCreateProgram();
        checkError();
        if (program == 0) {
            throw new RuntimeException("Cannot create GL program: 0x" + Integer.toHexString(
                    glGetError()));
        }

        glAttachShader(program, vertexShader);
        checkError();
        glAttachShader(program, fragmentShader);
        checkError();
        glLinkProgram(program);
        checkError();

        int[] linkStatus = new int[1];
        glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GL_TRUE) {
            logger.log(String.format("Could not link program: %s", glGetProgramInfoLog(program)));
            glDeleteProgram(program);

            throw new RuntimeException("Cannot create GL program: 0x" + Integer.toHexString(
                    glGetError()));
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
    public static int loadShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        int[] compiled = new int[1];
        glGetShaderiv(shader, GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            logger.log(String.format("Could not compile %s shader: %s", getShaderTypeString(type),
                glGetShaderInfoLog(shader)));
            glDeleteShader(shader);
            shader = 0;
        }

        return shader;
    }

    private static String getShaderTypeString(int type) {
        switch (type) {
            case GL_FRAGMENT_SHADER:
                return "fragment";
            case GL_VERTEX_SHADER:
                return "vertex";
            default:
                return String.format("unknown type[0x%s]", Integer.toHexString(type));
        }
    }

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

    /**
     * Writes GL version info to the log.
     */
    public static void logVersionInfo() {
        logger.log(String.format("vendor  : %s", glGetString(GL_VENDOR)));
        logger.log(String.format("renderer: %s", glGetString(GL_RENDERER)));
        logger.log(String.format("version : %s", glGetString(GL_VERSION)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && GLState.getGlVersion() == GLState.GLVersion.GLES_30) {
            int[] values = new int[1];
            GLES30.glGetIntegerv(GLES30.GL_MAJOR_VERSION, values, 0);
            int majorVersion = values[0];
            GLES30.glGetIntegerv(GLES30.GL_MINOR_VERSION, values, 0);
            int minorVersion = values[0];
            if (glGetError() == GL_NO_ERROR) {
                logger.log(String.format("iversion: %d.%d", majorVersion, minorVersion));
            }
        }
    }
}
