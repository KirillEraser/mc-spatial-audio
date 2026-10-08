#include "ChannelOrder.hpp"
#include <array>
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
        std::cout << "Channel order tests passed\n";
        return 0;
    } catch (const std::exception& failure) {
        std::cerr << failure.what() << '\n';
        return 1;
    }
}
