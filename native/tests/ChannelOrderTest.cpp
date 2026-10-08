#include "ChannelOrder.hpp"
#include <array>
#include <cmath>
#include <iostream>
#include <stdexcept>

namespace {
void require(bool condition, const char* message) {
    if (!condition) throw std::runtime_error(message);
}
}

int main() {
    try {
        std::array<float, 18> guarded{99, 0, 1, 2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14, 15, 16, 17, 88};
        mcspatial::openAlToApple71(guarded.data() + 1, 2);
        const std::array<float, 18> expected{99, 0, 1, 2, 3, 6, 7, 4, 5, 10, 11, 12, 13, 16, 17, 14, 15, 88};
        require(guarded == expected, "channel permutation or buffer boundary is incorrect");
        mcspatial::openAlToApple71(guarded.data() + 1, 2);
        const std::array<float, 18> original{99, 0, 1, 2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14, 15, 16, 17, 88};
        require(guarded == original, "permutation must be its own inverse");
        mcspatial::openAlToApple71(nullptr, 0);
        require(guarded == original, "zero-frame conversion modified data");

        // Isolate every source channel, including negative polarity. No boost, attenuation,
        // leakage, normalization or channel duplication is permitted in the bridge swizzle.
        constexpr std::array<std::size_t, 8> destinations{0, 1, 2, 3, 6, 7, 4, 5};
        for (std::size_t source = 0; source < 8; ++source) {
            for (float amplitude : {0.025f, -0.25f, 1.0f}) {
                std::array<float, 8> frame{};
                frame[source] = amplitude;
                mcspatial::openAlToApple71(frame.data(), 1);
                for (std::size_t output = 0; output < 8; ++output) {
                    require(frame[output] == (output == destinations[source] ? amplitude : 0.0f),
                        "channel amplitude, polarity or isolation changed");
                }
            }
        }
        std::array<float, 32> varying{};
        double before = 0;
        for (std::size_t i = 0; i < varying.size(); ++i) {
            varying[i] = static_cast<float>(static_cast<int>(i % 13) - 6) / 8.0f;
            before += static_cast<double>(varying[i]) * varying[i];
        }
        mcspatial::openAlToApple71(varying.data(), 4);
        double after = 0;
        for (float sample : varying) after += static_cast<double>(sample) * sample;
        require(before == after, "surround-bed energy must be unchanged by channel routing");
        std::cout << "Channel order, isolation, polarity and unchanged-gain tests passed\n";
        return 0;
    } catch (const std::exception& failure) {
        std::cerr << failure.what() << '\n';
        return 1;
    }
}
