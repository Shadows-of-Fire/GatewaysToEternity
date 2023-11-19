package dev.shadowsoffire.gateways.entity;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.mojang.authlib.GameProfile;

import dev.shadowsoffire.gateways.GatewayObjects;
import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.client.ParticleHandler;
import dev.shadowsoffire.gateways.event.GateEvent;
import dev.shadowsoffire.gateways.gate.GateRules;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.gateways.gate.SpawnAlgorithms.SpawnAlgorithm;
import dev.shadowsoffire.gateways.gate.Wave;
import dev.shadowsoffire.gateways.gate.normal.NormalGateway;
import dev.shadowsoffire.gateways.net.ParticleMessage;
import dev.shadowsoffire.placebo.network.PacketDistro;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public abstract class GatewayEntity extends Entity implements IEntityAdditionalSpawnData {

    public static final EntityDataAccessor<Boolean> WAVE_ACTIVE = SynchedEntityData.defineId(GatewayEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> TICKS_ACTIVE = SynchedEntityData.defineId(GatewayEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> WAVE = SynchedEntityData.defineId(GatewayEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> ENEMIES = SynchedEntityData.defineId(GatewayEntity.class, EntityDataSerializers.INT);

    protected final Set<LivingEntity> currentWaveEntities = new HashSet<>();
    protected final Set<UUID> unresolvedWaveEntities = new HashSet<>();

    protected UUID summonerId;
    protected DynamicHolder<? extends Gateway> gate;
    protected float clientScale = 0F;
    protected Queue<ItemStack> undroppedItems = new ArrayDeque<>();
    protected FailureReason failureReason;

    @Nullable
    protected ServerBossEvent bossEvent;

    public GatewayEntity(EntityType<? extends GatewayEntity> type, Level level, Player placer, DynamicHolder<? extends Gateway> gate) {
        super(type, level);
        this.summonerId = placer.getUUID();
        this.gate = gate;
        Preconditions.checkArgument(gate.isBound(), "A gateway may not be constructed for an unbound holder.");
        this.setCustomName(Component.translatable(gate.getId().toString().replace(':', '.')).withStyle(Style.EMPTY.withColor(gate.get().color())));
        this.bossEvent = this.createBossEvent();
        this.refreshDimensions();
    }

    public GatewayEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /**
     * Returns the current wave, or the final wave, if the wave index is past the last wave.
     */
    public abstract Wave getCurrentWave();

    /**
     * Returns true if the next wave can begin execution.
     */
    protected boolean canStartNextWave() {
        return this.getTicksActive() > this.getSetupTime();
    }

    public abstract boolean isCompleted();

    /**
     * Called when a wave is completed. Responsible for loot spawns.
     */
    protected abstract void completeWave();

    /**
     * Returns the setup time of the current wave.
     */
    public int getSetupTime() {
        return getCurrentWave().setupTime();
    }

    /**
     * Returns the max wave time of the current wave.
     */
    public int getMaxWaveTime() {
        return getCurrentWave().maxWaveTime();
    }

    @Override
    public void tick() {
        if (!this.gate.isBound()) {
            this.remove(RemovalReason.DISCARDED);
            return;
        }

        super.tick();

        if (!this.level().isClientSide) {
            if (!this.unresolvedWaveEntities.isEmpty()) {
                for (UUID id : this.unresolvedWaveEntities) {
                    Entity e = ((ServerLevel) this.level()).getEntity(id);
                    if (e instanceof LivingEntity) this.currentWaveEntities.add((LivingEntity) e);
                }
                this.unresolvedWaveEntities.clear();
            }

            if (this.isWaveActive()) {
                int maxWaveTime = this.getMaxWaveTime();
                if (this.getTicksActive() > maxWaveTime) {
                    this.onFailure(this.currentWaveEntities, FailureReason.TIMER_ELAPSED);
                    return;
                }
            }

            this.entityData.set(TICKS_ACTIVE, this.getTicksActive() + 1);

            // Collect all remaining enemies, which are those that are alive and not removed via a valid reason.
            List<LivingEntity> enemies = this.currentWaveEntities.stream().filter(e -> e.getHealth() > 0 && !this.isValidRemoval(e.getRemovalReason())).toList();
            if (this.tickCount % 20 == 0) {
                for (LivingEntity entity : enemies) {
                    if (hasLeftDimension(entity)) {
                        this.onFailure(this.currentWaveEntities, FailureReason.ENTITY_LEFT_DIMENSION);
                        return;
                    }
                    if (entity.getRemovalReason() == RemovalReason.DISCARDED) {
                        this.onFailure(this.currentWaveEntities, FailureReason.ENTITY_DISCARDED);
                        return;
                    }
                    if (entity.tickCount > 30) {
                        this.spawnParticle(entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), ParticleMessage.Type.IDLE);
                    }
                    if (this.isOutOfRange(entity)) {
                        if (this.getGateway().rules().failOnOutOfBounds() || !this.respawnEntity(entity)) {
                            this.onFailure(this.currentWaveEntities, FailureReason.ENTITY_TOO_FAR);
                            return;
                        }
                    }
                }
            }
            this.entityData.set(ENEMIES, enemies.size());

            if (this.tickCount % 4 == 0 && !this.undroppedItems.isEmpty()) {
                for (int i = 0; i < this.getDropCount(); i++) {
                    this.spawnItem(this.undroppedItems.remove());
                    if (this.undroppedItems.isEmpty()) break;
                }
            }

            if (this.isWaveActive()) {
                if (enemies.isEmpty()) {
                    this.completeWave();
                    MinecraftForge.EVENT_BUS.post(new GateEvent.WaveEnd(this));
                    this.currentWaveEntities.clear();
                    this.entityData.set(WAVE_ACTIVE, false);
                    this.entityData.set(TICKS_ACTIVE, 0);
                    this.entityData.set(WAVE, this.getWave() + 1);
                }
            }
            else {
                if (this.canStartNextWave()) {
                    this.startNextWave();
                    this.entityData.set(WAVE_ACTIVE, true);
                    this.entityData.set(TICKS_ACTIVE, 0);
                    this.entityData.set(ENEMIES, this.currentWaveEntities.size());
                    MinecraftForge.EVENT_BUS.post(new GateEvent.WaveStarted(this));
                    return;
                }
                else if (this.isCompleted()) {
                    completeGateway();
                }
            }
        }
        else {
            if (this.tickCount % 30 == 0) {
                ParticleHandler.spawnIdleParticles(this);
            }
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pPose) {
        return this.gate.get().size().getDims();
    }

    /**
     * Checks if the removal reason of the entity was a "legal" kill for this gateway.
     */
    protected boolean isValidRemoval(@Nullable RemovalReason reason) {
        GateRules rules = this.getGateway().rules();
        return reason == RemovalReason.KILLED || (rules.allowDiscarding() && reason == RemovalReason.DISCARDED) || (rules.allowDimChange() && reason == RemovalReason.CHANGED_DIMENSION);
    }

    protected int getDropCount() {
        return 3 + this.undroppedItems.size() / 100;
    }

    /**
     * Spawns the next wave of entities. The current wave counter has already been incremented, so {@link #getCurrentWave()} is the wave being spawned.
     */
    protected void startNextWave() {
        List<LivingEntity> spawned = this.getCurrentWave().spawnWave((ServerLevel) this.level(), this.position(), this);
        this.currentWaveEntities.addAll(spawned);
    }

    /**
     * Called when the final wave is completed and the portal should close.
     */
    protected void completeGateway() {
        this.remove(RemovalReason.KILLED);
        this.playSound(GatewayObjects.GATE_END.get(), 16, 1);

        this.level().getNearbyPlayers(TargetingConditions.DEFAULT, null, this.getBoundingBox().inflate(15)).forEach(p -> p.awardStat(GatewayObjects.GATES_DEFEATED.get()));
        MinecraftForge.EVENT_BUS.post(new GateEvent.Completed(this));
    }

    public void onGateCreated() {
        this.playSound(GatewayObjects.GATE_START.get(), 1, 1);
        MinecraftForge.EVENT_BUS.post(new GateEvent.Opened(this));
    }

    public Player summonerOrClosest() {
        Player player = this.summonerId == null ? null : this.level().getPlayerByUUID(this.summonerId);
        if (player == null) {
            player = this.level().getNearestPlayer(this, 50);
        }
        if (player == null) {
            return this.summonerId == null ? FakePlayerFactory.getMinecraft((ServerLevel) this.level()) : FakePlayerFactory.get((ServerLevel) this.level(), new GameProfile(this.summonerId, ""));
        }
        return player;
    }

    /**
     * Called when a player fails to complete a wave in time, closing the gateway.
     */
    public void onFailure(Collection<LivingEntity> remaining, FailureReason reason) {
        this.failureReason = reason;
        MinecraftForge.EVENT_BUS.post(new GateEvent.Failed(this));
        Player player = this.summonerOrClosest();
        if (player != null) player.sendSystemMessage(reason.getMsg());
        spawnLightningOn(this, false);
        remaining.stream().filter(Entity::isAlive).forEach(e -> {
            if (this.getGateway().rules().removeOnFailure()) {
                spawnLightningOn(e, true);
                e.remove(RemovalReason.DISCARDED);
            }
            else if (e instanceof Mob mob) {
                mob.persistenceRequired = false;
            }
        });
        this.getGateway().failures().forEach(f -> f.onFailure((ServerLevel) this.level(), this, player, reason));
        this.remove(RemovalReason.DISCARDED);
    }

    protected ServerBossEvent createBossEvent() {
        if (this.getGateway().bossSettings().drawAsBar()) {
            ServerBossEvent event = new ServerBossEvent(Component.literal("GATEWAY_ID" + this.getId()), BossBarColor.BLUE, BossBarOverlay.PROGRESS);
            event.setCreateWorldFog(this.getGateway().bossSettings().fog());
            return event;
        }
        return null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("wave", this.getWave());
        tag.putString("gate", this.gate.getId().toString());
        long[] ids = new long[this.currentWaveEntities.size() * 2];
        int idx = 0;
        for (LivingEntity e : this.currentWaveEntities) {
            UUID id = e.getUUID();
            ids[idx++] = id.getMostSignificantBits();
            ids[idx++] = id.getLeastSignificantBits();
        }
        tag.putLongArray("wave_entities", ids);
        tag.putBoolean("active", this.isWaveActive());
        tag.putInt("ticks_active", this.getTicksActive());
        if (this.summonerId != null) tag.putUUID("summoner", this.summonerId);
        ListTag stacks = new ListTag();
        for (ItemStack s : this.undroppedItems) {
            stacks.add(s.serializeNBT());
        }
        tag.put("queued_stacks", stacks);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("wave")) this.entityData.set(WAVE, tag.getInt("wave"));
        if (tag.contains("gate")) this.gate = GatewayRegistry.INSTANCE.holder(new ResourceLocation(tag.getString("gate")));

        if (!this.gate.isBound()) {
            Gateways.LOGGER.error("Invalid gateway at {} will be removed.", this.position());
            this.remove(RemovalReason.DISCARDED);
            return;
        }

        if (tag.contains("wave_entities")) {
            this.currentWaveEntities.clear();
            this.unresolvedWaveEntities.clear();
            long[] entities = tag.getLongArray("wave_entities");
            for (int i = 0; i < entities.length; i += 2) {
                this.unresolvedWaveEntities.add(new UUID(entities[i], entities[i + 1]));
            }
        }

        if (tag.contains("active")) this.entityData.set(WAVE_ACTIVE, tag.getBoolean("active"));
        if (tag.contains("ticks_active")) this.entityData.set(TICKS_ACTIVE, tag.getInt("ticks_active"));
        if (tag.contains("summoner")) this.summonerId = tag.getUUID("summoner");
        if (tag.contains("queued_stacks")) {
            this.undroppedItems.clear();
            ListTag stacks = tag.getList("queued_stacks", Tag.TAG_COMPOUND);
            for (Tag inbt : stacks) {
                this.undroppedItems.add(ItemStack.of((CompoundTag) inbt));
            }
        }
        this.bossEvent = this.createBossEvent();
        this.refreshDimensions();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(WAVE_ACTIVE, false);
        this.entityData.define(TICKS_ACTIVE, 0);
        this.entityData.define(WAVE, 0);
        this.entityData.define(ENEMIES, 0);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (this.bossEvent != null) {
            this.bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (this.bossEvent != null) {
            this.bossEvent.removePlayer(player);
        }
    }

    public int getTicksActive() {
        return this.entityData.get(TICKS_ACTIVE);
    }

    public boolean isWaveActive() {
        return this.entityData.get(WAVE_ACTIVE);
    }

    public int getWave() {
        return this.entityData.get(WAVE);
    }

    public int getActiveEnemies() {
        return this.entityData.get(ENEMIES);
    }

    public Gateway getGateway() {
        return this.gate.get();
    }

    public boolean isValid() {
        return this.gate.isBound();
    }

    @Nullable
    public ServerBossEvent getBossEvent() {
        return this.bossEvent;
    }

    public float getClientScale() {
        return this.clientScale;
    }

    public void setClientScale(float clientScale) {
        this.clientScale = clientScale;
    }

    public void spawnParticle(double x, double y, double z, ParticleMessage.Type type) {
        PacketDistro.sendToTracking(Gateways.CHANNEL, new ParticleMessage(this, x, y, z, this.getGateway().color(), type), (ServerLevel) this.level(), new BlockPos((int) x, (int) y, (int) z));
    }

    public void spawnItem(ItemStack stack) {
        ItemEntity i = new ItemEntity(this.level(), 0, 0, 0, stack);
        i.setPos(this.getX() + Mth.nextDouble(this.random, -0.5, 0.5), this.getY() + 1.5, this.getZ() + Mth.nextDouble(this.random, -0.5, 0.5));
        i.setDeltaMovement(Mth.nextDouble(this.random, -0.15, 0.15), 0.4, Mth.nextDouble(this.random, -0.15, 0.15));
        this.level().addFreshEntity(i);
        this.level().playSound(null, i.getX(), i.getY(), i.getZ(), GatewayObjects.GATE_WARP.get(), SoundSource.HOSTILE, 0.25F, 2.0F);
    }

    public void spawnCompletionItem(ItemStack stack) {
        ItemEntity i = new ItemEntity(this.level(), 0, 0, 0, stack);
        double variance = 0.05F * this.getGateway().size().getScale();
        i.setPos(this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ());
        i.setDeltaMovement(Mth.nextDouble(this.random, -variance, variance), this.getBbHeight() / 20F, Mth.nextDouble(this.random, -variance, variance));
        i.setUnlimitedLifetime();
        this.level().addFreshEntity(i);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.gate.getId());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf) {
        this.gate = GatewayRegistry.INSTANCE.holder(buf.readResourceLocation());
        if (!this.gate.isBound()) throw new RuntimeException("Invalid gateway received on client!");
        this.refreshDimensions();
    }

    @Override
    protected int getPermissionLevel() {
        return 2;
    }

    /**
     * @return The failure reason, if the gate has failed, or null otherwise.
     */
    @Nullable
    public FailureReason getFailureReason() {
        return this.failureReason;
    }

    /**
     * Checks if the wave entity is outside the Gateway's {@linkplain NormalGateway#leashRange() leash range}.
     * <p>
     * If {@link NormalGateway#failOnOutOfBounds()} is enabled, being outside the leash range triggers {@link FailureReason#ENTITY_TOO_FAR}.<br>
     * Otherwise, this causes the entity to be re-placed near the gateway using the current spawn algorithm.
     * 
     * @param entity The wave entity.
     * @return If the entity is outside the leash range.
     */
    public boolean isOutOfRange(Entity entity) {
        return entity.distanceToSqr(this) > this.getGateway().getLeashRangeSq();
    }

    /**
     * Handles the conversion of entity into outcome.<br>
     * From the context of a gateway, this means all references must be updated to track the new entity.
     *
     * @param entity  The old entity, which is owned by this gateway.
     * @param outcome The new entity.
     */
    public void handleConversion(Entity entity, LivingEntity outcome) {
        entity.getPersistentData().remove("gateways.owner");
        outcome.getPersistentData().putUUID("gateways.owner", this.getUUID());

        if (this.unresolvedWaveEntities.contains(entity.getUUID())) {
            this.unresolvedWaveEntities.remove(entity.getUUID());
            this.unresolvedWaveEntities.add(outcome.getUUID());
        }
        else if (this.currentWaveEntities.contains(entity)) {
            this.currentWaveEntities.remove(entity);
            this.currentWaveEntities.add(outcome);
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    /**
     * Attempts to respawn an out-of-bounds wave entity using the current spawn algorithm.<br>
     * If the respawn attempt fails, the gate will fail with {@link FailureReason#ENTITY_TOO_FAR}.
     * 
     * @param entity The out-of-bounds wave entity that needs to be respawned.
     * @return True if the respawn succeeded.
     */
    public boolean respawnEntity(Entity entity) {
        SpawnAlgorithm algo = this.getGateway().spawnAlgo();
        Vec3 pos = algo.spawn((ServerLevel) this.level(), this.position(), this, entity);
        if (pos == null) return false;
        entity.resetFallDistance();
        this.spawnParticle(entity.getX(), entity.getY(), entity.getZ(), ParticleMessage.Type.SPAWNED);
        entity.setPos(pos);
        this.spawnParticle(entity.getX(), entity.getY(), entity.getZ(), ParticleMessage.Type.SPAWNED);
        if (entity instanceof Mob mob) {
            Player p = summonerOrClosest();
            if (!(p instanceof FakePlayer)) mob.setTarget(p);
        }
        return true;
    }

    public static void spawnLightningOn(Entity entity, boolean effectOnly) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(entity.level());
        bolt.setPos(entity.getX(), entity.getY(), entity.getZ());
        bolt.setVisualOnly(effectOnly);
        entity.level().addFreshEntity(bolt);
    }

    /**
     * Checks if the wave entity has left the current dimension. This triggers an automatic {@link FailureReason#ENTITY_LEFT_DIMENSION}.
     * 
     * @param entity The wave entity.
     * @return True if the entity has changed dimensions.
     */
    public static boolean hasLeftDimension(Entity entity) {
        return entity.getRemovalReason() == RemovalReason.CHANGED_DIMENSION;
    }

    @Nullable
    public static GatewayEntity getOwner(Entity entity) {
        if (entity.getPersistentData().contains("gateways.owner")) {
            UUID id = entity.getPersistentData().getUUID("gateways.owner");
            if (entity.level() instanceof ServerLevel sl && sl.getEntity(id) instanceof GatewayEntity gate && gate.isValid()) {
                return gate;
            }
        }
        return null;
    }

    public static enum FailureReason {
        SPAWN_FAILED("error.gateways.wave_failed"),
        ENTITY_TOO_FAR("error.gateways.too_far"),
        TIMER_ELAPSED("error.gateways.wave_elapsed"),
        ENTITY_DISCARDED("error.gateways.entity_discarded"),
        ENTITY_LEFT_DIMENSION("error.gateways.left_dimension");

        private final String langKey;

        FailureReason(String langKey) {
            this.langKey = langKey;
        }

        public Component getMsg() {
            return Component.translatable(this.langKey).withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE);
        }
    }

}
