package me.lyamray.pacify.widgets.fabricate.slot;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import lombok.Getter;
import lombok.Setter;
import me.lyamray.pacify.SharedVariables;
import me.lyamray.pacify.widgets.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.text.Text;

import java.util.stream.Stream;

@Getter
public class ClickSlotScreen extends Screen {
    private final Screen parent;
    private final MinecraftClient mc;

    @Setter
    @Getter
    private TextFieldWidget syncId;

    @Setter
    @Getter
    private TextFieldWidget revision;

    @Setter
    @Getter
    private TextFieldWidget slot;

    @Setter
    @Getter
    private TextFieldWidget button;

    @Setter
    @Getter
    private TextFieldWidget timesToSend;

    @Setter
    @Getter
    private int actionIndex = 0;

    private static final String[] ACTIONS = {"PICKUP", "QUICK_MOVE", "SWAP", "CLONE", "THROW", "QUICK_CRAFT", "PICKUP_ALL"};

    public ClickSlotScreen(Screen parent, MinecraftClient mc) {
        super(Text.of("Click Slot Packet"));
        this.parent = parent;
        this.mc = mc;
    }

    @Override
    protected void init() {
        int x = width / 2 - 50, y = 40;

        syncId = field(x, y);
        syncId.setPlaceholder(Text.of("Sync Id"));
        syncId.setText(String.valueOf(SharedVariables.latestSyncId));

        revision = field(x, y + 20);
        revision.setPlaceholder(Text.of("Revision"));
        revision.setText(String.valueOf(SharedVariables.latestRevision));

        slot = field(x, y + 40);
        slot.setPlaceholder(Text.of("Slot"));
        button = field(x, y + 60);
        button.setPlaceholder(Text.of("0 or 1"));

        addDrawableChild(ButtonWidget.builder(Text.of("Action: " + ACTIONS[actionIndex]), b -> {
            actionIndex = (actionIndex + 1) % ACTIONS.length;
            b.setMessage(Text.of("Action: " + ACTIONS[actionIndex]));
        }).width(100).position(x, y + 80).build());
        timesToSend = field(x, y + 100);
        timesToSend.setPlaceholder(Text.of("Times"));
        timesToSend.setText("1");
        addDrawableChild(ButtonWidget.builder(Text.of("Send"), b -> trySend()).width(80).position(x, y + 120).build());
        addDrawableChild(ButtonWidget.builder(Text.of("Back"), b -> mc.setScreen(parent)).width(80).position(x, y + 140).build());
    }

    private TextFieldWidget field(int x, int y) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, 100, 16, Text.of(""));
        addDrawableChild(f);
        return f;
    }

    private void trySend() {
        if (!Stream.of(syncId.getText(), revision.getText(), slot.getText(), button.getText(), timesToSend.getText()).allMatch(Utils::isInteger))
            return;
        SlotActionType action = Utils.stringToSlotActionType(ACTIONS[actionIndex]);
        if (action == null) return;

        Utils.sendFabricatedPacket(mc, new ClickSlotC2SPacket(
                Integer.parseInt(syncId.getText()), Integer.parseInt(revision.getText()),
                Short.parseShort(slot.getText()), Byte.parseByte(button.getText()),
                action, new Int2ObjectArrayMap<>(), ItemStackHash.EMPTY
        ), Integer.parseInt(timesToSend.getText()));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int lx = width / 2 - 155, y = 40;
        context.drawText(textRenderer, "Sync Id:", lx, y + 4, 0xFFFFFF, false);
        context.drawText(textRenderer, "Revision:", lx, y + 24, 0xFFFFFF, false);
        context.drawText(textRenderer, "Slot:", lx, y + 44, 0xFFFFFF, false);
        context.drawText(textRenderer, "Button:", lx, y + 64, 0xFFFFFF, false);
        context.drawText(textRenderer, "Action:", lx, y + 84, 0xFFFFFF, false);
        context.drawText(textRenderer, "Times:", lx, y + 104, 0xFFFFFF, false);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);
    }
}
