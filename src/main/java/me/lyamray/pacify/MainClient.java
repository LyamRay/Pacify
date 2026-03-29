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
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MainClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Pacify");

    public static KeyBinding restoreScreenKey;
    private static final KeyBinding.Category PACIFY_CATEGORY =
            KeyBinding.Category.create(Identifier.of("key.categories.pacify"));
    @Override
    public void onInitializeClient() {
        restoreScreenKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.pacify.restore_screen",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_V,
                        PACIFY_CATEGORY
                )
        );

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
    }

    @SuppressWarnings("all")
    public static void createText(MinecraftClient mc, DrawContext context, TextRenderer textRenderer) {
        context.drawText(textRenderer, "Sync Id: " + mc.player.currentScreenHandler.syncId, 200, 5, Color.WHITE.getRGB(), false);
        context.drawText(textRenderer, "Revision: " + mc.player.currentScreenHandler.getRevision(), 200, 35, Color.WHITE.getRGB(), false);
    }

    public static void createWidgets(MinecraftClient mc, Screen screen) {
        addCloseWithoutPacketButton(mc, screen);
        addDesyncButton(mc, screen);
        addSendPacketsButton(screen);
        addDelayPacketsButton(mc, screen);
        addSaveGuiButton(mc, screen);
        addDisconnectAndSendButton(mc, screen);
        addFabricatePacketButton(mc, screen);
        addCopyTitleJsonButton(mc, screen);
    }

    private static void addCloseWithoutPacketButton(MinecraftClient mc, Screen screen) {
        screen.addDrawableChild(ButtonWidget.builder(Text.of("Close without packet"), b -> {
            if (mc.player != null) {
                SharedVariables.storedScreen = mc.currentScreen;
                SharedVariables.storedScreenHandler = mc.player.currentScreenHandler;
            }
            mc.setScreen(null);
        }).width(115).position(5, 5).build());
    }

    private static void addDesyncButton(MinecraftClient mc, Screen screen) {
        screen.addDrawableChild(ButtonWidget.builder(Text.of("De-sync"), b -> {
            if (mc.getNetworkHandler() != null && mc.player != null)
                mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
            else
                LOGGER.warn("Network handler or player was null while using 'De-sync'.");
        }).width(115).position(5, 35).build());
    }

    private static void addSendPacketsButton(Screen screen) {
        screen.addDrawableChild(ButtonWidget.builder(Text.of("Send packets: " + SharedVariables.sendUIPackets), b -> {
            SharedVariables.sendUIPackets = !SharedVariables.sendUIPackets;
            b.setMessage(Text.of("Send packets: " + SharedVariables.sendUIPackets));
        }).width(115).position(5, 65).build());
    }

    private static void addDelayPacketsButton(MinecraftClient mc, Screen screen) {
        screen.addDrawableChild(ButtonWidget.builder(Text.of("Delay packets: " + SharedVariables.delayUIPackets), b -> {
            SharedVariables.delayUIPackets = !SharedVariables.delayUIPackets;
            b.setMessage(Text.of("Delay packets: " + SharedVariables.delayUIPackets));
            if (!SharedVariables.delayUIPackets && !SharedVariables.delayedUIPackets.isEmpty() && mc.getNetworkHandler() != null) {
                SharedVariables.delayedUIPackets.forEach(p -> mc.getNetworkHandler().sendPacket(p));
                if (mc.player != null)
                    mc.player.sendMessage(Text.of("Sent " + SharedVariables.delayedUIPackets.size() + " packets."), false);
                SharedVariables.delayedUIPackets.clear();
            }
        }).width(115).position(5, 95).build());
    }

    private static void addSaveGuiButton(MinecraftClient mc, Screen screen) {
        screen.addDrawableChild(ButtonWidget.builder(Text.of("Save GUI"), b -> {
            if (mc.player != null) {
                SharedVariables.storedScreen = mc.currentScreen;
                SharedVariables.storedScreenHandler = mc.player.currentScreenHandler;
            }
        }).width(115).position(5, 125).build());
    }

    private static void addDisconnectAndSendButton(MinecraftClient mc, Screen screen) {
        screen.addDrawableChild(ButtonWidget.builder(Text.of("Disconnect and send packets"), b -> {
            SharedVariables.delayUIPackets = false;
            if (mc.getNetworkHandler() != null) {
                SharedVariables.delayedUIPackets.forEach(p -> mc.getNetworkHandler().sendPacket(p));
                mc.getNetworkHandler().getConnection().disconnect(Text.of("Disconnecting (Pacify)"));
            } else {
                LOGGER.warn("Network handler is null while disconnecting.");
            }
            SharedVariables.delayedUIPackets.clear();
        }).width(160).position(5, 155).build());
    }

    private static void addFabricatePacketButton(MinecraftClient mc, Screen screen) {
        screen.addDrawableChild(ButtonWidget.builder(Text.of("Fabricate packet"), b ->
                mc.setScreen(new FabricatePacketScreen(mc.currentScreen, mc))
        ).width(115).position(5, 185).build());
    }

    private static void addCopyTitleJsonButton(MinecraftClient mc, Screen screen) {
        screen.addDrawableChild(ButtonWidget.builder(Text.of("Copy GUI Title JSON"), b -> {
            try {
                if (mc.currentScreen == null) throw new IllegalStateException("mc.currentScreen is null");
                mc.keyboard.setClipboard(new Gson().toJson(TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, mc.currentScreen.getTitle()).getOrThrow()));
            } catch (IllegalStateException e) {
                LOGGER.error("Error copying title JSON", e);
            }
        }).width(115).position(5, 215).build());
    }

    private static void sendFabricatedPacket(MinecraftClient mc, Packet<?> packet, boolean delay, int times) {
        try {
            Runnable action = delay
                    ? () -> { if (mc.getNetworkHandler() != null) mc.getNetworkHandler().sendPacket(packet); }
                    : () -> {
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


    // --- Fabricate Packet Screen ---

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
            addDrawableChild(ButtonWidget.builder(Text.of("Click Slot"), b ->
                    mc.setScreen(new ClickSlotScreen(parent, mc))
            ).width(110).position(width / 2 - 120, height / 2 - 10).build());
            addDrawableChild(ButtonWidget.builder(Text.of("Button Click"), b ->
                    mc.setScreen(new ButtonClickScreen(parent, mc))
            ).width(110).position(width / 2 + 10, height / 2 - 10).build());
            addDrawableChild(ButtonWidget.builder(Text.of("Back"), b ->
                    mc.setScreen(parent)
            ).width(80).position(width / 2 - 40, height / 2 + 20).build());
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
            syncId = addField(x, y); revision = addField(x, y + 20);
            slot = addField(x, y + 40); button = addField(x, y + 60);
            timesToSend = addField(x, y + 100); timesToSend.setText("1");
            addDrawableChild(ButtonWidget.builder(Text.of("Action: " + ACTIONS[actionIndex]), b -> {
                actionIndex = (actionIndex + 1) % ACTIONS.length;
                b.setMessage(Text.of("Action: " + ACTIONS[actionIndex]));
            }).width(100).position(x, y + 80).build());
            addDrawableChild(ButtonWidget.builder(Text.of("Send"), b -> trySend()).width(80).position(x, y + 120).build());
            addDrawableChild(ButtonWidget.builder(Text.of("Back"), b -> mc.setScreen(parent)).width(80).position(x, y + 145).build());
        }

        private TextFieldWidget addField(int x, int y) {
            TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, 100, 18, Text.of(""));
            addDrawableChild(f);
            return f;
        }

        private void trySend() {
            if (!Stream.of(syncId.getText(), revision.getText(), slot.getText(), button.getText(), timesToSend.getText()).allMatch(MainClient::isInteger)) return;
            SlotActionType action = stringToSlotActionType(ACTIONS[actionIndex]);
            if (action == null) return;
            ClickSlotC2SPacket packet = new ClickSlotC2SPacket(
                    Integer.parseInt(syncId.getText()), Integer.parseInt(revision.getText()),
                    Short.parseShort(slot.getText()), Byte.parseByte(button.getText()),
                    action, new Int2ObjectArrayMap<>(), ItemStackHash.EMPTY
            );
            sendFabricatedPacket(mc, packet, false, Integer.parseInt(timesToSend.getText()));
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            int x = width / 2 - 155, y = 40;
            context.drawText(textRenderer, "Sync Id:", x, y + 4, 0xFFFFFF, false);
            context.drawText(textRenderer, "Revision:", x, y + 24, 0xFFFFFF, false);
            context.drawText(textRenderer, "Slot:", x, y + 44, 0xFFFFFF, false);
            context.drawText(textRenderer, "Button:", x, y + 64, 0xFFFFFF, false);
            context.drawText(textRenderer, "Action:", x, y + 84, 0xFFFFFF, false);
            context.drawText(textRenderer, "Times:", x, y + 104, 0xFFFFFF, false);
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
            syncId = addField(x, y); buttonId = addField(x, y + 20);
            timesToSend = addField(x, y + 40); timesToSend.setText("1");
            addDrawableChild(ButtonWidget.builder(Text.of("Send"), b -> trySend()).width(80).position(x, y + 60).build());
            addDrawableChild(ButtonWidget.builder(Text.of("Back"), b -> mc.setScreen(parent)).width(80).position(x, y + 85).build());
        }

        private TextFieldWidget addField(int x, int y) {
            TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, 100, 18, Text.of(""));
            addDrawableChild(f);
            return f;
        }

        private void trySend() {
            if (!isInteger(syncId.getText()) || !isInteger(buttonId.getText()) || !isInteger(timesToSend.getText())) return;
            ButtonClickC2SPacket packet = new ButtonClickC2SPacket(Integer.parseInt(syncId.getText()), Integer.parseInt(buttonId.getText()));
            sendFabricatedPacket(mc, packet, false, Integer.parseInt(timesToSend.getText()));
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            int x = width / 2 - 155, y = 40;
            context.drawText(textRenderer, "Sync Id:", x, y + 4, 0xFFFFFF, false);
            context.drawText(textRenderer, "Button Id:", x, y + 24, 0xFFFFFF, false);
            context.drawText(textRenderer, "Times:", x, y + 44, 0xFFFFFF, false);
            context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);
        }
    }
}