package me.lyamray.pacify;

import com.google.gson.Gson;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import me.lyamray.pacify.mixin.accessor.ClientConnectionAccessor;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MainClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Pacify");

    public static KeyBinding restoreScreenKey;
    private static final KeyBinding.Category PACIFY_CATEGORY =
            KeyBinding.Category.create(Identifier.of("key.categories.pacify"));

    public static final Map<String, Object[]> savedGuis = new LinkedHashMap<>();

    private static final int BTN_W = 110;
    private static final int BTN_X = 5;
    private static final int BTN_Y0 = 5;
    private static final int BTN_GAP = 22;

    @Override
    public void onInitializeClient() {
        restoreScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pacify.restore_screen", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, PACIFY_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (restoreScreenKey.wasPressed()) {
                if (SharedVariables.storedScreen != null &&
                        SharedVariables.storedScreenHandler != null &&
                        client.player != null) {
                    client.setScreen(SharedVariables.storedScreen);
                    client.player.currentScreenHandler = SharedVariables.storedScreenHandler;
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long now = System.currentTimeMillis();
            long diff = now - SharedVariables.lastTimeUpdate;

            if (diff > 0) {
                double tps = 1000.0 / diff;
                SharedVariables.estimatedTPS = Math.min(20.0, tps);
            }

            SharedVariables.lastTimeUpdate = now;
        });
    }

    @SuppressWarnings("all")
    public static void createText(MinecraftClient mc, DrawContext context, TextRenderer textRenderer) {
        context.drawText(textRenderer, "Sync Id: " + mc.player.currentScreenHandler.syncId, 200, 5, Color.WHITE.getRGB(), false);
        context.drawText(textRenderer, "Revision: " + mc.player.currentScreenHandler.getRevision(), 200, 35, Color.WHITE.getRGB(), false);
    }

    public static void createWidgets(MinecraftClient mc, Screen screen) {
        int[] y = {BTN_Y0};
        btn(screen, y, "Close w/o packet", b -> closeWithoutPacket(mc));
        btn(screen, y, "De-sync", b -> desync(mc));
        btn(screen, y, "Send packets: " + SharedVariables.sendUIPackets, MainClient::toggleSendPackets);
        btn(screen, y, "Delay packets: " + SharedVariables.delayUIPackets, b -> toggleDelayPackets(mc, b));
        btn(screen, y, "Save GUI", b -> mc.setScreen(new SaveGuiScreen(mc.currentScreen, mc)));
        btn(screen, y, "Load GUI", b -> mc.setScreen(new LoadGuiScreen(mc.currentScreen, mc)));
        btn(screen, y, "Disconnect & send packet", b -> disconnectAndSend(mc));
        btn(screen, y, "Fabricate packet", b -> mc.setScreen(new FabricatePacketScreen(mc.currentScreen, mc)));
        btn(screen, y, "Copy Title JSON", b -> copyTitleJson(mc));
    }

    public static void renderServerInfo(MinecraftClient mc, DrawContext context, TextRenderer textRenderer) {
        if (mc.player == null || mc.currentScreen == null || mc.getNetworkHandler() == null) return;

        int x = mc.currentScreen.width - 150;
        int y = 5;

        String address = mc.getCurrentServerEntry() != null
                ? mc.getCurrentServerEntry().address
                : "Singleplayer";

        String brand = mc.getNetworkHandler().getBrand();

        int players = mc.getNetworkHandler().getPlayerList().size();

        int ping = 0;
        if (mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null) {
            ping = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency();
        }

        double tps = SharedVariables.estimatedTPS;

        context.drawText(textRenderer, "Server: " + address, x, y, Color.WHITE.getRGB(), false);
        context.drawText(textRenderer, "Software: " + brand, x, y + 10, Color.WHITE.getRGB(), false);
        context.drawText(textRenderer, "Players: " + players, x, y + 20, Color.WHITE.getRGB(), false);
        context.drawText(textRenderer, "Ping: " + ping + "ms", x, y + 30, Color.WHITE.getRGB(), false);
        context.drawText(textRenderer, "TPS: " + String.format("%.2f", tps), x, y + 40, Color.WHITE.getRGB(), false);
    }

    private static void closeWithoutPacket(MinecraftClient mc) {
        if (mc.player == null) return;
        SharedVariables.storedScreen = mc.currentScreen;
        SharedVariables.storedScreenHandler = mc.player.currentScreenHandler;
        mc.setScreen(null);
    }

    private static void desync(MinecraftClient mc) {
        if (mc.getNetworkHandler() == null || mc.player == null) {
            LOGGER.warn("Network handler or player was null while using 'De-sync'.");
            return;
        }
        mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
    }

    private static void toggleSendPackets(ButtonWidget b) {
        SharedVariables.sendUIPackets = !SharedVariables.sendUIPackets;
        b.setMessage(Text.of("Send packets: " + SharedVariables.sendUIPackets));
    }

    private static void toggleDelayPackets(MinecraftClient mc, ButtonWidget b) {
        SharedVariables.delayUIPackets = !SharedVariables.delayUIPackets;
        b.setMessage(Text.of("Delay packets: " + SharedVariables.delayUIPackets));
        if (!SharedVariables.delayUIPackets) flushDelayedPackets(mc);
    }

    private static void flushDelayedPackets(MinecraftClient mc) {
        if (SharedVariables.delayedUIPackets.isEmpty() || mc.getNetworkHandler() == null) return;
        SharedVariables.delayedUIPackets.forEach(p -> mc.getNetworkHandler().sendPacket(p));
        if (mc.player != null)
            mc.player.sendMessage(Text.of("Sent " + SharedVariables.delayedUIPackets.size() + " packets."), false);
        SharedVariables.delayedUIPackets.clear();
    }

    private static void disconnectAndSend(MinecraftClient mc) {
        SharedVariables.delayUIPackets = false;
        if (mc.getNetworkHandler() == null) {
            LOGGER.warn("Network handler is null while disconnecting.");
            return;
        }
        SharedVariables.delayedUIPackets.forEach(p -> mc.getNetworkHandler().sendPacket(p));
        mc.getNetworkHandler().getConnection().disconnect(Text.of("Disconnecting (Pacify)"));
        SharedVariables.delayedUIPackets.clear();
    }

    private static void copyTitleJson(MinecraftClient mc) {
        if (mc.currentScreen == null) {
            LOGGER.error("Error copying title JSON", new IllegalStateException("mc.currentScreen is null"));
            return;
        }
        mc.keyboard.setClipboard(new Gson().toJson(
                TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, mc.currentScreen.getTitle()).getOrThrow()));
    }

    private static void btn(Screen screen, int[] y, String label, ButtonWidget.PressAction action) {
        screen.addDrawableChild(ButtonWidget.builder(Text.of(label), action)
                .width(BTN_W).position(BTN_X, y[0]).build());
        y[0] += BTN_GAP;
    }

    public static class SaveGuiScreen extends Screen {
        private final Screen parent;
        private final MinecraftClient mc;
        private TextFieldWidget nameField;
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
                if (name.isEmpty()) { feedback = "Name cannot be empty!"; return; }
                if (mc.player == null) { feedback = "Player is null!"; return; }
                savedGuis.put(name, new Object[]{parent, mc.player.currentScreenHandler});
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

    public static class LoadGuiScreen extends Screen {
        private final Screen parent;
        private final MinecraftClient mc;
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
            String[] names = savedGuis.keySet().toArray(new String[0]);
            int cx = width / 2;
            int visible = Math.min(VISIBLE, names.length - scrollOffset);
            IntStream.range(0, visible).forEach(i -> {
                String name = names[scrollOffset + i];
                int ry = LIST_TOP + i * ROW_H;
                addDrawableChild(ButtonWidget.builder(Text.of(name), b -> {
                    Object[] saved = savedGuis.get(name);
                    if (saved == null) return;
                    Screen s = (Screen) saved[0];
                    ScreenHandler h = (ScreenHandler) saved[1];
                    SharedVariables.storedScreen = s;
                    SharedVariables.storedScreenHandler = h;
                    mc.setScreen(s);
                    if (mc.player != null) mc.player.currentScreenHandler = h;
                }).width(160).position(cx - 100, ry).build());
                addDrawableChild(ButtonWidget.builder(Text.of("X"), b -> {
                    savedGuis.remove(name);
                    scrollOffset = Math.clamp(scrollOffset, 0, savedGuis.size() - 1);
                    rebuildButtons();
                }).width(20).position(cx + 64, ry).build());
            });
            if (scrollOffset > 0)
                addDrawableChild(ButtonWidget.builder(Text.of("^"), b -> { scrollOffset--; rebuildButtons(); })
                        .width(20).position(cx + 90, LIST_TOP).build());
            if (scrollOffset + VISIBLE < names.length)
                addDrawableChild(ButtonWidget.builder(Text.of("v"), b -> { scrollOffset++; rebuildButtons(); })
                        .width(20).position(cx + 90, LIST_TOP + (VISIBLE - 1) * ROW_H).build());
            addDrawableChild(ButtonWidget.builder(Text.of("Back"), b -> mc.setScreen(parent))
                    .width(80).position(width / 2 - 40, height - 28).build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);
            if (savedGuis.isEmpty())
                context.drawCenteredTextWithShadow(textRenderer, "No saved GUIs.", width / 2, LIST_TOP + 10, 0xAAAAAA);
        }
    }

    private static void sendFabricatedPacket(MinecraftClient mc, Packet<?> packet, int times) {
        try {
            Runnable action = () -> {
                if (mc.getNetworkHandler() != null) {
                    mc.getNetworkHandler().sendPacket(packet);
                    ((ClientConnectionAccessor) mc.getNetworkHandler().getConnection()).getChannel().writeAndFlush(packet);
                }
            };
            IntStream.range(0, times).forEach(i -> action.run());
        } catch (Exception e) {
            LOGGER.error("Error sending fabricated packet", e);
        }
    }

    public static boolean isInteger(String s) {
        try { Integer.parseInt(s); return true; } catch (Exception e) { return false; }
    }

    public static SlotActionType stringToSlotActionType(String s) {
        return switch (s) {
            case "PICKUP" -> SlotActionType.PICKUP;
            case "QUICK_MOVE" -> SlotActionType.QUICK_MOVE;
            case "SWAP" -> SlotActionType.SWAP;
            case "CLONE" -> SlotActionType.CLONE;
            case "THROW" -> SlotActionType.THROW;
            case "QUICK_CRAFT" -> SlotActionType.QUICK_CRAFT;
            case "PICKUP_ALL" -> SlotActionType.PICKUP_ALL;
            default -> null;
        };
    }

    public static class FabricatePacketScreen extends Screen {
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

    public static class ClickSlotScreen extends Screen {
        private final Screen parent;
        private final MinecraftClient mc;
        private TextFieldWidget syncId, revision, slot, button, timesToSend;
        private int actionIndex = 0;
        private static final String[] ACTIONS = {"PICKUP","QUICK_MOVE","SWAP","CLONE","THROW","QUICK_CRAFT","PICKUP_ALL"};

        public ClickSlotScreen(Screen parent, MinecraftClient mc) {
            super(Text.of("Click Slot Packet"));
            this.parent = parent;
            this.mc = mc;
        }

        @Override
        protected void init() {
            int x = width / 2 - 50, y = 40;
            syncId = field(x, y); syncId.setPlaceholder(Text.of("Sync Id"));
            revision = field(x, y + 20); revision.setPlaceholder(Text.of("Revision"));
            slot = field(x, y + 40); slot.setPlaceholder(Text.of("Slot"));
            button = field(x, y + 60); button.setPlaceholder(Text.of("0 or 1"));
            addDrawableChild(ButtonWidget.builder(Text.of("Action: " + ACTIONS[actionIndex]), b -> {
                actionIndex = (actionIndex + 1) % ACTIONS.length;
                b.setMessage(Text.of("Action: " + ACTIONS[actionIndex]));
            }).width(100).position(x, y + 80).build());
            timesToSend = field(x, y + 100); timesToSend.setPlaceholder(Text.of("Times"));
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
            if (!Stream.of(syncId.getText(), revision.getText(), slot.getText(), button.getText(), timesToSend.getText()).allMatch(MainClient::isInteger)) return;
            SlotActionType action = stringToSlotActionType(ACTIONS[actionIndex]);
            if (action == null) return;
            sendFabricatedPacket(mc, new ClickSlotC2SPacket(
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

    public static class ButtonClickScreen extends Screen {
        private final Screen parent;
        private final MinecraftClient mc;
        private TextFieldWidget syncId, buttonId, timesToSend;

        public ButtonClickScreen(Screen parent, MinecraftClient mc) {
            super(Text.of("Button Click Packet"));
            this.parent = parent;
            this.mc = mc;
        }

        @Override
        protected void init() {
            int x = width / 2 - 50, y = 40;
            syncId = field(x, y); syncId.setPlaceholder(Text.of("Sync Id"));
            buttonId = field(x, y + 20); buttonId.setPlaceholder(Text.of("Button Id"));
            timesToSend = field(x, y + 40); timesToSend.setPlaceholder(Text.of("Times"));
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
            if (!isInteger(syncId.getText()) || !isInteger(buttonId.getText()) || !isInteger(timesToSend.getText())) return;
            sendFabricatedPacket(mc, new ButtonClickC2SPacket(
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
}