cmake_minimum_required(VERSION 3.31.6)
project("sigma-eclipse-llm" LANGUAGES C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# llama.cpp is built as a native Android dependency. We intentionally link the
# CPU backend statically so the APK does not need to spawn a localhost server
# or execute an external binary from app storage.
set(LLAMA_BUILD_COMMON ON CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_OPENSSL OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_CPU_ALL_VARIANTS OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)

if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(GGML_CPU_KLEIDIAI ON CACHE BOOL "" FORCE)
else()
    set(GGML_CPU_KLEIDIAI OFF CACHE BOOL "" FORCE)
endif()

include(FetchContent)

FetchContent_Declare(
    llama_cpp
    GIT_REPOSITORY https://github.com/ggml-org/llama.cpp.git
    GIT_TAG ece963f41b0b02d7a0d61436ae365762c073a4c8
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(llama_cpp)

# Reuse the Android JNI implementation maintained by the upstream llama.cpp
# Android example. This keeps the native inference path aligned with the
# upstream API instead of maintaining a forked inference loop here.
set(UPSTREAM_ANDROID_CPP
    ${llama_cpp_SOURCE_DIR}/examples/llama.android/lib/src/main/cpp/ai_chat.cpp)

add_library(ai-chat SHARED ${UPSTREAM_ANDROID_CPP})

target_include_directories(ai-chat PRIVATE
    ${llama_cpp_SOURCE_DIR}
    ${llama_cpp_SOURCE_DIR}/common
    ${llama_cpp_SOURCE_DIR}/include
    ${llama_cpp_SOURCE_DIR}/ggml/include
    ${llama_cpp_SOURCE_DIR}/ggml/src
)

target_link_libraries(ai-chat
    llama
    llama-common
    android
    log
)

target_compile_options(ai-chat PRIVATE
    -O3
    -ffast-math
)
