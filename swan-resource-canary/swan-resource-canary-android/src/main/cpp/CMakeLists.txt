# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.18.1)

# Declares and names the project.

set(PROJECT_NAME "swan-resource")

project(${PROJECT_NAME})

file(GLOB SWAN_SRC
        common/*.c
        resource.cpp)

set(THIRD_PARTY_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../swan-third-libs)

include_directories(
        ${THIRD_PARTY_DIR}/xhook/src/main/cpp/
        ${THIRD_PARTY_DIR}/xdl/src/main/cpp/include/
)

link_directories(
        ${THIRD_PARTY_DIR}/xhook/src/main/libs/${ANDROID_ABI}/
        ${THIRD_PARTY_DIR}/xdl/src/main/libs/${ANDROID_ABI}/
)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        ${PROJECT_NAME}

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${SWAN_SRC})

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

target_include_directories(${PROJECT_NAME} PUBLIC
        common
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        ${PROJECT_NAME}
        swan-xhook
        xdl
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})