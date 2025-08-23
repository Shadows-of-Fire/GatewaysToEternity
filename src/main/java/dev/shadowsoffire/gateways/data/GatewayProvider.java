package dev.shadowsoffire.gateways.data;

import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.gate.BossEventSettings;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.gateways.gate.Reward.EntityLootReward;
import dev.shadowsoffire.gateways.gate.Reward.ExperienceReward;
import dev.shadowsoffire.gateways.gate.SpawnAlgorithms;
import dev.shadowsoffire.gateways.gate.StandardWaveEntity;
import dev.shadowsoffire.gateways.gate.WaveModifier.AttributeModifier;
import dev.shadowsoffire.gateways.gate.endless.ApplicationMode.AfterEveryNWaves;
import dev.shadowsoffire.gateways.gate.endless.EndlessGateway;
import dev.shadowsoffire.gateways.gate.normal.NormalGateway;
import dev.shadowsoffire.placebo.util.data.DynamicRegistryProvider;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class GatewayProvider extends DynamicRegistryProvider<Gateway> {

    public GatewayProvider(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, GatewayRegistry.INSTANCE);
    }

    @Override
    public String getName() {
        return "Gateways";
    }

    @Override
    public void generate() {
        normalGateway("basic/blaze", b -> b
            .size(Gateway.Size.SMALL)
            .color(0xFFFF84)
            .wave(w -> w
                .maxWaveTime(800)
                .setupTime(100)
                .entity(StandardWaveEntity
                    .builder(EntityType.BLAZE)
                    .count(3)
                    .build())
                .reward(new EntityLootReward(EntityType.BLAZE, null, 10)))
            .wave(w -> w
                .maxWaveTime(800)
                .setupTime(150)
                .entity(StandardWaveEntity
                    .builder(EntityType.BLAZE)
                    .count(4)
                    .build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.15F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 2.0F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.15F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.PROJECTILE_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.15F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.05F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, 0.05F))
                .reward(new EntityLootReward(EntityType.BLAZE, null, 15)))
            .wave(w -> w
                .maxWaveTime(800)
                .setupTime(200)
                .entity(StandardWaveEntity
                    .builder(EntityType.BLAZE)
                    .count(5)
                    .build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.20F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 3.0F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.20F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.PROJECTILE_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.20F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.15F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, 0.10F))
                .reward(new EntityLootReward(EntityType.BLAZE, null, 20)))
            .wave(w -> w
                .maxWaveTime(1000)
                .setupTime(280)
                .entity(StandardWaveEntity
                    .builder(EntityType.BLAZE)
                    .count(6)
                    .build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.25F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 5.0F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.30F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.PROJECTILE_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.30F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.15F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, 0.15F))
                .reward(new EntityLootReward(EntityType.BLAZE, null, 25)))
            .wave(w -> w
                .maxWaveTime(1200)
                .setupTime(340)
                .entity(StandardWaveEntity
                    .builder(EntityType.BLAZE)
                    .count(7)
                    .build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.35F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 5.0F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.50F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.PROJECTILE_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.50F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.20F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, 0.15F))
                .reward(new ExperienceReward(500, 25)))
            .keyReward(new EntityLootReward(EntityType.BLAZE, null, 75)));

        endlessGateway("endless/blaze", b -> b
            .size(Gateway.Size.MEDIUM)
            .color(0xFFFF84)
            .baseWave(w -> w
                .maxWaveTime(800)
                .setupTime(100)
                .entity(StandardWaveEntity
                    .builder(EntityType.BLAZE)
                    .count(3)
                    .build())
                .reward(new EntityLootReward(EntityType.BLAZE, null, 10)))
            .modifier(m -> m
                .applicationMode(new AfterEveryNWaves(3, 10)) // TODO: This isn't that readable. Static factory methods on ApplicationMode?
                .entity(StandardWaveEntity
                    .builder(EntityType.BLAZE)
                    .count(3)
                    .build())
                .reward(new EntityLootReward(EntityType.BLAZE, null, 10))
                .setupTime(-10)
                .maxWaveTime(-40))
            .modifier(m -> m
                .applicationMode(new AfterEveryNWaves(5, 3))
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.15F)))
            .bossSettings(new BossEventSettings(BossEventSettings.Mode.NAME_PLATE, false))
            .spawnAlgo(SpawnAlgorithms.INWARD_SPIRAL));
    }

    private void normalGateway(String path, UnaryOperator<NormalGateway.Builder> config) {
        this.add(Gateways.loc(path), config.apply(NormalGateway.builder()).build());
    }

    private void endlessGateway(String path, UnaryOperator<EndlessGateway.Builder> config) {
        this.add(Gateways.loc(path), config.apply(EndlessGateway.builder()).build());
    }

}
