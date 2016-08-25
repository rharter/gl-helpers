#include <jni.h>
#include <GLES2/gl2.h>

JNIEXPORT void JNICALL
Java_com_ryanharter_android_gl_GLES2Fix_glReadPixelsPBO(JNIEnv *env, jobject instance,
                                                        jint x, jint y, jint width, jint height,
                                                        GLenum format, GLenum type, jint offset) {
    glReadPixels(x, y, width, height, format, type, (void *) offset);
}