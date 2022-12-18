# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.18.1)

option(EnableLOG "Enable QUT Logs" ON)
if (EnableLOG)
    add_definitions(-DEnableLOG)
endif ()

# Declares and names the project.

project("hooks")

set(SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR})

MESSAGE(STATUS "SOURCE_DIR: ${SOURCE_DIR}")

set(THIRD_PARTY_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../swan-third-libs)

# 通常是使用 find_libaray 来进行查找三方库
find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

################################## Common Part ##################################
set(TARGET swan-hookcommon)

# 类似 add_executable(生成可执行文件)
# add_library 生成库文件
add_library(${TARGET}
        SHARED
        ${SOURCE_DIR}/common/JNICommon.cpp
        ${SOURCE_DIR}/common/Log.cpp
        ${SOURCE_DIR}/common/SoLoadMonitor.cpp
        ${SOURCE_DIR}/common/ReentrantPrevention.cpp
        ${SOURCE_DIR}/common/HookCommon.cpp
        )


target_include_directories(
        ${TARGET}
        PUBLIC ${THIRD_PARTY_DIR}/xhook/src/main/cpp/
        PUBLIC ${THIRD_PARTY_DIR}/xdl/src/main/cpp/include/
        PUBLIC ${THIRD_PARTY_DIR}/matrix-backtrace/src/main/cpp/libwechatbacktrace/include
        PUBLIC ${THIRD_PARTY_DIR}/matrix-backtrace/src/main/cpp/common/
)

link_directories(
        ${TARGET}
        ${THIRD_PARTY_DIR}/xhook/src/main/libs/${ANDROID_ABI}/
        ${THIRD_PARTY_DIR}/xdl/src/main/libs/${ANDROID_ABI}/
)


################################# Pthread Hook ##################################
set(TARGET swan-pthreadhook)

add_library(
        ${TARGET}
        SHARED
        ${SOURCE_DIR}/pthread/PthreadHookJNI.cpp
        ${SOURCE_DIR}/pthread/ThreadTrace.cpp
        ${SOURCE_DIR}/pthread/PthreadHook.cpp
        ${SOURCE_DIR}/pthread/ThreadStackShink.cpp
)

target_include_directories(
        ${TARGET}
        PUBLIC ${SOURCE_DIR}/common/
)

target_link_libraries(
        ${TARGET}
        PRIVATE swan-hookcommon
        PRIVATE -Wl,--version-script=${SOURCE_DIR}/pthread/pthread.ver
)
#################################################################################