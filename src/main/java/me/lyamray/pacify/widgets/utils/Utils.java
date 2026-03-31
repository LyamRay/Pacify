package me.lyamray.pacify.widgets.utils;


import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import me.lyamray.pacify.mixin.accessor.ClientConnectionAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.packet.Packet;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.stream.IntStream;

@Slf4j
@UtilityClass
public class Utils {

    public void button(Screen screen, int[] y, String label, ButtonWidget.PressAction action, int BTN_W, int BTN_X, int BTN_GAP) {
        screen.addDrawableChild(ButtonWidget.builder(Text.of(label), action)
                .width(BTN_W).position(BTN_X, y[0]).build());

        y[0] += BTN_GAP;

        log.info("Added button with label '{}'", label);
    }

    public void sendFabricatedPacket(MinecraftClient mc, Packet<?> packet, int times) {
        try {
            Runnable action = () -> {
                if (mc.getNetworkHandler() != null) {
                    mc.getNetworkHandler().sendPacket(packet);
                    ((ClientConnectionAccessor) mc.getNetworkHandler().getConnection()).getChannel().writeAndFlush(packet);
                }
            };
            IntStream.range(0, times).forEach(i -> action.run());
        } catch (Exception e) {
            log.error("Error sending fabricated packet", e);
        }
    }
    public boolean isInteger(String s){
        try {
            Integer.parseInt(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public SlotActionType stringToSlotActionType(String s) {
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
}
