package dev.shadowsoffire.gateways.entity;

import java.lang.ref.WeakReference;
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
import dev.shadowsoffire.gateways.client.GatewayTickableSound;
import dev.shadowsoffire.gateways.client.ParticleHandler;
import dev.shadowsoffire.gateways.event.GateEvent;
import dev.shadowsoffire.gateways.gate.GateRules;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.gateways.gate.SpawnAlgorithms.SpawnAlgorithm;
import dev.shadowsoffire.gateways.gate.Wave;
import dev.shadowsoffire.gateways.gate.normal.NormalGateway;
import dev.shadowsoffire.gateways.payloads.ParticlePayload;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.network.PacketDistributor;

public abstract class GatewayEntity extends Entity implements IEntityWithComplexSpawn {

    public static final EntityDataAccessor<Boolean> WAVE_ACTIVE = SynchedEntityData.defineId(GatewayEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> TICKS_ACTIVE = SynchedEntityData.defineId(GatewayEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> WAVE = SynchedEntityData.defineId(GatewayEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> ENEMIES = SynchedEntityData.defineId(GatewayEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> HAS_NEARBY_PLAYER = SynchedEntityData.defineId(GatewayEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> LIVES_REMAINING = SynchedEntityData.defineId(GatewayEntity.class, EntityDataSerializers.INT);

    protected final Set<LivingEntity> currentWaveEntities = new HashSet<>();
    protected final Set<UUID> unresolvedWaveEntities = new HashSet<>();

    protected UUID summonerId = UUID.fromString("41C82C87-7AfB-4024-BA57-13D2C99CAE77"); // Uses the FakePlayerFactory's default UUID as a fallback.
    protected DynamicHolder<Gateway> gate;
    protected float clientScale = 0F;
    protected Queue<ItemStack> undroppedItems = new ArrayDeque<>();
    protected FailureReason failureReason;

    /**
     * Remaining ticks before the gateway will fail due to a lack of nearby players.
     * <p>
     * This counter ticks down if {@link #HAS_NEARBY_PLAYER} is false, and resets to 200 if a player returns.
     */
    protected int nearbyPlayerTimer = 200;

    @Nullable
    protected ServerBossEvent bossEvent;

    protected transient WeakReference<Player> knownPlayer;

    public GatewayEntity(EntityType<? extends GatewayEntity> type, Level level, Player placer, DynamicHolder<Gateway> gate) {
        super(type, level);
        this.summonerId = placer.getUUID();
        this.gate = gate;
        Preconditions.checkArgument(gate.isBound(), "A gateway may not be constructed for an unbound holder.");
        this.setCustomName(Component.translatable(gate.getId().toString().replace(':', '.')).withStyle(Style.EMPTY.withColor(gate.get().color())));
        this.bossEvent = this.createBossEvent();
        this.refreshDimensions();
        this.knownPlayer = new WeakReference<>(placer);
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

        if (!this.level().isClientSide()) {
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

            // If we don't have a nearby player, tick down the counter, and if it reaches zero, fail the gateway.
            if (!this.entityData.get(HAS_NEARBY_PLAYER)) {
                this.nearbyPlayerTimer--;
                if (this.nearbyPlayerTimer <= 0) {
                    this.onFailure(this.currentWaveEntities, FailureReason.NO_NEARBY_PLAYER);
                    return;
                }
            }
            else {
                this.nearbyPlayerTimer = 200; // Reset the timer if we have a nearby player.
            }

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
                        this.spawnParticle(entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), ParticlePayload.EffectType.IDLE);
                    }
                    if (this.isOutOfRange(entity)) {
                        if (this.getGateway().rules().failOnOutOfBounds() || !this.respawnEntity(entity)) {
                            this.onFailure(this.currentWaveEntities, FailureReason.ENTITY_TOO_FAR);
                            return;
                        }
                    }

                    if (this.tickCount % 100 == 0 && entity instanceof Mob mob) {
                        if (!(mob.getTarget() instanceof Player)) {
                            Player p = summonerOrClosest();
                            if (!(p instanceof FakePlayer)) {
                                mob.setTarget(p);
                            }
                        }
                    }
                }

                Player player = this.level().getNearestPlayer(this, this.getGateway().getLeashRangeSq());
                this.entityData.set(HAS_NEARBY_PLAYER, player != null);
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
                    NeoForge.EVENT_BUS.post(new GateEvent.WaveEnd(this));
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
                    NeoForge.EVENT_BUS.post(new GateEvent.WaveStarted(this));
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
        this.playSound(GatewayObjects.GATE_END.value(), 16, 1);

        AABB completionBB = this.getBoundingBox().inflate(10 + this.getGateway().rules().leashRange());
        this.level().getEntitiesOfClass(Player.class, completionBB).forEach(p -> {
            p.awardStat(GatewayObjects.GATES_DEFEATED);
            if (p instanceof ServerPlayer sp) {
                GatewayObjects.FINISH_GATEWAY.trigger(sp, this.getGateway());
            }
        });
        NeoForge.EVENT_BUS.post(new GateEvent.Completed(this));
    }

    public void onGateCreated() {
        this.playSound(GatewayObjects.GATE_START.value(), 1, 1);
        NeoForge.EVENT_BUS.post(new GateEvent.Opened(this));
    }

    /**
     * Attempts to resolve the player-in-context for this gateway.
     * <p>
     * This method first prefers the original summoner, then the closest player within 50 blocks, and finally a fake player if no valid player is found.
     * <p>
     * The fake player, if used, will have the same UUID as the original summoner.
     */
    public Player summonerOrClosest() {
        if (this.knownPlayer != null) {
            Player player = this.knownPlayer.get();
            if (player != null && player.isAlive()) {
                return player;
            }
            else {
                this.knownPlayer = null;
            }
        }

        Player player = this.level().getPlayerByUUID(this.summonerId);
        if (player == null) {
            player = this.level().getNearestPlayer(this, 50);
        }

        if (player == null) {
            GameProfile profile = new GameProfile(this.summonerId, "Gateway_Summoner");
            return FakePlayerFactory.get((ServerLevel) this.level(), profile);
        }

        this.knownPlayer = new WeakReference<>(player);
        return player;
    }

    /**
     * Called when a player fails to complete a wave in time, closing the gateway.
     */
    public void onFailure(Collection<LivingEntity> remaining, FailureReason reason) {
        this.failureReason = reason;
        NeoForge.EVENT_BUS.post(new GateEvent.Failed(this));
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

    /**
     * Called when a player is killed within 100 blocks of the gateway.
     * <p>
     * If the player is within the leash range (+ 25 blocks) of the gateway, the gateway will lose a life.
     */
    public void playerDied(Player player) {
        if (this.getGateway().rules().lives() != -1 && this.distanceToSqr(player) <= Mth.square(this.getGateway().rules().leashRange() + 25)) {
            int lives = this.getRemainingLives() - 1;
            if (lives <= 0) {
                this.onFailure(this.currentWaveEntities, FailureReason.OUT_OF_LIVES);
            }
            else {
                this.setRemainingLives(lives);
            }
        }
    }

    protected ServerBossEvent createBossEvent() {
        if (this.getGateway().bossSettings().drawAsBar()) {
            ServerBossEvent event = new ServerBossEvent(this.getUUID(), Component.literal("GATEWAY_ID" + this.getId()), BossBarColor.BLUE, BossBarOverlay.PROGRESS);
            event.setCreateWorldFog(this.getGateway().bossSettings().fog());
            return event;
        }
        return null;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("wave", this.getWave());
        output.putString("gate", this.gate.getId().toString());
        java.util.List<UUID> waveEntityIds = new java.util.ArrayList<>();
        for (LivingEntity e : this.currentWaveEntities) {
            waveEntityIds.add(e.getUUID());
        }
        output.store("wave_entities", UUIDUtil.CODEC.listOf(), waveEntityIds);
        output.putBoolean("active", this.isWaveActive());
        output.putInt("ticks_active", this.getTicksActive());
        output.store("summoner", UUIDUtil.CODEC, this.summonerId);
        output.store("queued_stacks", ItemStack.CODEC.listOf(), new java.util.ArrayList<>(this.undroppedItems));
        output.putInt("lives", this.getRemainingLives());
        output.putInt("nearby_player_timer", this.nearbyPlayerTimer);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(WAVE, input.getIntOr("wave", 0));
        input.getString("gate").ifPresent(gateId -> this.gate = GatewayRegistry.INSTANCE.holder(Identifier.tryParse(gateId)));

        if (!this.gate.isBound()) {
            Gateways.LOGGER.error("Invalid gateway at {} will be removed.", this.position());
            this.remove(RemovalReason.DISCARDED);
            return;
        }

        input.read("wave_entities", UUIDUtil.CODEC.listOf()).ifPresent(uuids -> {
            this.currentWaveEntities.clear();
            this.unresolvedWaveEntities.clear();
            this.unresolvedWaveEntities.addAll(uuids);
        });

        this.entityData.set(WAVE_ACTIVE, input.getBooleanOr("active", false));
        this.entityData.set(TICKS_ACTIVE, input.getIntOr("ticks_active", 0));
        input.read("summoner", UUIDUtil.CODEC).ifPresent(id -> this.summonerId = id);
        input.read("queued_stacks", ItemStack.CODEC.listOf()).ifPresent(stacks -> {
            this.undroppedItems.clear();
            stacks.stream().filter(s -> !s.isEmpty()).forEach(this.undroppedItems::add);
        });
        this.setRemainingLives(input.getIntOr("lives", -1));
        this.nearbyPlayerTimer = input.getIntOr("nearby_player_timer", 0);

        this.bossEvent = this.createBossEvent();
        this.refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(WAVE_ACTIVE, false);
        builder.define(TICKS_ACTIVE, 0);
        builder.define(WAVE, 0);
        builder.define(ENEMIES, 0);
        builder.define(HAS_NEARBY_PLAYER, true);
        builder.define(LIVES_REMAINING, -1);
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

    @Override
    public CommandSourceStack createCommandSourceStackForNameResolution(ServerLevel level) {
        return new CommandSourceStack(
            level.getServer(),
            this.position(),
            this.getRotationVector(),
            level,
            PermissionSet.ALL_PERMISSIONS,
            this.getPlainTextName(),
            this.getDisplayName(),
            level.getServer(),
            this);
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

    /**
     * Returns the number of lives remaining for this gateway.
     * <p>
     * If the synced data value is -1, the lives have not been initialized, and the gateway returns the number of lives defined by the {@link GateRules}.
     */
    public int getRemainingLives() {
        int lives = this.entityData.get(LIVES_REMAINING);
        if (lives == -1) {
            return this.getGateway().rules().lives();
        }
        else {
            return lives;
        }
    }

    public void setRemainingLives(int lives) {
        this.entityData.set(LIVES_REMAINING, lives);
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

    public void spawnParticle(double x, double y, double z, ParticlePayload.EffectType type) {
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) this.level(), this.chunkPosition(), new ParticlePayload(this, x, y, z, this.getGateway().color(), type));
    }

    public void spawnItem(ItemStack stack) {
        ItemEntity i = new ItemEntity(this.level(), 0, 0, 0, stack);
        i.setPos(this.getX() + Mth.nextDouble(this.random, -0.5, 0.5), this.getY() + 1.5, this.getZ() + Mth.nextDouble(this.random, -0.5, 0.5));
        i.setDeltaMovement(Mth.nextDouble(this.random, -0.15, 0.15), 0.4, Mth.nextDouble(this.random, -0.15, 0.15));
        this.level().addFreshEntity(i);
        this.level().playSound(null, i.getX(), i.getY(), i.getZ(), GatewayObjects.GATE_WARP, SoundSource.HOSTILE, 0.25F, 2.0F);
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
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeIdentifier(this.gate.getId());
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        this.gate = GatewayRegistry.INSTANCE.holder(buf.readIdentifier());
        if (!this.gate.isBound()) throw new RuntimeException("Invalid gateway received on client!");
        this.refreshDimensions();
        GatewayTickableSound.startGatewaySound(this);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
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
        outcome.getPersistentData().store("gateways.owner", UUIDUtil.CODEC, this.getUUID());

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
    public boolean canBeCollidedWith(@Nullable Entity other) {
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
        this.spawnParticle(entity.getX(), entity.getY(), entity.getZ(), ParticlePayload.EffectType.SPAWNED);
        entity.setPos(pos);
        this.spawnParticle(entity.getX(), entity.getY(), entity.getZ(), ParticlePayload.EffectType.SPAWNED);
        if (entity instanceof Mob mob) {
            Player p = summonerOrClosest();
            if (!(p instanceof FakePlayer)) mob.setTarget(p);
        }
        return true;
    }

    public static void spawnLightningOn(Entity entity, boolean effectOnly) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(entity.level(), EntitySpawnReason.TRIGGERED);
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
        return entity.getPersistentData().read("gateways.owner", UUIDUtil.CODEC).map(id -> {
            if (entity.level() instanceof ServerLevel sl && sl.getEntity(id) instanceof GatewayEntity gate && gate.isValid()) {
                return gate;
            }
            return null;
        }).orElse(null);
    }

    public static enum FailureReason {
        SPAWN_FAILED("error.gateways.wave_failed"),
        ENTITY_TOO_FAR("error.gateways.too_far"),
        TIMER_ELAPSED("error.gateways.wave_elapsed"),
        ENTITY_DISCARDED("error.gateways.entity_discarded"),
        ENTITY_LEFT_DIMENSION("error.gateways.left_dimension"),
        NO_NEARBY_PLAYER("error.gateways.no_nearby_player"),
        OUT_OF_LIVES("error.gateways.out_of_lives");

        private final String langKey;

        FailureReason(String langKey) {
            this.langKey = langKey;
        }

        public Component getMsg() {
            return Component.translatable(this.langKey).withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE);
        }
    }

}
