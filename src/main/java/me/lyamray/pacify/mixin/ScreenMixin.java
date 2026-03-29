package me.lyamray.pacify.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.LecternScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.lyamray.pacify.ChatFieldFactory;
import me.lyamray.pacify.MainClient;
import me.lyamray.pacify.SharedVariables;
import me.lyamray.pacify.mixin.accessor.ScreenAccessor;

@SuppressWarnings("all")
@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow
    public abstract <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement);

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    @Inject(at = @At("TAIL"), method = "init(Lnet/minecraft/client/MinecraftClient;II)V")
    public void init(MinecraftClient client, int width, int height, CallbackInfo ci) {
        if (!(mc.currentScreen instanceof LecternScreen screen) || !SharedVariables.enabled) return;
        MainClient.createWidgets(mc, screen);
        this.addDrawableChild(ChatFieldFactory.create(((ScreenAccessor) this).getTextRenderer()));
    }

    @Inject(at = @At("TAIL"), method = "render")
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (SharedVariables.enabled && mc.player != null && mc.currentScreen instanceof LecternScreen)
            MainClient.createText(mc, context, ((ScreenAccessor) this).getTextRenderer());
    }
}