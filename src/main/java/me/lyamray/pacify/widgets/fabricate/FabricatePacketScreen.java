package me.lyamray.pacify.widgets.fabricate;

import lombok.Getter;
import me.lyamray.pacify.widgets.fabricate.button.ButtonClickScreen;
import me.lyamray.pacify.widgets.fabricate.slot.ClickSlotScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

@Getter
public class FabricatePacketScreen extends Screen {
    private final Screen parent;
    private final MinecraftClient mc;

    public FabricatePacketScreen(Screen parent, MinecraftClient mc) {
        super(Text.of("Fabricate Packet"));
        this.parent = parent;
        this.mc = mc;
    }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;
        addDrawableChild(ButtonWidget.builder(Text.of("Click Slot"), b -> mc.setScreen(new ClickSlotScreen(parent, mc))).width(110).position(cx - 120, cy - 10).build());
        addDrawableChild(ButtonWidget.builder(Text.of("Button Click"), b -> mc.setScreen(new ButtonClickScreen(parent, mc))).width(110).position(cx + 10, cy - 10).build());
        addDrawableChild(ButtonWidget.builder(Text.of("Back"), b -> mc.setScreen(parent)).width(80).position(cx - 40, cy + 12).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 30, 0xFFFFFF);
    }
}
