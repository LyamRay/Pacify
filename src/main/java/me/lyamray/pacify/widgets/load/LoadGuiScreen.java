package me.lyamray.pacify.widgets.load;

import lombok.Getter;
import lombok.Setter;
import me.lyamray.pacify.SharedVariables;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

import java.util.stream.IntStream;

@Getter
public class LoadGuiScreen extends Screen {
    private final Screen parent;
    private final MinecraftClient mc;

    @Setter
    @Getter
    private int scrollOffset = 0;
    private static final int ROW_H = 20;
    private static final int LIST_TOP = 40;
    private static final int VISIBLE = 8;

    public LoadGuiScreen(Screen parent, MinecraftClient mc) {
        super(Text.of("Load GUI"));
        this.parent = parent;
        this.mc = mc;
    }

    @Override
    protected void init() {
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearChildren();
        String[] names = SharedVariables.savedGuis.keySet().toArray(new String[0]);
        int cx = width / 2;
        IntStream.range(0, Math.min(VISIBLE, names.length - scrollOffset)).forEach(i -> {
            String name = names[scrollOffset + i];
            int ry = LIST_TOP + i * ROW_H;
            addDrawableChild(ButtonWidget.builder(Text.of(name), b -> loadGui(name)).width(160).position(cx - 100, ry).build());
            addDrawableChild(ButtonWidget.builder(Text.of("X"), b -> removeGui(name)).width(20).position(cx + 64, ry).build());
        });
        if (scrollOffset > 0)
            addDrawableChild(ButtonWidget.builder(Text.of("^"), b -> {
                scrollOffset--;
                rebuildButtons();
            }).width(20).position(cx + 90, LIST_TOP).build());
        if (scrollOffset + VISIBLE < names.length)
            addDrawableChild(ButtonWidget.builder(Text.of("v"), b -> {
                scrollOffset++;
                rebuildButtons();
            }).width(20).position(cx + 90, LIST_TOP + (VISIBLE - 1) * ROW_H).build());
        addDrawableChild(ButtonWidget.builder(Text.of("Back"), b -> mc.setScreen(parent)).width(80).position(width / 2 - 40, height - 28).build());
    }

    private void loadGui(String name) {
        Object[] saved = SharedVariables.savedGuis.get(name);
        if (saved == null) return;
        Screen s = (Screen) saved[0];
        ScreenHandler h = (ScreenHandler) saved[1];
        SharedVariables.storedScreen = s;
        SharedVariables.storedScreenHandler = h;
        mc.setScreen(s);
        if (mc.player != null) mc.player.currentScreenHandler = h;
    }

    private void removeGui(String name) {
        SharedVariables.savedGuis.remove(name);
        scrollOffset = Math.clamp(scrollOffset, 0, SharedVariables.savedGuis.size() - 1);
        rebuildButtons();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);
        if (SharedVariables.savedGuis.isEmpty())
            context.drawCenteredTextWithShadow(textRenderer, "No saved GUIs.", width / 2, LIST_TOP + 10, 0xAAAAAA);
    }
}
