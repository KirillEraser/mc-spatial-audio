#include "SpatialOutput.hpp"
#include "ChannelOrder.hpp"
#import <AppKit/AppKit.h>
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstdlib>
#include <iostream>
#include <stdexcept>
#include <string>

namespace {
struct ToneState { std::uint64_t frame = 0; };
constexpr double sampleRate = 48000.0;
constexpr double pi = 3.14159265358979323846;

// Same callback contract and OpenAL channel order as the Minecraft bridge.
void renderTone(void* opaque, void* destination, int frames) {
    auto& state = *static_cast<ToneState*>(opaque);
    auto* output = static_cast<float*>(destination);
    std::fill_n(output, static_cast<std::size_t>(frames) * mcspatial::channelCount, 0.0f);
    for (int i = 0; i < frames; ++i, ++state.frame) {
        constexpr std::uint64_t channelFrames = 96000; // two seconds per channel
        std::size_t channel = (state.frame / channelFrames) % mcspatial::channelCount;
        double channelTime = (state.frame % channelFrames) / sampleRate;
        double envelope = std::min({1.0, channelTime / 0.05, (2.0 - channelTime) / 0.05});
        double frequency = channel == 3 ? 80.0 : 440.0;
        output[i * mcspatial::channelCount + channel] = static_cast<float>(
            0.025 * envelope * std::sin(2.0 * pi * frequency * (state.frame / sampleRate)));
    }
}
}

int main(int argc, char** argv) {
    @autoreleasepool {
        try {
            bool headTracking = true;
            int seconds = 32;
            for (int i = 1; i < argc; ++i) {
                std::string argument = argv[i];
                if (argument == "--fixed") headTracking = false;
                else if (argument == "--seconds" && i + 1 < argc) {
                    std::string value = argv[++i];
                    std::size_t consumed = 0;
                    seconds = std::stoi(value, &consumed);
                    if (consumed != value.size() || seconds < 1 || seconds > 600) {
                        throw std::runtime_error("--seconds must be an integer from 1 to 600");
                    }
                } else {
                    throw std::runtime_error("Usage: MCSpatialProbe [--fixed] [--seconds 1..600]");
                }
            }
            [NSApplication sharedApplication];
            [NSApp setActivationPolicy:NSApplicationActivationPolicyAccessory];
            std::cout << "Native spatial audio probe. Start with a low system volume.\n"
                      << "Two seconds each: front L, front R, center, LFE, rear L, rear R, side L, side R.\n"
                      << "This application does not create a menu-bar icon.\n";
            ToneState state;
            auto audio = mcspatial::SpatialOutput::create(&renderTone, headTracking);
            audio->attach(&state);
            std::cout << audio->diagnostics() << std::endl;
            NSDate* deadline = [NSDate dateWithTimeIntervalSinceNow:seconds];
            while ([deadline timeIntervalSinceNow] > 0) {
                [[NSRunLoop currentRunLoop] runUntilDate:[NSDate dateWithTimeIntervalSinceNow:0.05]];
            }
            std::cout << audio->diagnostics() << std::endl;
            // Explicitly stop callbacks before ToneState is destroyed.
            audio.reset();
            return EXIT_SUCCESS;
        } catch (const std::exception& failure) {
            std::cerr << "Probe failed: " << failure.what() << std::endl;
            return EXIT_FAILURE;
        }
    }
}
