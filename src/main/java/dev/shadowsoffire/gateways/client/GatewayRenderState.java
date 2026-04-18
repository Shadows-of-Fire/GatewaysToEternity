package dev.shadowsoffire.gateways.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;

public class GatewayRenderState extends EntityRenderState {
    public boolean valid;
    public float baseScale;
    public float clientScale;
    public boolean waveActive;
    public boolean completed;
    public int ticksActive;
    public int tickCount;
    public float bbHeight;
    public int color;
    public int setupTime;
    public boolean drawAsName;

    public Component customName;
    public float topBarProgress;
    public float bottomBarProgress;
    public Component topStatusText;
    public Component bottomStatusText;
    public boolean isEndless;
}
