package dev.shadowsoffire.gateways.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class GatewayParticle extends SingleQuadParticle {

    public GatewayParticle(GatewayParticleData data, ClientLevel level, double x, double y, double z, double velX, double velY, double velZ, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
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
    protected int getLightCoords(float partialTicks) {
        return LightCoordsUtil.pack(15, 15);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
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

    public static class Provider implements ParticleProvider<GatewayParticleData> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public GatewayParticle createParticle(GatewayParticleData data, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
            return new GatewayParticle(data, level, x, y, z, xd, yd, zd, this.sprites.get(random));
        }
    }
}
