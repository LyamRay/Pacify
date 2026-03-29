package me.lyamray.pacify.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.lyamray.pacify.SharedVariables;

@Mixin(SleepingChatScreen.class)
public class SleepingChatScreenMixin extends Screen {
    protected SleepingChatScreenMixin(Text title) { super(title); }

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if (!SharedVariables.enabled) return;
        addDrawableChild(ButtonWidget.builder(Text.of("Client wake up"), b -> {
            if (this.client != null && this.client.player != null) {
                this.client.player.wakeUp();
                this.client.setScreen(null);
            }
        }).width(115).position(5, 5).build());
    }
}