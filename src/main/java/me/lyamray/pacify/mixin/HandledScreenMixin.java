package me.lyamray.pacify.mixin;

import me.lyamray.pacify.ChatFieldFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import me.lyamray.pacify.MainClient;
import me.lyamray.pacify.SharedVariables;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {

    private HandledScreenMixin(TextFieldWidget chatField) {
        super(null);
        this.chatField = chatField;
    }

    @Unique
    private static final MinecraftClient minecraftClient = MinecraftClient.getInstance();

    @Unique
    private TextFieldWidget chatField;

    @Inject(at = @At("TAIL"), method = "init")
    private void onInit(CallbackInfo ci) {
        if (!SharedVariables.enabled) return;

        MainClient.getInstance().createWidgets(minecraftClient, this);

        chatField = ChatFieldFactory.create(this.textRenderer);
        this.addDrawableChild(chatField);
    }

    @Inject(at = @At("HEAD"), method = "keyPressed", cancellable = true)
    private void onKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (!SharedVariables.enabled) return;

        if (chatField != null && chatField.isSelected()) {
            if (chatField.keyPressed(input)) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

    @Inject(at = @At("TAIL"), method = "render")
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (SharedVariables.enabled) {
            MainClient.getInstance().createText(minecraftClient, context, this.textRenderer);
            MainClient.getInstance().renderInfo(minecraftClient, context, this.textRenderer);
        }
    }
}