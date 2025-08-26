package dev.shadowsoffire.gateways.client;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;

@SuppressWarnings("deprecation")
public class GatewayParticle extends TextureSheetParticle {

    static final ParticleRenderType RENDER_TYPE = new ParticleRenderType(){
        @Override
        public BufferBuilder begin(Tesselator tess, TextureManager manager) {
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
            return tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "GatewayParticleType";
        }
    };

    public GatewayParticle(GatewayParticleData data, ClientLevel level, double x, double y, double z, double velX, double velY, double velZ) {
        super(level, x, y, z, velX, velY, velZ);
        this.rCol = data.red();
        this.gCol = data.green();
        this.bCol = data.blue();
        this.lifetime = 40;
        this.xd = velX;
        this.yd = velY;
        this.zd = velZ;
        this.speedUpWhenYMotionIsBlocked = true;
        this.friction = 0.86F;
    }

    @Override
    protected int getLightColor(float partialTicks) {
        return LightTexture.pack(15, 15);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return RENDER_TYPE;
    }

    @Override
    public float getQuadSize(float p_217561_1_) {
        return 0.75F * this.quadSize * Mth.clamp((this.age + p_217561_1_) / this.lifetime * 32.0F, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        this.alpha = 1 - (float) this.age / this.lifetime;
    }

}
