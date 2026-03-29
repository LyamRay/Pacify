package me.lyamray.pacify.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.lyamray.pacify.MainClient;
import me.lyamray.pacify.SharedVariables;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(at = @At("HEAD"), method = "sendMessage", cancellable = true)
    public void sendMessage(String chatText, boolean addToHistory, CallbackInfo ci) {
        if (!chatText.equals("^togglepacify")) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        SharedVariables.enabled = !SharedVariables.enabled;
        if (mc.player != null)
            mc.player.sendMessage(Text.of("Pacify is now " + (SharedVariables.enabled ? "enabled" : "disabled") + "."), false);
        else
            MainClient.LOGGER.warn("Player was null while toggling Pacify.");
        mc.inGameHud.getChatHud().addToMessageHistory(chatText);
        mc.setScreen(null);
        ci.cancel();
    }
}