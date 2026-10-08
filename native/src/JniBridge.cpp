#include "SpatialOutput.hpp"
#include <jni.h>
#include <cstdint>
#include <exception>
#include <string>

namespace {
thread_local std::string error;
mcspatial::SpatialOutput* instance(jlong handle) noexcept {
    return reinterpret_cast<mcspatial::SpatialOutput*>(static_cast<std::uintptr_t>(handle));
}
}

extern "C" {
JNIEXPORT jint JNICALL Java_dev_kirilleraser_mcspatial_NativeBridge_abiVersion(JNIEnv*, jclass) {
    return 1;
}
JNIEXPORT jlong JNICALL Java_dev_kirilleraser_mcspatial_NativeBridge_create(JNIEnv*, jclass,
    jlong renderFunction, jboolean requested) {
    try {
        error.clear();
        auto render = reinterpret_cast<mcspatial::RenderSamples>(static_cast<std::uintptr_t>(renderFunction));
        auto output = mcspatial::SpatialOutput::create(render, requested == JNI_TRUE);
        return static_cast<jlong>(reinterpret_cast<std::uintptr_t>(output.release()));
    } catch (const std::exception& failure) {
        error = failure.what();
        return 0;
    } catch (...) {
        error = "Unknown native initialization failure";
        return 0;
    }
}
JNIEXPORT void JNICALL Java_dev_kirilleraser_mcspatial_NativeBridge_attach(JNIEnv*, jclass,
    jlong handle, jlong device) {
    if (handle) instance(handle)->attach(reinterpret_cast<void*>(static_cast<std::uintptr_t>(device)));
}
JNIEXPORT void JNICALL Java_dev_kirilleraser_mcspatial_NativeBridge_destroy(JNIEnv*, jclass, jlong handle) {
    delete instance(handle);
}
JNIEXPORT jstring JNICALL Java_dev_kirilleraser_mcspatial_NativeBridge_lastError(JNIEnv* env, jclass) {
    return env->NewStringUTF(error.c_str());
}
JNIEXPORT jstring JNICALL Java_dev_kirilleraser_mcspatial_NativeBridge_diagnostics(JNIEnv* env, jclass, jlong handle) {
    try {
        std::string result = handle ? instance(handle)->diagnostics() : "Native route not started";
        return env->NewStringUTF(result.c_str());
    } catch (const std::exception& failure) {
        return env->NewStringUTF(failure.what());
    }
}
}
