package me.lyamray.pacify.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.lyamray.pacify.MainClient;
import me.lyamray.pacify.SharedVariables;

import java.util.regex.Pattern;

@Mixin(BookEditScreen.class)
public class BookEditScreenMixin extends Screen {
    @Unique private static final MinecraftClient mc = MinecraftClient.getInstance();
    @Unique private static final String TOGGLE_COMMAND = "^togglepacify";

    protected BookEditScreenMixin(Text title) { super(title); }

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if (!SharedVariables.enabled) return;
        MainClient.createWidgets(mc, this);
        this.addDrawableChild(createChatField());
    }

    @Unique
    private TextFieldWidget createChatField() {
        TextFieldWidget field = new TextFieldWidget(textRenderer, 5, 245, 160, 20, Text.of("Chat ...")) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == GLFW.GLFW_KEY_ENTER) handleEnter(this);
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        };
        field.setText("");
        field.setMaxLength(255);
        return field;
    }

    @Unique
    private void handleEnter(TextFieldWidget field) {
        if (field.getText().equals(TOGGLE_COMMAND)) { handleToggle(); field.setText(""); return; }
        sendChat(field.getText());
        field.setText("");
    }

    @Unique
    private void handleToggle() {
        SharedVariables.enabled = !SharedVariables.enabled;
        if (mc.player != null)
            mc.player.sendMessage(Text.of("Pacify is now " + (SharedVariables.enabled ? "enabled" : "disabled") + "."), false);
    }

    @Unique
    private void sendChat(String text) {
        if (mc.getNetworkHandler() == null) { MainClient.LOGGER.warn("Network handler was null while sending chat."); return; }
        if (text.startsWith("/")) mc.getNetworkHandler().sendChatCommand(text.replaceFirst(Pattern.quote("/"), ""));
        else mc.getNetworkHandler().sendChatMessage(text);
    }
}