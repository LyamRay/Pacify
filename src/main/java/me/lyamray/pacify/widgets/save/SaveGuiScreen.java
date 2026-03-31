package me.lyamray.pacify.widgets.save;

import lombok.Getter;
import lombok.Setter;
import me.lyamray.pacify.SharedVariables;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

@Getter
public class SaveGuiScreen extends Screen {
    private final Screen parent;
    private final MinecraftClient mc;

    @Setter
    @Getter
    private TextFieldWidget nameField;

    @Setter
    @Getter
    private String feedback = "";

    public SaveGuiScreen(Screen parent, MinecraftClient mc) {
        super(Text.of("Save GUI"));
        this.parent = parent;
        this.mc = mc;
    }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;
        nameField = new TextFieldWidget(textRenderer, cx - 75, cy - 30, 150, 18, Text.of(""));
        nameField.setPlaceholder(Text.of("Enter a name..."));
        addDrawableChild(nameField);
        addDrawableChild(ButtonWidget.builder(Text.of("Save current GUI"), b -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                feedback = "Name cannot be empty!";
                return;
            }
            if (mc.player == null) {
                feedback = "Player is null!";
                return;
            }
            SharedVariables.savedGuis.put(name, new Object[]{parent, mc.player.currentScreenHandler});
            SharedVariables.storedScreen = parent;
            SharedVariables.storedScreenHandler = mc.player.currentScreenHandler;
            feedback = "Saved as: " + name;
            mc.setScreen(parent);
        }).width(140).position(cx - 70, cy - 8).build());
        addDrawableChild(ButtonWidget.builder(Text.of("Back"), b ->
                mc.setScreen(parent)
        ).width(80).position(cx - 40, cy + 15).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 50, 0xFFFFFF);
        if (!feedback.isEmpty())
            context.drawCenteredTextWithShadow(textRenderer, feedback, width / 2, height / 2 + 35, 0xAAFFAA);
    }
}
