package dev.shadowsoffire.gateways.data;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.gate.BossEventSettings;
import dev.shadowsoffire.gateways.gate.Failure.ExplosionFailure;
import dev.shadowsoffire.gateways.gate.Failure.MobEffectFailure;
import dev.shadowsoffire.gateways.gate.Failure.SummonFailure;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.gateways.gate.Reward.EntityLootReward;
import dev.shadowsoffire.gateways.gate.Reward.ExperienceReward;
import dev.shadowsoffire.gateways.gate.Reward.LootTableReward;
import dev.shadowsoffire.gateways.gate.Reward.StackListReward;
import dev.shadowsoffire.gateways.gate.Reward.StackReward;
import dev.shadowsoffire.gateways.gate.Reward.SummonReward;
import dev.shadowsoffire.gateways.gate.SpawnAlgorithms;
import dev.shadowsoffire.gateways.gate.StandardWaveEntity;
import dev.shadowsoffire.gateways.gate.WaveModifier.AttributeModifier;
import dev.shadowsoffire.gateways.gate.WaveModifier.EffectModifier;
import dev.shadowsoffire.gateways.gate.WaveModifier.GearSetModifier;
import dev.shadowsoffire.gateways.gate.endless.ApplicationMode.AfterEveryNWaves;
import dev.shadowsoffire.gateways.gate.endless.EndlessGateway;
import dev.shadowsoffire.gateways.gate.normal.NormalGateway;
import dev.shadowsoffire.placebo.json.ChancedEffectInstance;
import dev.shadowsoffire.placebo.util.StepFunction;
import dev.shadowsoffire.placebo.util.data.DynamicRegistryProvider;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntityType;

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
        genBlazeGateway();
        genEndlessBlazeGateway();
        genEndermanGateway();
        genSlimeGateway();
        genEmeraldGrove();
        genHellishFortress();
        genOverworldianNights();
        genIronAutomaton();
        genEndlessAutomatonFabricator();
    }

    private void genBlazeGateway() {
        normalGateway("basic/blaze", b -> b
            .size(Gateway.Size.SMALL)
            .color(0xFFFF84)
            .wave(w -> w.maxWaveTime(800).setupTime(100)
                .entity(StandardWaveEntity.builder(EntityType.BLAZE).count(3).build())
                .reward(new EntityLootReward(EntityType.BLAZE, null, 10)))
            .wave(w -> w.maxWaveTime(800).setupTime(150)
                .entity(StandardWaveEntity.builder(EntityType.BLAZE).count(4).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.15F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 2.0F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.15F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.PROJECTILE_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.15F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.05F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, 0.05F))
                .reward(new EntityLootReward(EntityType.BLAZE, null, 15)))
            .wave(w -> w.maxWaveTime(800).setupTime(200)
                .entity(StandardWaveEntity.builder(EntityType.BLAZE).count(5).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.20F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 3.0F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.20F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.PROJECTILE_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.20F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.15F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, 0.10F))
                .reward(new EntityLootReward(EntityType.BLAZE, null, 20)))
            .wave(w -> w.maxWaveTime(1000).setupTime(280)
                .entity(StandardWaveEntity.builder(EntityType.BLAZE).count(6).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.25F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 5.0F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.30F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.PROJECTILE_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.30F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.15F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, 0.15F))
                .reward(new EntityLootReward(EntityType.BLAZE, null, 25)))
            .wave(w -> w.maxWaveTime(1200).setupTime(340)
                .entity(StandardWaveEntity.builder(EntityType.BLAZE).count(7).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.35F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 5.0F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.50F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.PROJECTILE_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.50F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.20F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, 0.15F))
                .reward(new ExperienceReward(500, 25)))
            .keyReward(new EntityLootReward(EntityType.BLAZE, null, 75)));
    }

    private void genEndlessBlazeGateway() {
        endlessGateway("endless/blaze", b -> b
            .size(Gateway.Size.MEDIUM).color(0xFFFF84)
            .baseWave(w -> w.maxWaveTime(800).setupTime(100)
                .entity(StandardWaveEntity.builder(EntityType.BLAZE).count(3).build())
                .reward(new EntityLootReward(EntityType.BLAZE, null, 10)))
            .modifier(m -> m.applicationMode(new AfterEveryNWaves(3, 10))
                .entity(StandardWaveEntity.builder(EntityType.BLAZE).count(3).build())
                .reward(new EntityLootReward(EntityType.BLAZE, null, 10))
                .setupTime(-10).maxWaveTime(-40))
            .modifier(m -> m.applicationMode(new AfterEveryNWaves(5, 3))
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.15F)))
            .bossSettings(new BossEventSettings(BossEventSettings.Mode.NAME_PLATE, false))
            .spawnAlgo(SpawnAlgorithms.INWARD_SPIRAL));
    }

    private void genEndermanGateway() {
        CompoundTag flamingNbt = namedNbt("name.gateways.flaming_enderman", "red");
        flamingNbt.putShort("Fire", (short) 32767);

        normalGateway("basic/enderman", b -> b
            .size(Gateway.Size.SMALL).color(0x662266)
            .wave(w -> w.maxWaveTime(800).setupTime(100)
                .entity(StandardWaveEntity.builder(EntityType.ENDERMAN).count(3).build())
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, -0.20F))
                .reward(new EntityLootReward(EntityType.ENDERMAN, null, 15)))
            .wave(w -> w.maxWaveTime(1200).setupTime(150)
                .entity(StandardWaveEntity.builder(EntityType.ENDERMAN).count(3).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.15F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 2F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.15F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.05F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, -0.20F))
                .reward(new EntityLootReward(EntityType.ENDERMAN, null, 25)))
            .wave(w -> w.maxWaveTime(1400).setupTime(200)
                .entity(StandardWaveEntity.builder(EntityType.ENDERMAN).count(4).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.20F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 3F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.20F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.15F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, -0.20F))
                .reward(new EntityLootReward(EntityType.ENDERMAN, null, 35)))
            .wave(w -> w.maxWaveTime(1600).setupTime(280)
                .entity(StandardWaveEntity.builder(EntityType.ENDERMAN).count(4).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.25F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 5F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.30F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.15F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, -0.20F))
                .reward(new EntityLootReward(EntityType.ENDERMAN, null, 45)))
            .wave(w -> w.maxWaveTime(2000).setupTime(340)
                .entity(StandardWaveEntity.builder(EntityType.ENDERMAN)
                    .desc("name.gateways.flaming_enderman").nbt(t -> flamingNbt)
                    .addModifier(new EffectModifier(new ChancedEffectInstance(1F, MobEffects.FIRE_RESISTANCE, new StepFunction(0, 1, 0, 0), true, false)))
                    .addModifier(AttributeModifier.create(ALObjects.Attributes.FIRE_DAMAGE, Operation.ADD_VALUE, 4F))
                    .addModifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_VALUE, 8F))
                    .count(1).finalizeSpawn(false).build())
                .entity(StandardWaveEntity.builder(EntityType.ENDERMAN).count(3).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.35F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 5F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.50F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.20F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, -0.20F))
                .reward(new ExperienceReward(500, 25)))
            .keyReward(new EntityLootReward(EntityType.ENDERMAN, null, 100)));
    }

    private void genSlimeGateway() {
        CompoundTag size1 = sizeNbt(1);
        CompoundTag size3 = sizeNbt(3);
        CompoundTag size0 = sizeNbt(0);
        CompoundTag acidicNbt = namedNbt("name.gateways.acidic_slime", "green");
        acidicNbt.putInt("Size", 3);
        CompoundTag magicbaneNbt = namedNbt("name.gateways.magicbane_slime", "blue");
        magicbaneNbt.putInt("Size", 3);
        CompoundTag hugeNbt = namedNbt("name.gateways.huge_slime", "green");
        hugeNbt.putInt("Size", 7);

        normalGateway("basic/slime", b -> b
            .size(Gateway.Size.SMALL).color(0x33AE53)
            .wave(w -> w.maxWaveTime(800).setupTime(100)
                .entity(StandardWaveEntity.builder(EntityType.SLIME).nbt(t -> size1).count(3).finalizeSpawn(false).build())
                .reward(new EntityLootReward(EntityType.SLIME, size0, 5)))
            .wave(w -> w.maxWaveTime(800).setupTime(150)
                .entity(StandardWaveEntity.builder(EntityType.SLIME).nbt(t -> size3).count(3).finalizeSpawn(false).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.15F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 2F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.15F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.05F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, 0.05F))
                .reward(new EntityLootReward(EntityType.SLIME, size0, 7)))
            .wave(w -> w.maxWaveTime(1200).setupTime(200)
                .entity(StandardWaveEntity.builder(EntityType.SLIME).desc("name.gateways.acidic_slime").nbt(t -> acidicNbt)
                    .addModifier(AttributeModifier.create(ALObjects.Attributes.ARMOR_SHRED, Operation.ADD_VALUE, 0.5F))
                    .addModifier(AttributeModifier.create(ALObjects.Attributes.FIRE_DAMAGE, Operation.ADD_VALUE, 2F))
                    .count(3).finalizeSpawn(false).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.15F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 2F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.10F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.15F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, 0.10F))
                .reward(new EntityLootReward(EntityType.SLIME, size0, 15)))
            .wave(w -> w.maxWaveTime(1800).setupTime(280)
                .entity(StandardWaveEntity.builder(EntityType.SLIME).desc("name.gateways.magicbane_slime").nbt(t -> magicbaneNbt)
                    .addModifier(AttributeModifier.create(ALObjects.Attributes.PROT_SHRED, Operation.ADD_VALUE, 0.5F))
                    .addModifier(AttributeModifier.create(ALObjects.Attributes.COLD_DAMAGE, Operation.ADD_VALUE, 2F))
                    .count(3).finalizeSpawn(false).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.15F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 2F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.10F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.10F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, 0.10F))
                .reward(new EntityLootReward(EntityType.SLIME, size0, 12)))
            .wave(w -> w.maxWaveTime(2600).setupTime(200)
                .entity(StandardWaveEntity.builder(EntityType.SLIME).desc("name.gateways.huge_slime").nbt(t -> hugeNbt).count(1).finalizeSpawn(false).build())
                .entity(StandardWaveEntity.builder(EntityType.SLIME).desc("name.gateways.magicbane_slime").nbt(t -> magicbaneNbt)
                    .addModifier(AttributeModifier.create(ALObjects.Attributes.PROT_SHRED, Operation.ADD_VALUE, 0.5F))
                    .addModifier(AttributeModifier.create(ALObjects.Attributes.COLD_DAMAGE, Operation.ADD_VALUE, 2F))
                    .count(2).finalizeSpawn(false).build())
                .entity(StandardWaveEntity.builder(EntityType.SLIME).desc("name.gateways.acidic_slime").nbt(t -> acidicNbt)
                    .addModifier(AttributeModifier.create(ALObjects.Attributes.ARMOR_SHRED, Operation.ADD_VALUE, 0.5F))
                    .addModifier(AttributeModifier.create(ALObjects.Attributes.FIRE_DAMAGE, Operation.ADD_VALUE, 2F))
                    .count(2).finalizeSpawn(false).build())
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 4F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.20F))
                .reward(new ExperienceReward(500, 25)))
            .keyReward(new EntityLootReward(EntityType.SLIME, size0, 75)));
    }

    private void genEmeraldGrove() {
        CompoundTag farmerNbt = namedNbt("name.gateways.necrotic_farmer", "red");
        CompoundTag villagerData = new CompoundTag();
        villagerData.putString("profession", "minecraft:farmer");
        farmerNbt.put("VillagerData", villagerData);

        normalGateway("emerald_grove", b -> b
            .size(Gateway.Size.MEDIUM).color(0x056608)
            .wave(w -> w.maxWaveTime(1000).setupTime(200)
                .entity(StandardWaveEntity.builder(EntityType.ZOMBIE).count(5).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.25F))
                .reward(new StackReward(template(Items.HAY_BLOCK, 16))))
            .wave(w -> w.maxWaveTime(1000).setupTime(200)
                .entity(StandardWaveEntity.builder(EntityType.SPIDER).count(5).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.20F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, 0.05F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 4F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_VALUE, 2.5F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.ARMOR_PIERCE, Operation.ADD_VALUE, 2F))
                .reward(new StackListReward(List.of(
                    template(Items.CACTUS, 16), template(Items.SUGAR_CANE, 16), template(Items.BAMBOO, 16)))))
            .wave(w -> w.maxWaveTime(1000).setupTime(200)
                .entity(StandardWaveEntity.builder(EntityType.ZOMBIE).count(5).build())
                .entity(StandardWaveEntity.builder(EntityType.SPIDER).count(2).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.35F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, 0.15F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 5F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_VALUE, 2.5F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.ARMOR_PIERCE, Operation.ADD_VALUE, 3F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.LIFE_STEAL, Operation.ADD_VALUE, 0.10F))
                .reward(new StackListReward(List.of(
                    template(Items.CARROT, 32), template(Items.POTATO, 32),
                    template(Items.APPLE, 32), template(Items.BEETROOT, 32)))))
            .wave(w -> w.maxWaveTime(1200).setupTime(200)
                .entity(StandardWaveEntity.builder(EntityType.ZOMBIE_VILLAGER).desc("name.gateways.necrotic_farmer").nbt(t -> farmerNbt)
                    .addModifier(GearSetModifier.create(Gateways.loc("iron"))).finalizeSpawn(false).build())
                .entity(StandardWaveEntity.builder(EntityType.ZOMBIE).count(3).build())
                .entity(StandardWaveEntity.builder(EntityType.SPIDER).count(2).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.40F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.5F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 5F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_VALUE, 4F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.ARMOR_PIERCE, Operation.ADD_VALUE, 3F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.LIFE_STEAL, Operation.ADD_VALUE, 0.10F))
                .reward(new StackListReward(List.of(
                    template(Items.OAK_SAPLING, 16), template(Items.BIRCH_SAPLING, 16),
                    template(Items.SPRUCE_SAPLING, 16), template(Items.DARK_OAK_SAPLING, 16),
                    template(Items.ACACIA_SAPLING, 16), template(Items.CHERRY_SAPLING, 16)))))
            .keyReward(new SummonReward(StandardWaveEntity.builder(EntityType.COW).count(6).build()))
            .keyReward(new SummonReward(StandardWaveEntity.builder(EntityType.CHICKEN).count(6).build()))
            .keyReward(new SummonReward(StandardWaveEntity.builder(EntityType.SHEEP).count(6).build()))
            .keyReward(new SummonReward(StandardWaveEntity.builder(EntityType.PIG).count(6).build()))
            .failure(new ExplosionFailure(2, true, true)));
    }

    private void genHellishFortress() {
        CompoundTag rangerNbt = namedNbt("name.gateways.withered_ranger", "red");
        CompoundTag butcherNbt = namedNbt("name.gateways.butcher", "red");
        butcherNbt.putBoolean("IsImmuneToZombification", true);

        normalGateway("hellish_fortress", b -> b
            .size(Gateway.Size.LARGE).color(0xCF352E)
            .wave(w -> w.maxWaveTime(600).setupTime(100)
                .entity(StandardWaveEntity.builder(EntityType.ZOMBIFIED_PIGLIN).count(5).build())
                .entity(StandardWaveEntity.builder(EntityType.WITHER_SKELETON).count(3).build())
                .reward(new EntityLootReward(EntityType.ZOMBIFIED_PIGLIN, null, 10))
                .reward(new EntityLootReward(EntityType.WITHER_SKELETON, null, 10)))
            .wave(w -> w.maxWaveTime(1200).setupTime(200)
                .entity(StandardWaveEntity.builder(EntityType.ZOMBIFIED_PIGLIN).count(8).build())
                .entity(StandardWaveEntity.builder(EntityType.WITHER_SKELETON).count(5).build())
                .entity(StandardWaveEntity.builder(EntityType.BLAZE).count(3).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.10F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, 0.05F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.FIRE_DAMAGE, Operation.ADD_VALUE, 2F))
                .reward(new EntityLootReward(EntityType.ZOMBIFIED_PIGLIN, null, 15))
                .reward(new EntityLootReward(EntityType.WITHER_SKELETON, null, 15))
                .reward(new EntityLootReward(EntityType.BLAZE, null, 10)))
            .wave(w -> w.maxWaveTime(1600).setupTime(400)
                .entity(StandardWaveEntity.builder(EntityType.ZOMBIFIED_PIGLIN).count(8).build())
                .entity(StandardWaveEntity.builder(EntityType.WITHER_SKELETON).count(5).build())
                .entity(StandardWaveEntity.builder(EntityType.WITHER_SKELETON).desc("name.gateways.withered_ranger").nbt(t -> rangerNbt)
                    .addModifier(GearSetModifier.create(Gateways.loc("chain_with_bow")))
                    .addModifier(AttributeModifier.create(ALObjects.Attributes.ARROW_DAMAGE, Operation.ADD_MULTIPLIED_BASE, 0.2F))
                    .count(3).finalizeSpawn(false).build())
                .entity(StandardWaveEntity.builder(EntityType.BLAZE).count(3).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.15F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, 0.05F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.FIRE_DAMAGE, Operation.ADD_VALUE, 3F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.ARROW_DAMAGE, Operation.ADD_VALUE, 0.10F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.LIFE_STEAL, Operation.ADD_VALUE, 0.10F))
                .reward(new EntityLootReward(EntityType.ZOMBIFIED_PIGLIN, null, 15))
                .reward(new EntityLootReward(EntityType.WITHER_SKELETON, null, 30))
                .reward(new EntityLootReward(EntityType.BLAZE, null, 15)))
            .wave(w -> w.maxWaveTime(2400).setupTime(400)
                .entity(StandardWaveEntity.builder(EntityType.PIGLIN_BRUTE).desc("name.gateways.butcher").nbt(t -> butcherNbt)
                    .addModifier(GearSetModifier.create(Gateways.loc("gold_with_axe")))
                    .addModifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_VALUE, 30F))
                    .addModifier(AttributeModifier.create(ALObjects.Attributes.ARMOR_SHRED, Operation.ADD_VALUE, 0.5F))
                    .addModifier(AttributeModifier.create(ALObjects.Attributes.PROT_SHRED, Operation.ADD_VALUE, 0.25F))
                    .addModifier(AttributeModifier.create(Attributes.ATTACK_KNOCKBACK, Operation.ADD_VALUE, 2.5F))
                    .addModifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.25F))
                    .addModifier(AttributeModifier.create(ALObjects.Attributes.LIFE_STEAL, Operation.ADD_VALUE, 0.10F))
                    .count(1).finalizeSpawn(false).build())
                .entity(StandardWaveEntity.builder(EntityType.WITHER_SKELETON).desc("name.gateways.withered_ranger").nbt(t -> rangerNbt)
                    .addModifier(GearSetModifier.create(Gateways.loc("chain_with_bow")))
                    .addModifier(AttributeModifier.create(ALObjects.Attributes.ARROW_DAMAGE, Operation.ADD_MULTIPLIED_BASE, 0.2F))
                    .count(5).finalizeSpawn(false).build())
                .entity(StandardWaveEntity.builder(EntityType.BLAZE).count(3).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.25F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.5F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 3F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.FIRE_DAMAGE, Operation.ADD_VALUE, 4F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.LIFE_STEAL, Operation.ADD_VALUE, 0.10F))
                .reward(new EntityLootReward(EntityType.PIGLIN_BRUTE, null, 20))
                .reward(new EntityLootReward(EntityType.WITHER_SKELETON, null, 40))
                .reward(new EntityLootReward(EntityType.BLAZE, null, 40)))
            .keyReward(LootTableReward.create(ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/nether_bridge")), 10, "rewards.gateways.loot_table.nether_bridge"))
            .keyReward(new StackReward(witherSkeletonSpawnerTemplate()))
            .failure(new ExplosionFailure(4, true, true))
            .failure(new SummonFailure(StandardWaveEntity.builder(EntityType.BLAZE).count(2).build())));
    }

    private void genOverworldianNights() {
        CompoundTag legionnaireNbt = namedNbt("name.gateways.undead_legionnaire", "red");

        normalGateway("overworldian_nights", b -> b
            .size(Gateway.Size.MEDIUM).color(0x5C54A4)
            .wave(w -> w.maxWaveTime(1000).setupTime(100)
                .entity(StandardWaveEntity.builder(EntityType.ZOMBIE).count(5).build())
                .entity(StandardWaveEntity.builder(EntityType.SKELETON).count(3).build())
                .entity(StandardWaveEntity.builder(EntityType.SPIDER).count(1).build())
                .entity(StandardWaveEntity.builder(EntityType.CREEPER).count(1).build())
                .reward(new EntityLootReward(EntityType.ZOMBIE, null, 10))
                .reward(new EntityLootReward(EntityType.SKELETON, null, 10))
                .reward(new EntityLootReward(EntityType.SPIDER, null, 5))
                .reward(new EntityLootReward(EntityType.CREEPER, null, 5)))
            .wave(w -> w.maxWaveTime(1000).setupTime(400)
                .entity(StandardWaveEntity.builder(EntityType.ZOMBIE).count(8).build())
                .entity(StandardWaveEntity.builder(EntityType.SKELETON).count(5).build())
                .entity(StandardWaveEntity.builder(EntityType.SPIDER).count(3).build())
                .entity(StandardWaveEntity.builder(EntityType.CREEPER).count(2).build())
                .entity(StandardWaveEntity.builder(EntityType.WITCH).count(1).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.10F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, 0.05F))
                .reward(new EntityLootReward(EntityType.ZOMBIE, null, 15))
                .reward(new EntityLootReward(EntityType.SKELETON, null, 15))
                .reward(new EntityLootReward(EntityType.SPIDER, null, 10))
                .reward(new EntityLootReward(EntityType.CREEPER, null, 10))
                .reward(new EntityLootReward(EntityType.WITCH, null, 10)))
            .wave(w -> w.maxWaveTime(1600).setupTime(400)
                .entity(StandardWaveEntity.builder(EntityType.ZOMBIE).count(8).build())
                .entity(StandardWaveEntity.builder(EntityType.SKELETON).count(5).build())
                .entity(StandardWaveEntity.builder(EntityType.SPIDER).count(3).build())
                .entity(StandardWaveEntity.builder(EntityType.CREEPER).count(2).build())
                .entity(StandardWaveEntity.builder(EntityType.WITCH).count(1).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.15F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, 0.05F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_VALUE, 2.5F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.ARROW_DAMAGE, Operation.ADD_VALUE, 0.10F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.LIFE_STEAL, Operation.ADD_VALUE, 0.10F))
                .reward(new EntityLootReward(EntityType.ZOMBIE, null, 15))
                .reward(new EntityLootReward(EntityType.SKELETON, null, 15))
                .reward(new EntityLootReward(EntityType.SPIDER, null, 15))
                .reward(new EntityLootReward(EntityType.CREEPER, null, 15))
                .reward(new EntityLootReward(EntityType.WITCH, null, 25)))
            .wave(w -> w.maxWaveTime(2000).setupTime(400)
                .entity(StandardWaveEntity.builder(EntityType.ZOMBIE).desc("name.gateways.undead_legionnaire").nbt(t -> legionnaireNbt)
                    .addModifier(GearSetModifier.create(Gateways.loc("iron_with_diamond")))
                    .count(5).finalizeSpawn(false).build())
                .entity(StandardWaveEntity.builder(EntityType.SKELETON).count(3).build())
                .entity(StandardWaveEntity.builder(EntityType.CREEPER).count(2).build())
                .entity(StandardWaveEntity.builder(EntityType.WITCH).count(1).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.25F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.5F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 3F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_VALUE, 3F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.ARMOR_PIERCE, Operation.ADD_VALUE, 3F))
                .modifier(AttributeModifier.create(ALObjects.Attributes.LIFE_STEAL, Operation.ADD_VALUE, 0.10F))
                .reward(new EntityLootReward(EntityType.ZOMBIE, null, 20))
                .reward(new EntityLootReward(EntityType.SKELETON, null, 20))
                .reward(new EntityLootReward(EntityType.SPIDER, null, 20))
                .reward(new EntityLootReward(EntityType.CREEPER, null, 20))
                .reward(new EntityLootReward(EntityType.WITCH, null, 30)))
            .keyReward(LootTableReward.create(ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/simple_dungeon")), 10, "rewards.gateways.loot_table.simple_dungeon"))
            .failure(new MobEffectFailure(MobEffects.BLINDNESS, 400, 0))
            .failure(new SummonFailure(StandardWaveEntity.builder(EntityType.WITCH).count(2).build()))
            .rules(r -> r.allowDiscarding(true)));
    }

    private void genIronAutomaton() {
        CompoundTag ironSentinelNbt = namedNbt("name.gateways.iron_sentinel", "gray");
        CompoundTag ironJuggernautNbt = namedNbt("name.gateways.iron_juggernaut", "red");

        normalGateway("iron_automaton", b -> b
            .size(Gateway.Size.LARGE)
            .color(0xD8D8D8)
            // Wave 1: Simple iron golem encounter.
            .wave(w -> w.maxWaveTime(1200).setupTime(200)
                .entity(StandardWaveEntity.builder(EntityType.IRON_GOLEM).count(2).build())
                .reward(new StackReward(template(Items.IRON_INGOT, 16))))
            // Wave 2: More golems with increased health and armor.
            .wave(w -> w.maxWaveTime(1400).setupTime(300)
                .entity(StandardWaveEntity.builder(EntityType.IRON_GOLEM).count(3).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.20F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 4F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.10F))
                .reward(new StackReward(template(Items.IRON_INGOT, 32))))
            // Wave 3: Iron Sentinels with knockback resistance and armor pierce.
            .wave(w -> w.maxWaveTime(1600).setupTime(400)
                .entity(StandardWaveEntity.builder(EntityType.IRON_GOLEM)
                    .desc("name.gateways.iron_sentinel").nbt(t -> ironSentinelNbt)
                    .addModifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_VALUE, 20F))
                    .addModifier(AttributeModifier.create(ALObjects.Attributes.ARMOR_PIERCE, Operation.ADD_VALUE, 4F))
                    .count(2).finalizeSpawn(false).build())
                .entity(StandardWaveEntity.builder(EntityType.IRON_GOLEM).count(2).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.30F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 6F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.20F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.25F))
                .reward(new StackListReward(List.of(
                    template(Items.IRON_INGOT, 32),
                    template(Items.IRON_BLOCK, 4)))))
            // Wave 4: The Iron Juggernaut boss with a full squad.
            .wave(w -> w.maxWaveTime(2400).setupTime(400)
                .entity(StandardWaveEntity.builder(EntityType.IRON_GOLEM)
                    .desc("name.gateways.iron_juggernaut").nbt(t -> ironJuggernautNbt)
                    .addModifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_VALUE, 50F))
                    .addModifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_VALUE, 8F))
                    .addModifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 1.0F))
                    .addModifier(AttributeModifier.create(ALObjects.Attributes.ARMOR_SHRED, Operation.ADD_VALUE, 0.5F))
                    .addModifier(AttributeModifier.create(ALObjects.Attributes.LIFE_STEAL, Operation.ADD_VALUE, 0.15F))
                    .count(1).finalizeSpawn(false).build())
                .entity(StandardWaveEntity.builder(EntityType.IRON_GOLEM)
                    .desc("name.gateways.iron_sentinel").nbt(t -> ironSentinelNbt)
                    .addModifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_VALUE, 20F))
                    .addModifier(AttributeModifier.create(ALObjects.Attributes.ARMOR_PIERCE, Operation.ADD_VALUE, 4F))
                    .count(3).finalizeSpawn(false).build())
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.40F))
                .modifier(AttributeModifier.create(Attributes.ARMOR, Operation.ADD_VALUE, 8F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.30F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.5F))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, 0.10F))
                .reward(new StackListReward(List.of(
                    template(Items.IRON_BLOCK, 8),
                    template(Items.IRON_INGOT, 32)))))
            .keyReward(new StackListReward(List.of(
                template(Items.IRON_BLOCK, 48),
                template(Items.POPPY, 48),
                template(Items.ANVIL, 1))))
            .failure(new SummonFailure(StandardWaveEntity.builder(EntityType.IRON_GOLEM).count(2).build())));
    }

    private void genEndlessAutomatonFabricator() {
        endlessGateway("endless/automaton_fabricator", b -> b
            .size(Gateway.Size.LARGE).color(0xC8C8C8)
            // Base wave: 2 iron golems with a short timer. Yields 3-4 iron per golem (vanilla drop) plus 8 bonus ingots.
            // At ~30s per wave cycle (600 wave + 100 setup), that's roughly 480 iron/hour from rewards alone, plus natural drops.
            .baseWave(w -> w.maxWaveTime(600).setupTime(100)
                .entity(StandardWaveEntity.builder(EntityType.IRON_GOLEM).count(2).build())
                .reward(new StackReward(template(Items.IRON_INGOT, 12))))
            // Every 3 waves (up to 10 times): add 1 more golem and 4 more iron per wave.
            // By wave 30 this adds 10 golems and 40 iron per wave on top of the base.
            .modifier(m -> m.applicationMode(new AfterEveryNWaves(3, 10))
                .entity(StandardWaveEntity.builder(EntityType.IRON_GOLEM).count(1).build())
                .reward(new StackReward(template(Items.IRON_INGOT, 6)))
                .maxWaveTime(100))
            // Every 5 waves (up to 6 times): golems get tougher. Keeps the challenge scaling with the mob count.
            .modifier(m -> m.applicationMode(new AfterEveryNWaves(5, 6))
                .modifier(AttributeModifier.create(Attributes.MAX_HEALTH, Operation.ADD_MULTIPLIED_TOTAL, 0.10F))
                .modifier(AttributeModifier.create(Attributes.ATTACK_DAMAGE, Operation.ADD_MULTIPLIED_TOTAL, 0.10F)))
            // Every 10 waves (up to 3 times): add an iron block bonus and a speed bump.
            .modifier(m -> m.applicationMode(new AfterEveryNWaves(10, 3))
                .reward(new StackReward(template(Items.IRON_BLOCK, 3)))
                .modifier(AttributeModifier.create(Attributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_TOTAL, 0.05F))
                .modifier(AttributeModifier.create(Attributes.KNOCKBACK_RESISTANCE, Operation.ADD_VALUE, 0.15F)))
            .bossSettings(new BossEventSettings(BossEventSettings.Mode.NAME_PLATE, false))
            .spawnAlgo(SpawnAlgorithms.INWARD_SPIRAL));
    }

    private static CompoundTag sizeNbt(int size) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Size", size);
        return tag;
    }

    private static CompoundTag namedNbt(String translationKey, String color) {
        CompoundTag tag = new CompoundTag();
        tag.putByte("CustomNameVisible", (byte) 1);
        Component name = Component.translatable(translationKey).withStyle(Style.EMPTY.withColor(TextColor.parseColor(color).getOrThrow()));
        Tag nameTag = ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, name).getOrThrow();
        tag.put("CustomName", nameTag);
        return tag;
    }

    @SuppressWarnings("deprecation")
    private static ItemStackTemplate template(ItemLike item, int count) {
        return new ItemStackTemplate(item.asItem().builtInRegistryHolder(), count, DataComponentPatch.EMPTY);
    }

    @SuppressWarnings("deprecation")
    private static ItemStackTemplate witherSkeletonSpawnerTemplate() {
        CompoundTag spawnData = new CompoundTag();
        CompoundTag entityTag = new CompoundTag();
        entityTag.putString("id", "minecraft:wither_skeleton");
        spawnData.put("SpawnData", entityTag.copy());
        CompoundTag spawnDataWrapper = new CompoundTag();
        spawnDataWrapper.put("entity", entityTag);
        spawnData.put("SpawnData", spawnDataWrapper);

        TypedEntityData<BlockEntityType<?>> blockEntityData = TypedEntityData
            .of(BlockEntityType.MOB_SPAWNER, spawnData);

        DataComponentPatch patch = DataComponentPatch.builder()
            .set(DataComponents.BLOCK_ENTITY_DATA, blockEntityData)
            .set(DataComponents.CUSTOM_NAME, Component.literal("Wither Skeleton Spawner"))
            .build();

        return new ItemStackTemplate(Items.SPAWNER.builtInRegistryHolder(), 1, patch);
    }

    private void normalGateway(String path, UnaryOperator<NormalGateway.Builder> config) {
        this.add(Gateways.loc(path), config.apply(NormalGateway.builder()).build());
    }

    private void endlessGateway(String path, UnaryOperator<EndlessGateway.Builder> config) {
        this.add(Gateways.loc(path), config.apply(EndlessGateway.builder()).build());
    }
}
