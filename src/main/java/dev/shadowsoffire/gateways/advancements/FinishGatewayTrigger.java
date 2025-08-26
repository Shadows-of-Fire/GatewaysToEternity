package dev.shadowsoffire.gateways.advancements;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

public class FinishGatewayTrigger extends SimpleCriterionTrigger<FinishGatewayTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, Gateway gateway) {
        DynamicHolder<Gateway> holder = GatewayRegistry.INSTANCE.holder(gateway);
        this.trigger(player, inst -> inst.test(holder));
    }

    public static record Instance(Optional<ContextAwarePredicate> player, DynamicHolder<Gateway> gateway) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
            GatewayRegistry.INSTANCE.holderCodec().fieldOf("gateway").forGetter(Instance::gateway))
            .apply(inst, Instance::new));

        public boolean test(DynamicHolder<Gateway> gateway) {
            return this.gateway.equals(gateway);
        }
    }

}
