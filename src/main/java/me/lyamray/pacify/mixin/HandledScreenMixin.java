package me.lyamray.pacify.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import me.lyamray.pacify.ChatFieldFactory;
import me.lyamray.pacify.MainClient;
import me.lyamray.pacify.SharedVariables;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {
    private HandledScreenMixin() { super(null); }

    @Shadow protected abstract boolean handleHotbarKeyPressed(int keyCode, int scanCode);
    @Shadow protected abstract void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType);
    @Shadow @Nullable protected Slot focusedSlot;

    @Unique private static final MinecraftClient mc = MinecraftClient.getInstance();
    @Unique private TextFieldWidget chatField;

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if (!SharedVariables.enabled) return;
        MainClient.createWidgets(mc, this);
        chatField = ChatFieldFactory.create(this.textRenderer);
        this.addDrawableChild(chatField);
    }

    @Inject(at = @At("HEAD"), method = "keyPressed", cancellable = true)
    public void keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        cir.cancel();
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            cir.setReturnValue(true);
            return;
        }
        if (mc.options.inventoryKey.matchesKey(keyCode, scanCode) && (chatField == null || !chatField.isSelected())) {
            this.close();
            cir.setReturnValue(true);
            return;
        }
        handleHotbarKeyPressed(keyCode, scanCode);
        if (focusedSlot != null && focusedSlot.hasStack()) {
            if (mc.options.pickItemKey.matchesKey(keyCode, scanCode))
                onMouseClick(focusedSlot, focusedSlot.id, 0, SlotActionType.CLONE);
            else if (mc.options.dropKey.matchesKey(keyCode, scanCode))
                onMouseClick(focusedSlot, focusedSlot.id, hasControlDown() ? 1 : 0, SlotActionType.THROW);
        }
        cir.setReturnValue(true);
    }

    @Inject(at = @At("TAIL"), method = "render")
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (SharedVariables.enabled) MainClient.createText(mc, context, this.textRenderer);
    }
}