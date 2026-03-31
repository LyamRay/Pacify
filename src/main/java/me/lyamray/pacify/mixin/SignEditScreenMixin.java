package me.lyamray.pacify.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.lyamray.pacify.SharedVariables;

@Mixin(SignEditScreen.class)
public class SignEditScreenMixin extends Screen {
    protected SignEditScreenMixin(Text title) { super(title); }

    @Unique
    private static final MinecraftClient minecraftClient = MinecraftClient.getInstance();

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if (!SharedVariables.enabled) return;
        addDrawableChild(ButtonWidget.builder(Text.of("Close without packet"), b -> {
            SharedVariables.shouldEditSign = false;
            minecraftClient.setScreen(null);
        }).width(115).position(5, 5).build());

        addDrawableChild(ButtonWidget.builder(Text.of("Disconnect"), b -> {
            if (minecraftClient.getNetworkHandler() != null)
                minecraftClient.getNetworkHandler().getConnection().disconnect(Text.of("Disconnecting (Pacify by Hexa Studios)"));
        }).width(115).position(5, 35).build());
    }
}