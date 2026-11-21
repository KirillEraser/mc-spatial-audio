package dev.kirilleraser.mcspatial;

import dev.kirilleraser.mcspatial.client.SpatialSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Locale;

@Mod(value = SpatialAudioMod.MOD_ID, dist = Dist.CLIENT)
public final class SpatialAudioMod {
    public static final String MOD_ID = "mc_spatial_audio";
    public static final Logger LOG = LoggerFactory.getLogger("MC Spatial Audio");

    public SpatialAudioMod(ModContainer container) {
        SpatialSettings.current();
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (IConfigScreenFactory) (owner, parent) -> new SpatialSettingsScreen(parent));
        NeoForge.EVENT_BUS.addListener(SpatialAudioMod::registerCommands);
        LOG.info("Audio bridge loaded. Configure through Mods > MC Spatial Audio > Config.");
    }

    public static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    /** Client-thread only; release the router lock before Minecraft tears down/recreates its library. */
    public static void reloadAudio() {
        AudioRouter.prepareForReload();
        Minecraft.getInstance().getSoundManager().reload();
    }

    private static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mcspatial")
            .then(Commands.literal("status").executes(context -> {
                context.getSource().sendSuccess(() -> Component.literal(AudioRouter.status()), false);
                return 1;
            }))
            .then(Commands.literal("reload").executes(context -> {
                reloadAudio();
                context.getSource().sendSuccess(() -> Component.translatable("mc_spatial_audio.reload.done"), false);
                return 1;
            })));
    }
}
