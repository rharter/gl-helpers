cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -fno-rtti -fno-exceptions -Wall")
set(glhelper_SRCS src/main/jni/gles2fix.c)

add_library(glhelper SHARED ${glhelper_SRCS})
target_link_libraries(glhelper android log m jnigraphics EGL GLESv2)
