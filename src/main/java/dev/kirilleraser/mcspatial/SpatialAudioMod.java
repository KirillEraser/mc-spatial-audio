package dev.kirilleraser.mcspatial;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value = SpatialAudioMod.MOD_ID, dist = Dist.CLIENT)
public final class SpatialAudioMod {
    public static final String MOD_ID = "mc_spatial_audio";
    public static final Logger LOG = LoggerFactory.getLogger("MC Spatial Audio");

    public SpatialAudioMod() {
        SpatialSettings.current();
        NeoForge.EVENT_BUS.addListener(SpatialAudioMod::registerCommands);
        LOG.info("Experimental output bridge loaded. No macOS control UI is implemented by this mod.");
    }

    private static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mcspatial")
            .then(Commands.literal("status").executes(context -> {
                context.getSource().sendSuccess(Component.literal(AudioRouter.status()));
                return 1;
            })));
    }
}
