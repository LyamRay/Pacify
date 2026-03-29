package me.lyamray.pacify;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.regex.Pattern;

public final class ChatFieldFactory {

    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private ChatFieldFactory() {}

    public static TextFieldWidget create(TextRenderer textRenderer) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, 5, 245, 160, 20, Text.of("Chat ...")) {

            @Override
            public boolean keyPressed(KeyInput input) {
                if (input.getKeycode() == GLFW.GLFW_KEY_ENTER) {
                    handleEnter(this.getText());
                    this.setText("");
                    return true;
                }
                return super.keyPressed(input);
            }
        };

        field.setText("");
        field.setMaxLength(256);

        return field;
    }

    private static void handleEnter(String text) {

        if (text.equals("^togglepacify")) {
            SharedVariables.enabled = !SharedVariables.enabled;

            if (MC.player != null) {
                MC.player.sendMessage(
                        Text.of("Pacify is now " + (SharedVariables.enabled ? "enabled" : "disabled") + "."),
                        false
                );
            }
            return;
        }

        if (MC.getNetworkHandler() == null) {
            MainClient.LOGGER.warn("Network handler was null while sending chat message from Pacify.");
            return;
        }

        if (text.startsWith("/")) {
            MC.getNetworkHandler().sendChatCommand(text.replaceFirst(Pattern.quote("/"), ""));
        } else {
            MC.getNetworkHandler().sendChatMessage(text);
        }
    }
}