package dev.shadowsoffire.gateways.gate;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.placebo.codec.CodecMap;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.json.NBTAdapter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;

/**
 * A Reward is provided when a gateway wave is finished.
 */
public interface Reward extends CodecProvider<Reward> {

    public static final CodecMap<Reward> CODEC = new CodecMap<>("Gateway Reward");

    public static final Method dropFromLootTable = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "dropFromLootTable", ServerLevel.class, DamageSource.class, boolean.class);
    public static final MethodHandle DROP_LOOT = lootMethodHandle();

    /**
     * Called when this reward is to be granted to the player, either on gate or wave completion.
     *
     * @param level    The level the gateway is in
     * @param gate     The gateway entity
     * @param summoner The summoning player
     * @param list     When generating item rewards, add them to this list instead of directly to the player or the world.
     */
    public void generateLoot(ServerLevel level, GatewayEntity gate, Player summoner, Consumer<ItemStack> list);

    public void appendHoverText(TooltipContext ctx, Consumer<MutableComponent> list);

    public static void initCodecs() {
        register("stack", StackReward.CODEC);
        register("stack_list", StackListReward.CODEC);
        register("entity_loot", EntityLootReward.CODEC);
        register("loot_table", LootTableReward.CODEC);
        register("chanced", ChancedReward.CODEC);
        register("command", CommandReward.CODEC);
        register("experience", ExperienceReward.CODEC);
        register("summon", SummonReward.CODEC);
        register("counted", CountedReward.CODEC);
    }

    private static void register(String id, Codec<? extends Reward> codec) {
        CODEC.register(Gateways.loc(id), codec);
    }

    private static MethodHandle lootMethodHandle() {
        dropFromLootTable.setAccessible(true);
        try {
            return MethodHandles.lookup().unreflect(dropFromLootTable);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Provides a single stack as a reward.
     */
    public static record StackReward(ItemStackTemplate stack, Optional<String> desc) implements Reward {

        public static Codec<StackReward> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                ItemStackTemplate.CODEC.fieldOf("stack").forGetter(StackReward::stack),
                Codec.STRING.optionalFieldOf("desc").forGetter(StackReward::desc))
            .apply(inst, StackReward::new));

        public StackReward(ItemStackTemplate stack) {
            this(stack, Optional.empty());
        }

        public StackReward(ItemStackTemplate stack, String desc) {
            this(stack, Optional.of(desc));
        }

        @Override
        public void generateLoot(ServerLevel level, GatewayEntity gate, Player summoner, Consumer<ItemStack> list) {
            list.accept(this.stack.create());
        }

        @Override
        public void appendHoverText(TooltipContext ctx, Consumer<MutableComponent> list) {
            ItemStack resolved = this.stack.create();
            Component name = this.desc.<Component>map(Component::translatable).orElse(resolved.getHoverName());
            list.accept(Gateways.lang("tooltip", "with_count", resolved.getCount(), name));
        }

        @Override
        public Codec<? extends Reward> getCodec() {
            return CODEC;
        }
    }

    /**
     * Provides a list of stacks as a reward.
     */
    public static record StackListReward(List<ItemStackTemplate> stacks) implements Reward {

        public static Codec<StackListReward> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                ItemStackTemplate.CODEC.listOf().fieldOf("stacks").forGetter(StackListReward::stacks))
            .apply(inst, StackListReward::new));

        @Override
        public void generateLoot(ServerLevel level, GatewayEntity gate, Player summoner, Consumer<ItemStack> list) {
            this.stacks.forEach(s -> list.accept(s.create()));
        }

        @Override
        public void appendHoverText(TooltipContext ctx, Consumer<MutableComponent> list) {
            for (ItemStackTemplate stack : this.stacks) {
                ItemStack resolved = stack.create();
                list.accept(Gateways.lang("tooltip", "with_count", resolved.getCount(), resolved.getHoverName()));
            }
        }

        @Override
        public Codec<? extends Reward> getCodec() {
            return CODEC;
        }
    }

    /**
     * Provides multiple rolls of an entity's loot table as a reward.
     */
    public static record EntityLootReward(EntityType<?> type, @Nullable CompoundTag nbt, int rolls) implements Reward {

        public static Codec<EntityLootReward> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(EntityLootReward::type),
                NBTAdapter.EITHER_CODEC.optionalFieldOf("nbt").forGetter(r -> Optional.ofNullable(r.nbt)),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("rolls", 1).forGetter(EntityLootReward::rolls))
            .apply(inst, (type, nbt, rolls) -> new EntityLootReward(type, nbt.orElse(null), rolls)));

        @Override
        public void generateLoot(ServerLevel level, GatewayEntity gate, Player summoner, Consumer<ItemStack> list) {
            try {
                List<ItemEntity> items = new ArrayList<>();

                CompoundTag data = this.nbt != null ? this.nbt.copy() : new CompoundTag();
                data.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(this.type).toString());
                Entity entity = EntityType.loadEntityRecursive(data, level, EntitySpawnReason.SPAWNER, EntityProcessor.NOP);
                if (entity == null) return;
                entity.getPersistentData().putBoolean("apoth.no_pinata", true);
                for (int i = 0; i < this.rolls; i++) {
                    entity.snapTo(summoner.getX(), summoner.getY(), summoner.getZ(), 0, 0);
                    DamageSource src = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.GENERIC_KILL), summoner);
                    entity.hurtServer(level, src, 1);
                    entity.captureDrops(items);
                    DROP_LOOT.invoke(entity, level, level.damageSources().playerAttack(summoner), true);
                }
                entity.remove(RemovalReason.DISCARDED);

                items.stream().map(ItemEntity::getItem).forEach(list);
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }

        @Override
        public void appendHoverText(TooltipContext ctx, Consumer<MutableComponent> list) {
            list.accept(Component.translatable("reward.gateways.entity", this.rolls, Component.translatable(this.type.getDescriptionId())));
        }

        @Override
        public Codec<? extends Reward> getCodec() {
            return CODEC;
        }
    }

    /**
     * Provides a roll of a single loot table as a reward.
     */
    public static record LootTableReward(Identifier table, int rolls, String desc) implements Reward {

        public static Codec<LootTableReward> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                Identifier.CODEC.fieldOf("loot_table").forGetter(LootTableReward::table),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("rolls", 1).forGetter(LootTableReward::rolls),
                Codec.STRING.fieldOf("desc").forGetter(LootTableReward::desc))
            .apply(inst, LootTableReward::new));

        @Override
        public void generateLoot(ServerLevel level, GatewayEntity gate, Player summoner, Consumer<ItemStack> list) {
            LootTable realTable = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, this.table));
            for (int i = 0; i < this.rolls; i++) {
                LootParams.Builder ctx = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, gate.getPosition(1));
                ctx.withLuck(summoner.getLuck()).withParameter(LootContextParams.THIS_ENTITY, summoner).withParameter(LootContextParams.TOOL, summoner.getMainHandItem());
                realTable.getRandomItems(ctx.create(LootContextParamSets.CHEST)).forEach(list);
            }
        }

        @Override
        public void appendHoverText(TooltipContext ctx, Consumer<MutableComponent> list) {
            list.accept(Component.translatable("reward.gateways.loot_table", this.rolls, this.desc.isEmpty() ? this.table : Component.translatable(this.desc)));
        }

        @Override
        public Codec<? extends Reward> getCodec() {
            return CODEC;
        }

        public static LootTableReward create(ResourceKey<LootTable> table, int rolls, String desc) {
            return new LootTableReward(table.identifier(), rolls, desc);
        }
    }

    /**
     * Wraps a reward with a random chance applied to it.
     */
    public static record ChancedReward(Reward reward, float chance) implements Reward {

        public static Codec<ChancedReward> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                Reward.CODEC.fieldOf("reward").forGetter(ChancedReward::reward),
                Codec.FLOAT.fieldOf("chance").forGetter(ChancedReward::chance))
            .apply(inst, ChancedReward::new));

        protected static final DecimalFormat fmt = new DecimalFormat("##.##%");

        @Override
        public void generateLoot(ServerLevel level, GatewayEntity gate, Player summoner, Consumer<ItemStack> list) {
            if (level.getRandom().nextFloat() < this.chance) this.reward.generateLoot(level, gate, summoner, list);
        }

        @Override
        public void appendHoverText(TooltipContext ctx, Consumer<MutableComponent> list) {
            this.reward.appendHoverText(ctx, c -> {
                list.accept(Component.translatable("reward.gateways.chance", fmt.format(this.chance), c));
            });
        }

        @Override
        public Codec<? extends Reward> getCodec() {
            return CODEC;
        }
    }

    /**
     * Provides a roll of a single loot table as a reward.
     */
    public static record CommandReward(String command, String desc) implements Reward {

        public static Codec<CommandReward> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                Codec.STRING.fieldOf("command").forGetter(CommandReward::command),
                Codec.STRING.fieldOf("desc").forGetter(CommandReward::desc))
            .apply(inst, CommandReward::new));

        @Override
        public void generateLoot(ServerLevel level, GatewayEntity gate, Player summoner, Consumer<ItemStack> list) {
            String realCmd = this.command.replace("<summoner>", summoner.getGameProfile().name());
            level.getServer().getCommands().performPrefixedCommand(gate.createCommandSourceStackForNameResolution((ServerLevel) gate.level()), realCmd);
        }

        @Override
        public void appendHoverText(TooltipContext ctx, Consumer<MutableComponent> list) {
            list.accept(Component.translatable(this.desc));
        }

        @Override
        public Codec<? extends Reward> getCodec() {
            return CODEC;
        }
    }

    /**
     * Provides a certain amount of XP as a reward.
     */
    public static record ExperienceReward(int xp, int orbSize) implements Reward {

        public static Codec<ExperienceReward> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                Codec.INT.fieldOf("experience").forGetter(ExperienceReward::xp),
                Codec.INT.optionalFieldOf("orb_size", 5).forGetter(ExperienceReward::orbSize))
            .apply(inst, ExperienceReward::new));

        @Override
        public void generateLoot(ServerLevel level, GatewayEntity gate, Player summoner, Consumer<ItemStack> list) {
            int remaining = this.xp;
            while (remaining > 0) {
                remaining -= orbSize;
                level.addFreshEntity(new ExperienceOrb(level, gate.getX(), gate.getY(), gate.getZ(), orbSize));
            }
        }

        @Override
        public void appendHoverText(TooltipContext ctx, Consumer<MutableComponent> list) {
            list.accept(Component.translatable("reward.gateways.experience", this.xp));
        }

        @Override
        public Codec<? extends Reward> getCodec() {
            return CODEC;
        }
    }

    /**
     * Summons a {@link WaveEntity} as a reward.
     */
    public static record SummonReward(WaveEntity entity) implements Reward {

        public static Codec<SummonReward> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                WaveEntity.CODEC.fieldOf("entity").forGetter(SummonReward::entity))
            .apply(inst, SummonReward::new));

        @Override
        public void generateLoot(ServerLevel level, GatewayEntity gate, Player summoner, Consumer<ItemStack> list) {
            for (int i = 0; i < entity.getCount(); i++) {
                Entity ent = entity.createEntity(level, gate);
                if (ent != null) {
                    Vec3 pos = gate.getGateway().spawnAlgo().spawn(level, gate.position(), gate, ent);
                    ent.setPos(pos != null ? pos : gate.position());
                    level.addFreshEntity(ent);
                }
            }
        }

        @Override
        public void appendHoverText(TooltipContext ctx, Consumer<MutableComponent> list) {
            list.accept(Component.translatable("reward.gateways.summon", this.entity.getDescription()));
        }

        @Override
        public Codec<? extends Reward> getCodec() {
            return CODEC;
        }
    }

    /**
     * Rolls the same reward multiple times.
     */
    public static record CountedReward(Reward reward, int count) implements Reward {

        public static Codec<CountedReward> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                Reward.CODEC.fieldOf("reward").forGetter(CountedReward::reward),
                Codec.intRange(1, 1024).fieldOf("count").forGetter(CountedReward::count))
            .apply(inst, CountedReward::new));

        @Override
        public void generateLoot(ServerLevel level, GatewayEntity gate, Player summoner, Consumer<ItemStack> list) {
            for (int i = 0; i < this.count; i++) {
                this.reward.generateLoot(level, gate, summoner, list);
            }
        }

        @Override
        public void appendHoverText(TooltipContext ctx, Consumer<MutableComponent> list) {
            this.reward.appendHoverText(ctx, c -> {
                list.accept(Gateways.lang("tooltip", "with_count", this.count, c));
            });
        }

        @Override
        public Codec<? extends Reward> getCodec() {
            return CODEC;
        }
    }
}
