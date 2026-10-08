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

/** Reads mapped bytecode without initializing Minecraft or an audio device. */
final class LibraryHookShapeTest {
    private static ClassNode read(String path) throws IOException {
        try (InputStream input = LibraryHookShapeTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "Mapped bytecode missing: " + path);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static ClassNode library() throws IOException { return read("com/mojang/blaze3d/audio/Library.class"); }
    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(candidate -> candidate.name.equals(name)).findFirst()
            .orElseThrow(() -> new AssertionError("Missing method: " + name));
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
        assertNotEquals(0, open.access & Opcodes.ACC_STATIC);
        assertTrue(invokes(open, "org/lwjgl/openal/ALC10", "alcOpenDevice", "(Ljava/lang/CharSequence;)J"));
    }

    @Test void contextCreationCanMergeExistingAttributes() throws IOException {
        MethodNode init = method(library(), "init");
        assertEquals(0, init.access & Opcodes.ACC_STATIC);
        assertTrue(init.desc.endsWith(")V"));
        assertTrue(invokes(init, "org/lwjgl/openal/ALC10", "alcCreateContext", "(JLjava/nio/IntBuffer;)J"));
    }

    @Test void loopbackHrtfBypassMatchesTheActualMethodAndDeviceField() throws IOException {
        ClassNode node = library();
        MethodNode hrtf = method(node, "setHrtf");
        assertEquals("(Z)V", hrtf.desc);
        assertEquals(0, hrtf.access & Opcodes.ACC_STATIC);
        assertTrue(node.fields.stream().anyMatch(field -> field.name.equals("currentDevice") && field.desc.equals("J")));
        assertTrue(invokes(hrtf, "org/lwjgl/openal/SOFTHRTF", "alcResetDeviceSOFT", "(JLjava/nio/IntBuffer;)Z"));
        assertTrue(invokes(method(node, "init"), "com/mojang/blaze3d/audio/Library", "setHrtf", "(Z)V"));
    }

    @Test void cleanupCanStopBeforeDeviceClose() throws IOException {
        MethodNode cleanup = method(library(), "cleanup");
        assertEquals(0, cleanup.access & Opcodes.ACC_STATIC);
        assertEquals("()V", cleanup.desc);
        assertTrue(invokes(cleanup, "org/lwjgl/openal/ALC10", "alcCloseDevice", "(J)Z"));
    }

    @Test void audioReloadIsAPublicNoArgumentMethod() throws IOException {
        ClassNode manager = read("net/minecraft/client/sounds/SoundManager.class");
        assertTrue(manager.methods.stream().anyMatch(candidate -> candidate.name.equals("reload")
            && candidate.desc.equals("()V") && (candidate.access & Opcodes.ACC_PUBLIC) != 0));
    }
}
