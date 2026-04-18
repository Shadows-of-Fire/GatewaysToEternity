package dev.shadowsoffire.gateways.client;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.item.GatePearlItem;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public record GatewayColorTintSource() implements ItemTintSource {

    public static final MapCodec<GatewayColorTintSource> MAP_CODEC = MapCodec.unit(new GatewayColorTintSource());

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        DynamicHolder<Gateway> gate = GatePearlItem.getGate(stack);
        if (gate.isBound()) {
            return 0xFF000000 | gate.get().color().getValue();
        }
        return 0xFFAAAAFF;
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
