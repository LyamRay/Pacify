package me.lyamray.pacify;

import com.google.gson.Gson;
import com.mojang.serialization.JsonOps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.lyamray.pacify.widgets.fabricate.FabricatePacketScreen;
import me.lyamray.pacify.widgets.load.LoadGuiScreen;
import me.lyamray.pacify.widgets.save.SaveGuiScreen;
import me.lyamray.pacify.widgets.utils.Utils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

@Slf4j
public class MainClient implements ClientModInitializer {

    @Getter
    private static MainClient instance;

    public KeyBinding restoreScreenKey;
    private static final KeyBinding.Category PACIFY_CATEGORY =
            KeyBinding.Category.create(Identifier.of("key.categories.pacify"));

    private static final int BTN_W = 110;
    private static final int BTN_X = 5;
    private static final int BTN_Y0 = 5;
    private static final int BTN_GAP = 22;

    private static final String VERSION = FabricLoader.getInstance()
            .getModContainer("pacify")
            .map(m -> m.getMetadata().getVersion().getFriendlyString())
            .orElse("?");

    @Override
    public void onInitializeClient() {
        instance = this;
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

            long now = System.currentTimeMillis();
            long diff = now - SharedVariables.lastTimeUpdate;
            if (diff > 0) SharedVariables.estimatedTPS = Math.min(20.0, 1000.0 / diff);
            SharedVariables.lastTimeUpdate = now;
        });
    }

    public void createText(MinecraftClient minecraftClient, DrawContext context, TextRenderer textRenderer) {
        context.drawText(textRenderer, "Sync Id: " + minecraftClient.player.currentScreenHandler.syncId, 150, 5, Color.WHITE.getRGB(), false);
        context.drawText(textRenderer, "Revision: " + minecraftClient.player.currentScreenHandler.getRevision(), 150, 35, Color.WHITE.getRGB(), false);
    }

    public void createWidgets(MinecraftClient minecraftClient, Screen screen) {
        int[] y = {BTN_Y0};
        Utils.button(screen, y, "Close without packet", b -> closeWithoutPacket(minecraftClient), BTN_W, BTN_X, BTN_GAP);
        Utils.button(screen, y, "De-sync", b -> desync(minecraftClient), BTN_W, BTN_X, BTN_GAP);
        Utils.button(screen, y, "Send packets: " + SharedVariables.sendUIPackets, this::toggleSendPackets, BTN_W, BTN_X, BTN_GAP);
        Utils.button(screen, y, "Delay packets: " + SharedVariables.delayUIPackets, b -> toggleDelayPackets(minecraftClient, b), BTN_W, BTN_X, BTN_GAP);
        Utils.button(screen, y, "Save GUI", b -> minecraftClient.setScreen(new SaveGuiScreen(screen, minecraftClient)), BTN_W, BTN_X, BTN_GAP);
        Utils.button(screen, y, "Load GUI", b -> minecraftClient.setScreen(new LoadGuiScreen(screen, minecraftClient)), BTN_W, BTN_X, BTN_GAP);
        Utils.button(screen, y, "Disconnect & send packet", b -> disconnectAndSend(minecraftClient), BTN_W, BTN_X, BTN_GAP);
        Utils.button(screen, y, "Fabricate packet", b -> minecraftClient.setScreen(new FabricatePacketScreen(screen, minecraftClient)), BTN_W, BTN_X, BTN_GAP);
        Utils.button(screen, y, "Copy Title JSON", b -> copyTitleJson(minecraftClient), BTN_W, BTN_X, BTN_GAP);
    }

    public void renderInfo(MinecraftClient minecraftClient, DrawContext context, TextRenderer textRenderer) {
        if (minecraftClient.player == null || minecraftClient.currentScreen == null || minecraftClient.getNetworkHandler() == null) return;
        var net = minecraftClient.getNetworkHandler();
        var entry = net.getPlayerListEntry(minecraftClient.player.getUuid());
        var server = minecraftClient.getCurrentServerEntry();
        context.drawCenteredTextWithShadow(textRenderer, "Pacify v" + VERSION, minecraftClient.currentScreen.width / 2, 5, Color.CYAN.getRGB());
        int x = minecraftClient.currentScreen.width - 110;
        int[] y = {5};

        drawInfo(context, textRenderer, x, y, "Server: " + (server != null ? server.address : "Singleplayer"));
        drawInfo(context, textRenderer, x, y, "Software: " + net.getBrand());
        drawInfo(context, textRenderer, x, y, "Players: " + net.getPlayerList().size());
        drawInfo(context, textRenderer, x, y, "Ping: " + (entry != null ? entry.getLatency() : 0) + "ms");
        drawInfo(context, textRenderer, x, y, "TPS: " + String.format("%.2f", SharedVariables.estimatedTPS));
    }

    private void drawInfo(DrawContext context, TextRenderer tr, int x, int[] y, String text) {
        context.drawText(tr, text, x, y[0], Color.WHITE.getRGB(), false);
        y[0] += 10;
    }

    void closeWithoutPacket(MinecraftClient minecraftClient) {
        if (minecraftClient.player == null) return;
        SharedVariables.storedScreen = minecraftClient.currentScreen;
        SharedVariables.storedScreenHandler = minecraftClient.player.currentScreenHandler;
        minecraftClient.setScreen(null);
    }

    void desync(MinecraftClient minecraftClient) {
        if (minecraftClient.getNetworkHandler() == null || minecraftClient.player == null) return;
        minecraftClient.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(minecraftClient.player.currentScreenHandler.syncId));
    }

    void toggleSendPackets(ButtonWidget b) {
        SharedVariables.sendUIPackets = !SharedVariables.sendUIPackets;
        b.setMessage(Text.of("Send packets: " + SharedVariables.sendUIPackets));
    }

    void toggleDelayPackets(MinecraftClient minecraftClient, ButtonWidget b) {
        SharedVariables.delayUIPackets = !SharedVariables.delayUIPackets;
        b.setMessage(Text.of("Delay packets: " + SharedVariables.delayUIPackets));
        if (!SharedVariables.delayUIPackets) flushDelayedPackets(minecraftClient);
    }

    void flushDelayedPackets(MinecraftClient minecraftClient) {
        if (minecraftClient.player == null || SharedVariables.delayedUIPackets.isEmpty() || minecraftClient.getNetworkHandler() == null) return;
        SharedVariables.delayedUIPackets.forEach(p -> minecraftClient.getNetworkHandler().sendPacket(p));
        minecraftClient.player.sendMessage(Text.of("Sent " + SharedVariables.delayedUIPackets.size() + " packets."), false);
        SharedVariables.delayedUIPackets.clear();
    }

    void disconnectAndSend(MinecraftClient minecraftClient) {
        SharedVariables.delayUIPackets = false;
        if (minecraftClient.getNetworkHandler() == null) return;
        SharedVariables.delayedUIPackets.forEach(p -> minecraftClient.getNetworkHandler().sendPacket(p));
        minecraftClient.getNetworkHandler().getConnection().disconnect(Text.of("Disconnecting (Pacify)"));
        SharedVariables.delayedUIPackets.clear();
    }

    void copyTitleJson(MinecraftClient minecraftClient) {
        if (minecraftClient.currentScreen == null) return;
        minecraftClient.keyboard.setClipboard(new Gson().toJson(
                TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, minecraftClient.currentScreen.getTitle()).getOrThrow()));
    }
}