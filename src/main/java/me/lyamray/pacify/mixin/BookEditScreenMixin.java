package me.lyamray.pacify.mixin;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
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

@Slf4j
@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {

    @Unique
    private static final MinecraftClient minecraftClient = MinecraftClient.getInstance();

    @Unique
    private static final String TOGGLE_COMMAND = "^togglepacify";

    protected BookEditScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void pacify$init(CallbackInfo ci) {
        if (!SharedVariables.enabled) return;

        MainClient.getInstance().createWidgets(minecraftClient, this);
        this.addDrawableChild(createChatField());
    }

    @Unique
    private TextFieldWidget createChatField() {
        TextFieldWidget field = new TextFieldWidget(textRenderer, 5, 245, 160, 20, Text.of("Chat ...")) {

            @Override
            public boolean keyPressed(KeyInput input) {
                if (input.getKeycode() == GLFW.GLFW_KEY_ENTER) {
                    handleEnter(this);
                    return true;
                }
                return super.keyPressed(input);
            }
        };

        field.setMaxLength(255);
        field.setText("");
        return field;
    }

    @Unique
    private void handleEnter(TextFieldWidget field) {
        String text = field.getText();

        if (text.equals(TOGGLE_COMMAND)) {
            handleToggle();
            field.setText("");
            return;
        }

        sendChat(text);
        field.setText("");
    }

    @Unique
    private void handleToggle() {
        SharedVariables.enabled = !SharedVariables.enabled;

        if (minecraftClient.player != null) {
            minecraftClient.player.sendMessage(
                    Text.of("Pacify is now " + (SharedVariables.enabled ? "enabled" : "disabled") + "."),
                    false
            );
        }
    }

    @Unique
    private void sendChat(String text) {
        if (minecraftClient.getNetworkHandler() == null) {
            log.warn("Network handler was null while sending chat.");
            return;
        }

        if (text.startsWith("/")) {
            minecraftClient.getNetworkHandler().sendChatCommand(text.replaceFirst(Pattern.quote("/"), ""));
        } else {
            minecraftClient.getNetworkHandler().sendChatMessage(text);
        }
    }
}