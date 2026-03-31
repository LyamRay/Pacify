package me.lyamray.pacify;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.packet.Packet;
import net.minecraft.screen.ScreenHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@UtilityClass

public class SharedVariables {

    public boolean sendUIPackets = true;
    public boolean delayUIPackets = false;
    public boolean shouldEditSign = true;
    public boolean enabled = true;
    public boolean bypassResourcePack = false;
    public boolean resourcePackForceDeny = false;

    public final ArrayList<Packet<?>> delayedUIPackets = new ArrayList<>();

    public int latestSyncId = 0;
    public int latestRevision = 0;

    public Screen storedScreen = null;
    public ScreenHandler storedScreenHandler = null;

    public final Map<String, Object[]> savedGuis = new LinkedHashMap<>();

    public double estimatedTPS = 20.0;
    public long lastTimeUpdate = System.currentTimeMillis();
}