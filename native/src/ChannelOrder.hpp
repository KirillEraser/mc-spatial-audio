#pragma once
#include <cstddef>
#include <utility>

namespace mcspatial {
inline constexpr std::size_t channelCount = 8;

// OpenAL Soft 7.1: L R C LFE BL BR SL SR.
// Apple's MPEG_7_1_C: L R C LFE Ls Rs Rls Rrs.
// Move side channels before rear channels. This permutation is its own inverse.
inline void openAlToApple71(float* interleaved, std::size_t frames) noexcept {
    for (std::size_t frame = 0; frame < frames; ++frame) {
        float* samples = interleaved + frame * channelCount;
        std::swap(samples[4], samples[6]);
        std::swap(samples[5], samples[7]);
    }
}
}
