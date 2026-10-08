#pragma once
#include <AudioToolbox/AudioToolbox.h>
#include <CoreAudio/CoreAudio.h>
#include <atomic>
#include <cstdint>
#include <memory>
#include <string>
#include <vector>

namespace mcspatial {
// Signature of alcRenderSamplesSOFT. Use the pointer supplied by LWJGL; never load a second OpenAL library.
using RenderSamples = void (*)(void* device, void* samples, int frames);

class SpatialOutput final {
public:
    static std::unique_ptr<SpatialOutput> create(RenderSamples render, bool headTrackingRequested);
    ~SpatialOutput();
    SpatialOutput(const SpatialOutput&) = delete;
    SpatialOutput& operator=(const SpatialOutput&) = delete;
    void attach(void* device) noexcept;
    std::string diagnostics() const;

private:
    SpatialOutput(RenderSamples render, bool headTrackingRequested);
    void initialize();
    void dispose() noexcept;
    static OSStatus inputCallback(void*, AudioUnitRenderActionFlags*, const AudioTimeStamp*, UInt32, UInt32, AudioBufferList*) noexcept;
    static OSStatus outputCallback(void*, AudioUnitRenderActionFlags*, const AudioTimeStamp*, UInt32, UInt32, AudioBufferList*) noexcept;

    static constexpr UInt32 inputRate = 48000;
    static constexpr UInt32 maxInputFrames = 32768;
    RenderSamples render_;
    bool headTrackingRequested_;
    UInt32 headTrackingReadback_ = 0;
    bool headPoseEntitlement_ = false;
    AudioDeviceID outputDevice_ = kAudioObjectUnknown;
    Float64 hardwareRate_ = 0;
    AudioUnit mixer_ = nullptr;
    AudioUnit converter_ = nullptr;
    AudioUnit output_ = nullptr;
    bool outputStarted_ = false;
    std::vector<float> inputSamples_;
    std::atomic<void*> device_{nullptr};
    std::atomic<std::uint64_t> framesRendered_{0};
    std::atomic<std::uint64_t> renderErrors_{0};
    std::atomic<OSStatus> lastRenderError_{noErr};
};
}
