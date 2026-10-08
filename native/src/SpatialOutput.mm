#include "SpatialOutput.hpp"
#include "ChannelOrder.hpp"
#import <Foundation/Foundation.h>
#include <Security/SecTask.h>
#include <algorithm>
#include <cmath>
#include <cstring>
#include <sstream>
#include <stdexcept>

namespace mcspatial {
namespace {
void check(OSStatus status, const char* operation) {
    if (status == noErr) return;
    std::ostringstream message;
    message << operation << " failed (OSStatus " << status << ")";
    throw std::runtime_error(message.str());
}

AudioUnit newUnit(OSType type, OSType subtype) {
    AudioComponentDescription description{};
    description.componentType = type;
    description.componentSubType = subtype;
    description.componentManufacturer = kAudioUnitManufacturer_Apple;
    AudioComponent component = AudioComponentFindNext(nullptr, &description);
    if (!component) throw std::runtime_error("Required Apple audio component was not found");
    AudioUnit unit = nullptr;
    check(AudioComponentInstanceNew(component, &unit), "Create audio unit");
    return unit;
}

template<class T>
void property(AudioUnit unit, AudioUnitPropertyID key, AudioUnitScope scope, AudioUnitElement bus, const T& value) {
    check(AudioUnitSetProperty(unit, key, scope, bus, &value, sizeof(value)), "Set audio-unit property");
}

AudioDeviceID defaultOutput() {
    AudioDeviceID device = kAudioObjectUnknown;
    UInt32 size = sizeof(device);
    AudioObjectPropertyAddress address{kAudioHardwarePropertyDefaultOutputDevice, kAudioObjectPropertyScopeGlobal, kAudioObjectPropertyElementMain};
    check(AudioObjectGetPropertyData(kAudioObjectSystemObject, &address, 0, nullptr, &size, &device), "Get default output device");
    if (device == kAudioObjectUnknown) throw std::runtime_error("There is no default macOS audio output device");
    return device;
}

bool hasHeadPoseEntitlement() {
    SecTaskRef task = SecTaskCreateFromSelf(kCFAllocatorDefault);
    if (!task) return false;
    CFTypeRef value = SecTaskCopyValueForEntitlement(task, CFSTR("com.apple.developer.coremotion.head-pose"), nullptr);
    bool enabled = value && CFGetTypeID(value) == CFBooleanGetTypeID() && CFBooleanGetValue(static_cast<CFBooleanRef>(value));
    if (value) CFRelease(value);
    CFRelease(task);
    return enabled;
}

AudioStreamBasicDescription floatFormat(Float64 rate, UInt32 channels, bool nonInterleaved) {
    AudioStreamBasicDescription format{};
    format.mSampleRate = rate;
    format.mFormatID = kAudioFormatLinearPCM;
    format.mFormatFlags = kAudioFormatFlagsNativeFloatPacked;
    if (nonInterleaved) format.mFormatFlags |= kAudioFormatFlagIsNonInterleaved;
    format.mBytesPerFrame = sizeof(float) * (nonInterleaved ? 1 : channels);
    format.mBytesPerPacket = format.mBytesPerFrame;
    format.mFramesPerPacket = 1;
    format.mChannelsPerFrame = channels;
    format.mBitsPerChannel = 32;
    return format;
}

void zeroBuffers(AudioBufferList* buffers) noexcept {
    if (!buffers) return;
    for (UInt32 i = 0; i < buffers->mNumberBuffers; ++i) {
        if (buffers->mBuffers[i].mData) {
            std::memset(buffers->mBuffers[i].mData, 0, buffers->mBuffers[i].mDataByteSize);
        }
    }
}
}

SpatialOutput::SpatialOutput(RenderSamples render, bool requested)
    : render_(render), headTrackingRequested_(requested), inputSamples_(maxInputFrames * channelCount, 0.0f) {}

std::unique_ptr<SpatialOutput> SpatialOutput::create(RenderSamples render, bool requested) {
    if (!render) throw std::runtime_error("OpenAL render function pointer is null");
    auto result = std::unique_ptr<SpatialOutput>(new SpatialOutput(render, requested));
    // The owning pointer cleans up even if any initialization operation throws.
    result->initialize();
    return result;
}

void SpatialOutput::initialize() {
    @autoreleasepool {
        if (![[NSProcessInfo processInfo] isOperatingSystemAtLeastVersion:NSOperatingSystemVersion{13, 0, 0}]) {
            throw std::runtime_error("This prototype requires macOS 13 or newer");
        }
        headPoseEntitlement_ = hasHeadPoseEntitlement();
    }
    outputDevice_ = defaultOutput();
    UInt32 rateSize = sizeof(hardwareRate_);
    AudioObjectPropertyAddress rateAddress{kAudioDevicePropertyNominalSampleRate, kAudioObjectPropertyScopeGlobal, kAudioObjectPropertyElementMain};
    check(AudioObjectGetPropertyData(outputDevice_, &rateAddress, 0, nullptr, &rateSize, &hardwareRate_), "Get hardware sample rate");
    if (!std::isfinite(hardwareRate_) || hardwareRate_ < 8000 || hardwareRate_ > 192000) {
        throw std::runtime_error("Unsupported hardware sample rate");
    }

    mixer_ = newUnit(kAudioUnitType_Mixer, kAudioUnitSubType_SpatialMixer);
    converter_ = newUnit(kAudioUnitType_FormatConverter, kAudioUnitSubType_AUConverter);
    output_ = newUnit(kAudioUnitType_Output, kAudioUnitSubType_HALOutput);

    property(output_, kAudioOutputUnitProperty_EnableIO, kAudioUnitScope_Input, 1, UInt32{0});
    property(output_, kAudioOutputUnitProperty_EnableIO, kAudioUnitScope_Output, 0, UInt32{1});
    property(output_, kAudioOutputUnitProperty_CurrentDevice, kAudioUnitScope_Global, 0, outputDevice_);
    property(mixer_, kAudioUnitProperty_ElementCount, kAudioUnitScope_Input, 0, UInt32{1});
    property(mixer_, kAudioUnitProperty_MaximumFramesPerSlice, kAudioUnitScope_Global, 0, maxInputFrames);
    property(converter_, kAudioUnitProperty_MaximumFramesPerSlice, kAudioUnitScope_Global, 0, maxInputFrames);

    auto surround = floatFormat(inputRate, channelCount, false);
    auto stereo = floatFormat(inputRate, 2, true);
    auto hardware = floatFormat(hardwareRate_, 2, true);
    property(mixer_, kAudioUnitProperty_StreamFormat, kAudioUnitScope_Input, 0, surround);
    property(mixer_, kAudioUnitProperty_StreamFormat, kAudioUnitScope_Output, 0, stereo);
    property(converter_, kAudioUnitProperty_StreamFormat, kAudioUnitScope_Input, 0, stereo);
    property(converter_, kAudioUnitProperty_StreamFormat, kAudioUnitScope_Output, 0, hardware);
    property(output_, kAudioUnitProperty_StreamFormat, kAudioUnitScope_Input, 0, hardware);

    AudioChannelLayout inputLayout{};
    inputLayout.mChannelLayoutTag = kAudioChannelLayoutTag_MPEG_7_1_C;
    // For a predefined tag there are no variable channel descriptions.
    check(AudioUnitSetProperty(mixer_, kAudioUnitProperty_AudioChannelLayout, kAudioUnitScope_Input, 0,
        &inputLayout, offsetof(AudioChannelLayout, mChannelDescriptions)), "Set 7.1 input channel layout");
    AudioChannelLayout outputLayout{};
    outputLayout.mChannelLayoutTag = kAudioChannelLayoutTag_Stereo;
    check(AudioUnitSetProperty(mixer_, kAudioUnitProperty_AudioChannelLayout, kAudioUnitScope_Output, 0,
        &outputLayout, offsetof(AudioChannelLayout, mChannelDescriptions)), "Set stereo output channel layout");

    property(mixer_, kAudioUnitProperty_SpatialMixerSourceMode, kAudioUnitScope_Input, 0,
        UInt32{kSpatialMixerSourceMode_AmbienceBed});
    property(mixer_, kAudioUnitProperty_SpatializationAlgorithm, kAudioUnitScope_Input, 0,
        UInt32{kSpatializationAlgorithm_UseOutputType});
    property(mixer_, kAudioUnitProperty_SpatialMixerOutputType, kAudioUnitScope_Global, 0,
        UInt32{kSpatialMixerOutputType_Headphones});

    UInt32 propertySize = 0;
    Boolean writable = false;
    OSStatus headInfo = AudioUnitGetPropertyInfo(mixer_, kAudioUnitProperty_SpatialMixerEnableHeadTracking,
        kAudioUnitScope_Global, 0, &propertySize, &writable);
    if (headTrackingRequested_ && (headInfo != noErr || !writable || propertySize != sizeof(UInt32))) {
        throw std::runtime_error("Spatial mixer does not expose a writable head-tracking property");
    }
    if (headInfo == noErr && writable) {
        property(mixer_, kAudioUnitProperty_SpatialMixerEnableHeadTracking, kAudioUnitScope_Global, 0,
            UInt32{headTrackingRequested_ ? 1u : 0u});
    }

    AURenderCallbackStruct input{&SpatialOutput::inputCallback, this};
    property(mixer_, kAudioUnitProperty_SetRenderCallback, kAudioUnitScope_Input, 0, input);
    AudioUnitConnection connection{mixer_, 0, 0};
    property(converter_, kAudioUnitProperty_MakeConnection, kAudioUnitScope_Input, 0, connection);
    AURenderCallbackStruct output{&SpatialOutput::outputCallback, this};
    property(output_, kAudioUnitProperty_SetRenderCallback, kAudioUnitScope_Input, 0, output);

    check(AudioUnitInitialize(mixer_), "Initialize spatial mixer");
    check(AudioUnitInitialize(converter_), "Initialize sample-rate converter");
    check(AudioUnitInitialize(output_), "Initialize HAL output");
    if (headInfo == noErr) {
        propertySize = sizeof(headTrackingReadback_);
        check(AudioUnitGetProperty(mixer_, kAudioUnitProperty_SpatialMixerEnableHeadTracking,
            kAudioUnitScope_Global, 0, &headTrackingReadback_, &propertySize), "Read back head-tracking request");
    }
    if (headTrackingRequested_ && headTrackingReadback_ != 1) {
        throw std::runtime_error("Spatial mixer did not retain the head-tracking request");
    }
    // Output initially pulls silence, so OpenAL cannot be rendered before its context exists.
    check(AudioOutputUnitStart(output_), "Start HAL output");
    outputStarted_ = true;
}

void SpatialOutput::attach(void* device) noexcept {
    device_.store(device, std::memory_order_release);
}

OSStatus SpatialOutput::inputCallback(void* context, AudioUnitRenderActionFlags* flags,
    const AudioTimeStamp*, UInt32, UInt32 frames, AudioBufferList* buffers) noexcept {
    auto* self = static_cast<SpatialOutput*>(context);
    if (!buffers || buffers->mNumberBuffers != 1 || frames > maxInputFrames) {
        zeroBuffers(buffers);
        self->renderErrors_.fetch_add(1, std::memory_order_relaxed);
        self->lastRenderError_.store(kAudioUnitErr_TooManyFramesToProcess, std::memory_order_relaxed);
        return kAudioUnitErr_TooManyFramesToProcess;
    }
    float* samples = self->inputSamples_.data();
    void* device = self->device_.load(std::memory_order_acquire);
    if (device) {
        self->render_(device, samples, static_cast<int>(frames));
        openAlToApple71(samples, frames);
        self->framesRendered_.fetch_add(frames, std::memory_order_relaxed);
    } else {
        std::fill_n(samples, frames * channelCount, 0.0f);
        if (flags) *flags |= kAudioUnitRenderAction_OutputIsSilence;
    }
    UInt32 bytes = frames * channelCount * sizeof(float);
    AudioBuffer& buffer = buffers->mBuffers[0];
    if (buffer.mData) {
        if (buffer.mDataByteSize < bytes) {
            zeroBuffers(buffers);
            self->renderErrors_.fetch_add(1, std::memory_order_relaxed);
            self->lastRenderError_.store(kAudioUnitErr_TooManyFramesToProcess, std::memory_order_relaxed);
            return kAudioUnitErr_TooManyFramesToProcess;
        }
        std::memcpy(buffer.mData, samples, bytes);
    } else {
        buffer.mData = samples;
    }
    buffer.mNumberChannels = channelCount;
    buffer.mDataByteSize = bytes;
    return noErr;
}

OSStatus SpatialOutput::outputCallback(void* context, AudioUnitRenderActionFlags* flags,
    const AudioTimeStamp* timestamp, UInt32, UInt32 frames, AudioBufferList* buffers) noexcept {
    auto* self = static_cast<SpatialOutput*>(context);
    OSStatus status = AudioUnitRender(self->converter_, flags, timestamp, 0, frames, buffers);
    if (status != noErr) {
        zeroBuffers(buffers);
        if (flags) *flags |= kAudioUnitRenderAction_OutputIsSilence;
        self->renderErrors_.fetch_add(1, std::memory_order_relaxed);
        self->lastRenderError_.store(status, std::memory_order_relaxed);
    }
    return status;
}

std::string SpatialOutput::diagnostics() const {
    bool changed = false;
    try { changed = defaultOutput() != outputDevice_; } catch (...) { changed = true; }
    std::ostringstream text;
    text << "outputDevice=" << outputDevice_ << "; hardwareRate=" << hardwareRate_
         << "; input=48000Hz/float/7.1; headTrackingRequested=" << headTrackingRequested_
         << "; headTrackingPropertyReadback=" << headTrackingReadback_
         << "; processHeadPoseEntitlement=" << headPoseEntitlement_
         << "; framesRendered=" << framesRendered_.load(std::memory_order_relaxed)
         << "; renderErrors=" << renderErrors_.load(std::memory_order_relaxed)
         << "; lastRenderOSStatus=" << lastRenderError_.load(std::memory_order_relaxed)
         << "; defaultOutputChanged=" << changed
         << "; actualTracking=UNVERIFIED; macOSControls=UNVERIFIED";
    return text.str();
}

void SpatialOutput::dispose() noexcept {
    device_.store(nullptr, std::memory_order_release);
    if (output_) {
        if (outputStarted_) AudioOutputUnitStop(output_);
        AudioUnitUninitialize(output_);
        AudioComponentInstanceDispose(output_);
        output_ = nullptr;
    }
    // HAL has been stopped/disposed before freeing its callback's graph or sample storage.
    if (converter_) {
        AudioUnitUninitialize(converter_);
        AudioComponentInstanceDispose(converter_);
        converter_ = nullptr;
    }
    if (mixer_) {
        AudioUnitUninitialize(mixer_);
        AudioComponentInstanceDispose(mixer_);
        mixer_ = nullptr;
    }
}
SpatialOutput::~SpatialOutput() { dispose(); }
}
