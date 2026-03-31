package me.lyamray.pacify.mixin;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.lyamray.pacify.MainClient;
import me.lyamray.pacify.SharedVariables;

@Slf4j
@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(at = @At("HEAD"), method = "sendMessage", cancellable = true)
    public void sendMessage(String chatText, boolean addToHistory, CallbackInfo ci) {
        if (!chatText.equals("^togglepacify")) return;
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        SharedVariables.enabled = !SharedVariables.enabled;
        if (minecraftClient.player != null) {
            minecraftClient.player.sendMessage(Text.of("Pacify is now " + (SharedVariables.enabled ? "enabled" : "disabled") + "."), false);
        } else {
            log.warn("Player was null while toggling Pacify.");
        }
        minecraftClient.inGameHud.getChatHud().addToMessageHistory(chatText);
        minecraftClient.setScreen(null);
        ci.cancel();
    }
}
