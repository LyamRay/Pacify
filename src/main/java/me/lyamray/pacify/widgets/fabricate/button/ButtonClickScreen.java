package me.lyamray.pacify.widgets.fabricate.button;

import lombok.Getter;
import lombok.Setter;
import me.lyamray.pacify.widgets.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.text.Text;

@Getter
public class ButtonClickScreen extends Screen {
    private final Screen parent;
    private final MinecraftClient mc;

    @Setter
    @Getter
    private TextFieldWidget syncId;

    @Setter
    @Getter
    private TextFieldWidget buttonId;

    @Setter
    @Getter
    private TextFieldWidget timesToSend;

    public ButtonClickScreen(Screen parent, MinecraftClient mc) {
        super(Text.of("Button Click Packet"));
        this.parent = parent;
        this.mc = mc;
    }

    @Override
    protected void init() {
        int x = width / 2 - 50, y = 40;
        syncId = field(x, y);
        syncId.setPlaceholder(Text.of("Sync Id"));
        buttonId = field(x, y + 20);
        buttonId.setPlaceholder(Text.of("Button Id"));
        timesToSend = field(x, y + 40);
        timesToSend.setPlaceholder(Text.of("Times"));
        timesToSend.setText("1");
        addDrawableChild(ButtonWidget.builder(Text.of("Send"), b -> trySend()).width(80).position(x, y + 60).build());
        addDrawableChild(ButtonWidget.builder(Text.of("Back"), b -> mc.setScreen(parent)).width(80).position(x, y + 78).build());
    }

    private TextFieldWidget field(int x, int y) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, 100, 16, Text.of(""));
        addDrawableChild(f);
        return f;
    }

    private void trySend() {
        if (!Utils.isInteger(syncId.getText()) || !Utils.isInteger(buttonId.getText()) || !Utils.isInteger(timesToSend.getText())) return;
        Utils.sendFabricatedPacket(mc, new ButtonClickC2SPacket(
                Integer.parseInt(syncId.getText()), Integer.parseInt(buttonId.getText())
        ), Integer.parseInt(timesToSend.getText()));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int lx = width / 2 - 155, y = 40;
        context.drawText(textRenderer, "Sync Id:", lx, y + 4, 0xFFFFFF, false);
        context.drawText(textRenderer, "Button Id:", lx, y + 24, 0xFFFFFF, false);
        context.drawText(textRenderer, "Times:", lx, y + 44, 0xFFFFFF, false);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);
    }
}
