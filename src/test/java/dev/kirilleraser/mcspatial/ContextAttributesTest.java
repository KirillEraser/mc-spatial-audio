package dev.kirilleraser.mcspatial;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ContextAttributesTest {
    @Test void preservesAuxiliarySendsAndOverridesFormat() {
        assertArrayEquals(new int[] {0x20003, 4, 0x1007, 48000, 0x1990, 0x1506, 0},
            ContextAttributes.replace(new int[] {0x20003, 4, 0x1007, 44100, 0},
                new int[] {0x1007, 48000, 0x1990, 0x1506, 0}));
    }
    @Test void zeroIsAValidValueNotATerminatorInValuePosition() {
        assertArrayEquals(new int[] {0x1992, 0, 0x1007, 48000, 0},
            ContextAttributes.replace(new int[] {0x1992, 1, 0},
                new int[] {0x1992, 0, 0x1007, 48000, 0}));
    }
    @Test void ignoresDataAfterTheKeyTerminator() {
        assertArrayEquals(new int[] {8, 2, 0},
            ContextAttributes.replace(new int[] {8, 1, 0, 999}, new int[] {8, 2, 0}));
    }
    @Test void normalizesDuplicates() {
        assertArrayEquals(new int[] {8, 3, 0},
            ContextAttributes.replace(new int[] {8, 1, 8, 2, 0}, new int[] {8, 3, 0}));
    }
    @Test void acceptsMissingOriginalAttributes() {
        assertArrayEquals(new int[] {7, 0, 0}, ContextAttributes.replace(null, new int[] {7, 0, 0}));
        assertArrayEquals(new int[] {0}, ContextAttributes.replace(new int[0], null));
    }
    @Test void rejectsIncompletePair() {
        assertThrows(IllegalArgumentException.class,
            () -> ContextAttributes.replace(new int[] {8}, null));
    }
}
