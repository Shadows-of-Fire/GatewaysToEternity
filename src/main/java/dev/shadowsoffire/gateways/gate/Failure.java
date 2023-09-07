package dev.shadowsoffire.gateways.gate;

import java.text.DecimalFormat;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.entity.GatewayEntity.FailureReason;
import dev.shadowsoffire.placebo.codec.CodecMap;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A Failure is a negative effect that triggers when a gateway errors for some reason.
 */
public interface Failure extends CodecProvider<Failure> {

    public static final CodecMap<Failure> CODEC = new CodecMap<>("Gateway Failure");

    /**
     * Called when this failure is to be applied.
     *
     * @param level    The level the gateway is in
     * @param gate     The gateway entity
     * @param summoner The summoning player
     * @param reason   The reason the failure happened
     */
    public void onFailure(ServerLevel level, GatewayEntity gate, Player summoner, FailureReason reason);

    public void appendHoverText(Consumer<MutableComponent> list);

    public static void initSerializers() {
        register("explosion", ExplosionFailure.CODEC);
        register("mob_effect", MobEffectFailure.CODEC);
        register("summon", SummonFailure.CODEC);
        register("chanced", ChancedFailure.CODEC);
        register("command", CommandFailure.CODEC);
    }

    private static void register(String id, Codec<? extends Failure> codec) {
        CODEC.register(Gateways.loc(id), codec);
    }

    /**
     * Triggers an explosion on failure, with a specific strength, and optional fire/block damage.
     */
    public static record ExplosionFailure(float strength, boolean fire, boolean blockDamage) implements Failure {

        public static Codec<ExplosionFailure> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                Codec.FLOAT.fieldOf("strength").forGetter(ExplosionFailure::strength),
                Codec.BOOL.fieldOf("fire").forGetter(ExplosionFailure::fire),
                Codec.BOOL.fieldOf("block_damage").forGetter(ExplosionFailure::blockDamage))
            .apply(inst, ExplosionFailure::new));

        @Override
        public void onFailure(ServerLevel level, GatewayEntity gate, Player summoner, FailureReason reason) {
            level.explode(gate, gate.getX(), gate.getY(), gate.getZ(), this.strength, this.fire, this.blockDamage ? ExplosionInteraction.MOB : ExplosionInteraction.NONE);
        }

        @Override
        public Codec<? extends Failure> getCodec() {
            return CODEC;
        }

        @Override
        public void appendHoverText(Consumer<MutableComponent> list) {
            list.accept(Component.translatable("failure.gateways.explosion", this.strength, this.fire, this.blockDamage));
        }
    }

    /**
     * Applies a mob effect to all nearby players on failure.
     */
    public static record MobEffectFailure(MobEffect effect, int duration, int amplifier) implements Failure {

        public static Codec<MobEffectFailure> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                ForgeRegistries.MOB_EFFECTS.getCodec().fieldOf("effect").forGetter(MobEffectFailure::effect),
                Codec.INT.fieldOf("duration").forGetter(MobEffectFailure::duration),
                PlaceboCodecs.nullableField(Codec.INT, "amplifier", 0).forGetter(MobEffectFailure::amplifier))
            .apply(inst, MobEffectFailure::new));

        @Override
        public void onFailure(ServerLevel level, GatewayEntity gate, Player summoner, FailureReason reason) {
            level.getNearbyPlayers(TargetingConditions.forNonCombat(), null, gate.getBoundingBox().inflate(gate.getGateway().rules().leashRange())).forEach(p -> {
                p.addEffect(new MobEffectInstance(this.effect, this.duration, this.amplifier));
            });
        }

        @Override
        public Codec<? extends Failure> getCodec() {
            return CODEC;
        }

        @Override
        public void appendHoverText(Consumer<MutableComponent> list) {
            list.accept(Component.translatable("failure.gateways.mob_effect", toComponent(new MobEffectInstance(this.effect, this.duration, this.amplifier))));
        }

        private static Component toComponent(MobEffectInstance mobeffectinstance) {
            MutableComponent mutablecomponent = Component.translatable(mobeffectinstance.getDescriptionId());
            MobEffect mobeffect = mobeffectinstance.getEffect();

            if (mobeffectinstance.getAmplifier() > 0) {
                mutablecomponent = Component.translatable("potion.withAmplifier", mutablecomponent, Component.translatable("potion.potency." + mobeffectinstance.getAmplifier()));
            }

            if (mobeffectinstance.getDuration() > 20) {
                mutablecomponent = Component.translatable("potion.withDuration", mutablecomponent, MobEffectUtil.formatDuration(mobeffectinstance, 1));
            }

            return mutablecomponent.withStyle(mobeffect.getCategory().getTooltipFormatting());
        }
    }

    /**
     * Summons a {@link WaveEntity} as a failure penalty.
     */
    public static record SummonFailure(WaveEntity entity) implements Failure {

        public static Codec<SummonFailure> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                WaveEntity.CODEC.fieldOf("entity").forGetter(SummonFailure::entity))
            .apply(inst, SummonFailure::new));

        @Override
        public void onFailure(ServerLevel level, GatewayEntity gate, Player summoner, FailureReason reason) {
            for (int i = 0; i < entity.getCount(); i++) {
                Entity ent = entity.createEntity(level);
                if (ent != null) {
                    Vec3 pos = gate.getGateway().spawnAlgo().spawn(level, gate.position(), gate, ent);
                    ent.setPos(pos != null ? pos : gate.position());
                    level.addFreshEntity(ent);
                }
            }
        }

        @Override
        public void appendHoverText(Consumer<MutableComponent> list) {
            list.accept(Component.translatable("failure.gateways.summon", this.entity.getDescription()));
        }

        @Override
        public Codec<? extends Failure> getCodec() {
            return CODEC;
        }
    }

    /**
     * Wraps a failure with a random chance applied to it.
     */
    public static record ChancedFailure(Failure failure, float chance) implements Failure {

        public static Codec<ChancedFailure> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                Failure.CODEC.fieldOf("failure").forGetter(ChancedFailure::failure),
                Codec.FLOAT.fieldOf("chance").forGetter(ChancedFailure::chance))
            .apply(inst, ChancedFailure::new));

        @Override
        public void onFailure(ServerLevel level, GatewayEntity gate, Player summoner, FailureReason reason) {
            if (level.random.nextFloat() < this.chance) this.failure.onFailure(level, gate, summoner, reason);
        }

        @Override
        public Codec<? extends Failure> getCodec() {
            return CODEC;
        }

        static DecimalFormat fmt = new DecimalFormat("##.##%");

        @Override
        public void appendHoverText(Consumer<MutableComponent> list) {
            this.failure.appendHoverText(c -> {
                list.accept(Component.translatable("failure.gateways.chance", fmt.format(this.chance * 100), c));
            });
        }
    }

    /**
     * Executes a command on Gateway failure.
     */
    public static record CommandFailure(String command, String desc) implements Failure {

        public static Codec<CommandFailure> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                Codec.STRING.fieldOf("command").forGetter(CommandFailure::command),
                Codec.STRING.fieldOf("desc").forGetter(CommandFailure::desc))
            .apply(inst, CommandFailure::new));

        @Override
        public void onFailure(ServerLevel level, GatewayEntity gate, Player summoner, FailureReason reason) {
            String realCmd = this.command.replace("<summoner>", summoner.getGameProfile().getName());
            level.getServer().getCommands().performPrefixedCommand(gate.createCommandSourceStack(), realCmd);
        }

        @Override
        public Codec<? extends Failure> getCodec() {
            return CODEC;
        }

        @Override
        public void appendHoverText(Consumer<MutableComponent> list) {
            list.accept(Component.translatable(this.desc));
        }
    }

}
