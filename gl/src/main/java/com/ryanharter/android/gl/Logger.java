package com.ryanharter.android.gl;

public interface Logger {
    void log(String message);

    class VoidLogger implements Logger {
        @Override public void log(String message) {

        }
    }
}
