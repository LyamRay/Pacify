package me.lyamray.pacify.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

public class UpdateScreen extends Screen {
    private static final Text LINE1 = Text.of("To update Pacify, quit the game, delete the old jar,");
    private static final Text LINE2 = Text.of("and replace it with the new one downloaded from the website.");

    public UpdateScreen(Text title) { super(title); }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        this.addDrawableChild(new TextWidget(cx - textRenderer.getWidth(LINE1) / 2, 80,
                textRenderer.getWidth(LINE1), 20, LINE1, textRenderer));

        this.addDrawableChild(new TextWidget(cx - textRenderer.getWidth(LINE2) / 2, 95,
                textRenderer.getWidth(LINE2), 20, LINE2, textRenderer));

        if (this.client == null) return;

        this.addDrawableChild(ButtonWidget.builder(Text.of("Quit"), b ->
                this.client.stop()).width(80).position(cx - 85, 145).build());

        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), b ->
                this.client.setScreen(null)).width(80).position(cx + 5, 145).build());
    }
}