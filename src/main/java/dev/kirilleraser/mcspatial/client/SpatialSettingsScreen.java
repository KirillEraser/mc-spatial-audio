package dev.kirilleraser.mcspatial.client;

import dev.kirilleraser.mcspatial.AudioRouter;
import dev.kirilleraser.mcspatial.SpatialAudioMod;
import dev.kirilleraser.mcspatial.SpatialSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.io.IOException;

/** Small, dependency-free settings screen; changes are staged until Apply is pressed. */
public final class SpatialSettingsScreen extends Screen {
    private final Screen parent;
    private boolean enabled;
    private boolean tracking;
    private Button enabledButton;
    private Button trackingButton;
    private Component feedback = Component.empty();
    private boolean feedbackError;

    public SpatialSettingsScreen(Screen parent) {
        super(Component.translatable("mc_spatial_audio.config.title"));
        this.parent = parent;
        SpatialSettings settings = SpatialSettings.current();
        enabled = settings.enabled();
        tracking = settings.headTrackingRequested();
    }

    private int rowWidth() { return Math.min(310, width - 24); }
    private int left() { return (width - rowWidth()) / 2; }
    private int top() { return Math.max(42, height / 2 - 80); }

    @Override
    protected void init() {
        int row = rowWidth();
        int half = (row - 8) / 2;
        enabledButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            enabled = !enabled;
            updateButtons();
        }).bounds(left(), top(), row, 20).build());
        trackingButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            tracking = !tracking;
            updateButtons();
        }).bounds(left(), top() + 26, row, 20).build());
        enabledButton.setTooltip(Tooltip.create(Component.translatable(SpatialSettings.enabledOverridden()
            ? "mc_spatial_audio.config.override" : "mc_spatial_audio.config.enabled.tooltip")));
        trackingButton.setTooltip(Tooltip.create(Component.translatable("mc_spatial_audio.config.tracking.tooltip")));

        addRenderableWidget(Button.builder(Component.translatable("mc_spatial_audio.config.copy"), button -> {
            minecraft.keyboardHandler.setClipboard(AudioRouter.status());
            feedback = Component.translatable("mc_spatial_audio.config.copied");
            feedbackError = false;
        }).bounds(left(), height - 54, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("mc_spatial_audio.config.defaults"), button -> {
            enabled = SpatialSettings.enabledOverridden() ? SpatialSettings.current().enabled() : SpatialSettings.DEFAULTS.enabled();
            tracking = SpatialSettings.DEFAULTS.headTrackingRequested();
            feedback = Component.translatable("mc_spatial_audio.config.staged");
            feedbackError = false;
            updateButtons();
        }).bounds(left() + half + 8, height - 54, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("mc_spatial_audio.config.apply"), button -> apply())
            .bounds(left(), height - 28, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
            .bounds(left() + half + 8, height - 28, half, 20).build());
        updateButtons();
    }

    private void updateButtons() {
        enabledButton.setMessage(Component.translatable("mc_spatial_audio.config.enabled", onOff(enabled)));
        trackingButton.setMessage(Component.translatable("mc_spatial_audio.config.tracking", onOff(tracking)));
        enabledButton.active = SpatialAudioMod.isMac() && !SpatialSettings.enabledOverridden();
        trackingButton.active = SpatialAudioMod.isMac() && enabled;
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value ? "options.on" : "options.off");
    }

    private void apply() {
        try {
            SpatialSettings.save(new SpatialSettings(enabled, tracking));
            SpatialAudioMod.reloadAudio();
            feedback = Component.translatable("mc_spatial_audio.reload.done");
            feedbackError = false;
        } catch (IOException | RuntimeException | LinkageError failure) {
            SpatialAudioMod.LOG.error("Could not apply audio settings", failure);
            feedback = Component.translatable("mc_spatial_audio.config.failed");
            feedbackError = true;
        }
    }

    private int wrapped(GuiGraphics graphics, Component text, int y, int color, int maxLines) {
        int count = 0;
        for (FormattedCharSequence line : font.split(text, rowWidth())) {
            if (count++ >= maxLines) break;
            graphics.drawString(font, line, left(), y, color, false);
            y += font.lineHeight + 1;
        }
        return y;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFFFF);
        int y = wrapped(graphics, Component.translatable(SpatialAudioMod.isMac()
            ? "mc_spatial_audio.config.apple" : "mc_spatial_audio.config.macos"), top() + 56, 0xFFB0B0B0, 3);
        y = wrapped(graphics, Component.translatable("mc_spatial_audio.config.status", AudioRouter.summary()), y + 6, 0xFFE0E0E0, 2);
        if (y + font.lineHeight < height - 58) {
            wrapped(graphics, feedback, y + 4, feedbackError ? 0xFFFF7070 : 0xFF80D080, 2);
        }
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }
}
