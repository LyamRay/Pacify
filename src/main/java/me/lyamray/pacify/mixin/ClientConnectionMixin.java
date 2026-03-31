package me.lyamray.pacify.mixin;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.lyamray.pacify.SharedVariables;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

    @Inject(at = @At("HEAD"), method = "sendImmediately", cancellable = true)
    public void sendImmediately(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        boolean isUiPacket = packet instanceof ClickSlotC2SPacket || packet instanceof ButtonClickC2SPacket;

        if (isUiPacket && !SharedVariables.sendUIPackets) { ci.cancel(); return; }
        if (isUiPacket && SharedVariables.delayUIPackets) { SharedVariables.delayedUIPackets.add(packet); ci.cancel(); return; }

        if (!SharedVariables.shouldEditSign && packet instanceof UpdateSignC2SPacket) {
            SharedVariables.shouldEditSign = true;
            ci.cancel();
        }
    }
}