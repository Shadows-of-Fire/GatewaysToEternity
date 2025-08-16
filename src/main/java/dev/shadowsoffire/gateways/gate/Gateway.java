package dev.shadowsoffire.gateways.gate;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;

import dev.shadowsoffire.gateways.GatewayObjects;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.SpawnAlgorithms.SpawnAlgorithm;
import dev.shadowsoffire.gateways.item.GatePearlItem;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.color.GradientColor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public interface Gateway extends CodecProvider<Gateway> {

    /**
     * Returns the size of this gateway, which impacts the scale it will be rendered with, its hitbox, and the texture used for the gate pearl.
     */
    Size size();

    /**
     * Returns the color of this gateway, which is applied to the pearl texture, the portal texture, the boss bar, and related text values.
     * <p>
     * This method returns a {@link TextColor} instead of an integer literal to allow for complex colors, such as {@link GradientColor}.
     */
    TextColor color();

    /**
     * Returns the list of {@link Failure}s that will be executed if this gateway is failed for any reason.
     */
    List<Failure> failures();

    /**
     * Returns the spawn algorithm that will be used by this gateway to place any spawned {@link WaveEntity wave entities}.
     */
    SpawnAlgorithm spawnAlgo();

    /**
     * Returns the list of active {@link GateRules} used by this gateway.
     */
    GateRules rules();

    /**
     * Returns the {@link BossEventSettings} for this gateway, which control how the boss bar is rendered.
     */
    BossEventSettings bossSettings();

    /**
     * Creates a {@link GatewayEntity} for this Gateway.
     * 
     * @param level    The level in which to place the new entity.
     * @param summoner The summoning player.
     * @return The fresh entity.
     */
    GatewayEntity createEntity(Level level, Player summoner);

    /**
     * Appends tooltip lines to a {@link GatePearlItem} which targets this Gateway.
     * <p>
     * The implementation of this method must be bounced to a client-only class and guarded appropriately if it access client code.
     * 
     * @param ctx      The tooltip context.
     * @param tooltips The current list of tooltips.
     * @param flag     The tooltip flag used to collect tooltips.
     */
    void appendPearlTooltip(TooltipContext ctx, List<Component> tooltips, TooltipFlag flag);

    /**
     * Renders the boss bar and other relevant text information on a {@link GatewayEntity}.
     * 
     * @param gate      The gateway entity.
     * @param gfx       A {@link GuiGraphics} used to draw text information and other graphics.
     * @param x         The x coordinate to render at.
     * @param y         The y coordinate to render at.
     * @param isInWorld If the boss bar is being drawn in-world (atop the gateway) or on-screen (as a normal boss bar).
     */
    void renderBossBar(GatewayEntity gate, Object gfx, int x, int y, boolean isInWorld);

    /**
     * Returns the square of {@link GateRules#leashRange()}.
     */
    default double getLeashRangeSq() {
        double leashRange = this.rules().leashRange();
        return leashRange * leashRange;
    }

    /**
     * Returns the soundtrack for this gateway, which is played (in a loop) while the gateway is active.
     */
    default Holder<SoundEvent> getSoundtrack() {
        return GatewayObjects.GATE_AMBIENT;
    }

    /**
     * Checks if the player can open this gateway.
     * <p>
     * Returning null means the player can always open the gateway.
     * Returning a non-null Component means the player cannot open the gateway, and the returned Component will be displayed as an error message.
     * 
     * @apiNote This method is only called on the logical server.
     */
    @Nullable
    default Component canOpen(Player player) {
        return null;
    }

    public static enum Size {
        SMALL(1F, EntityDimensions.fixed(2F, 2F)),
        MEDIUM(2F, EntityDimensions.fixed(4F, 4F)),
        LARGE(2.5F, EntityDimensions.fixed(5.5F, 5.5F));

        public static final Codec<Size> CODEC = PlaceboCodecs.enumCodec(Size.class);

        private final float scale;
        private final EntityDimensions dims;

        Size(float scale, EntityDimensions dims) {
            this.scale = scale;
            this.dims = dims;
        }

        public float getScale() {
            return this.scale;
        }

        public EntityDimensions getDims() {
            return this.dims;
        }
    }

}
