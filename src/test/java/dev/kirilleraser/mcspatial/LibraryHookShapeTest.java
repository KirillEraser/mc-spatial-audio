package dev.kirilleraser.mcspatial;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import java.io.IOException;
import java.io.InputStream;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Check the actual mapped Minecraft bytecode on the test classpath.
 * This does not initialize Minecraft/OpenAL and does NOT replace a real Mixin/client launch test.
 */
final class LibraryHookShapeTest {
    private static ClassNode library() throws IOException {
        String path = "com/mojang/blaze3d/audio/Library.class";
        try (InputStream input = LibraryHookShapeTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "Mapped Minecraft Library bytecode is missing from the test runtime");
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(candidate -> candidate.name.equals(name)).findFirst()
            .orElseThrow(() -> new AssertionError("Missing Minecraft Library method: " + name));
    }

    private static boolean invokes(MethodNode method, String owner, String name, String descriptor) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner)
                    && call.name.equals(name) && call.desc.equals(descriptor)) return true;
        }
        return false;
    }

    @Test void deviceOpenRedirectHasExpectedStaticInvocation() throws IOException {
        MethodNode open = method(library(), "tryOpenDevice");
        assertNotEquals(0, open.access & Opcodes.ACC_STATIC, "Open redirect handler assumes a static target method");
        assertTrue(invokes(open, "org/lwjgl/openal/ALC10", "alcOpenDevice", "(Ljava/lang/CharSequence;)J"),
            "Update LibraryMixin's open-device invocation target for the actual Minecraft bytecode");
    }

    @Test void contextCreationCanMergeExistingAttributes() throws IOException {
        MethodNode init = method(library(), "init");
        assertEquals(0, init.access & Opcodes.ACC_STATIC);
        assertTrue(init.desc.endsWith(")V"), "Init-tail callback assumes a void target");
        assertTrue(invokes(init, "org/lwjgl/openal/ALC10", "alcCreateContext", "(JLjava/nio/IntBuffer;)J"),
            "Update LibraryMixin's context-attribute invocation target");
    }

    @Test void cleanupCanStopBeforeDeviceClose() throws IOException {
        MethodNode cleanup = method(library(), "cleanup");
        assertEquals(0, cleanup.access & Opcodes.ACC_STATIC);
        assertEquals("()V", cleanup.desc);
        assertTrue(invokes(cleanup, "org/lwjgl/openal/ALC10", "alcCloseDevice", "(J)Z"),
            "Update LibraryMixin's device-close invocation target");
    }
}
