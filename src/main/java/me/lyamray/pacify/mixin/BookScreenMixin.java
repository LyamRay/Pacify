package me.lyamray.pacify.mixin;

import me.lyamray.pacify.ChatFieldFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.lyamray.pacify.MainClient;
import me.lyamray.pacify.SharedVariables;

@Mixin(BookScreen.class)
public class BookScreenMixin extends Screen {
    protected BookScreenMixin(Text title) { super(title); }

    @Unique private static final MinecraftClient minecraftClient = MinecraftClient.getInstance();

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if (!SharedVariables.enabled) return;

        MainClient.getInstance().createWidgets(minecraftClient, this);

        this.addDrawableChild(ChatFieldFactory.create(this.textRenderer));
    }
}