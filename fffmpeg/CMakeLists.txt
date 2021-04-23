# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

#source_file变量就是src/main/cpp/所有的.cpp文件
file(GLOB source_file src/main/cpp/*.cpp)

add_library(
        fffmpeg-native-lib
             SHARED
             ${source_file} )

include_directories(src/main/cpp/include)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/src/main/cpp/libs/${ANDROID_ABI}")



target_link_libraries(
        fffmpeg-native-lib

        avformat avcodec avfilter avutil swresample swscale
        log z android OpenSLES)