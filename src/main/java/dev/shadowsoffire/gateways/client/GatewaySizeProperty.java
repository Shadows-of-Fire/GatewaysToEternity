package dev.shadowsoffire.gateways.client;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.item.GatePearlItem;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public record GatewaySizeProperty() implements SelectItemModelProperty<String> {

    public static final SelectItemModelProperty.Type<GatewaySizeProperty, String> TYPE = SelectItemModelProperty.Type.create(
        MapCodec.unit(new GatewaySizeProperty()),
        Codec.STRING
    );

    @Nullable
    @Override
    public String get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext displayContext) {
        DynamicHolder<Gateway> gate = GatePearlItem.getGate(stack);
        if (gate.isBound()) {
            return gate.get().size().name().toLowerCase(java.util.Locale.ROOT);
        }
        return "large";
    }

    @Override
    public Codec<String> valueCodec() {
        return Codec.STRING;
    }

    @Override
    public SelectItemModelProperty.Type<? extends SelectItemModelProperty<String>, String> type() {
        return TYPE;
    }
}
