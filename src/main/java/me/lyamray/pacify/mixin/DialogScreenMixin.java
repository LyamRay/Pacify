package me.lyamray.pacify.mixin;

import me.lyamray.pacify.ChatFieldFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.dialog.DialogScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.lyamray.pacify.MainClient;
import me.lyamray.pacify.SharedVariables;

@Mixin(DialogScreen.class)
public class DialogScreenMixin extends Screen {
    protected DialogScreenMixin(Text title) { super(title); }

    @Unique private static final MinecraftClient mc = MinecraftClient.getInstance();

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if (!SharedVariables.enabled) return;
        MainClient.createWidgets(mc, this);
        this.addDrawableChild(ChatFieldFactory.create(this.textRenderer));
    }
}