package dev.shadowsoffire.gateways.entity;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import dev.shadowsoffire.gateways.GatewayObjects;
import dev.shadowsoffire.gateways.gate.Wave;
import dev.shadowsoffire.gateways.gate.WaveEntity;
import dev.shadowsoffire.gateways.gate.WaveModifier;
import dev.shadowsoffire.gateways.gate.endless.EndlessGateway;
import dev.shadowsoffire.gateways.gate.endless.EndlessModifier;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class EndlessGatewayEntity extends GatewayEntity {

    public static final EntityDataAccessor<Integer> MAX_ENEMIES = SynchedEntityData.defineId(EndlessGatewayEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> MODIFIERS = SynchedEntityData.defineId(EndlessGatewayEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> INCOMING_ENEMIES = SynchedEntityData.defineId(EndlessGatewayEntity.class, EntityDataSerializers.INT);

    public EndlessGatewayEntity(Level level, Player placer, DynamicHolder<EndlessGateway> gate) {
        super(GatewayObjects.ENDLESS_GATEWAY.get(), level, placer, gate);
    }

    public EndlessGatewayEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public Wave getCurrentWave() {
        return this.getGateway().baseWave();
    }

    @Override
    protected boolean canStartNextWave() {
        return this.getTicksActive() > this.getCurrentWave().setupTime();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isRemoved()) return;
        if (this.getMaxEnemies() == -1) {
            this.computeMaxEnemies(this.getWave());
        }
    }

    @Override
    protected void startNextWave() {
        super.startNextWave();
        executeModifiers(m -> m.entities().forEach(waveEntity -> {
            for (int i = 0; i < waveEntity.getCount(); i++) {
                LivingEntity entity = Wave.spawnWaveEntity((ServerLevel) this.level(), this.position(), this, getCurrentWave(), waveEntity);
                if (entity == null) {
                    this.onFailure(this.currentWaveEntities, FailureReason.SPAWN_FAILED);
                    break;
                }
                else this.currentWaveEntities.add(entity);
            }
        }));
        int applied = executeModifiers(m -> {
            for (LivingEntity entity : this.currentWaveEntities) {
                for (WaveModifier waveModif : m.modifiers()) {
                    waveModif.apply(entity);
                }
                entity.setHealth(entity.getMaxHealth());
            }
        });
        this.entityData.set(MODIFIERS, applied);
        this.entityData.set(MAX_ENEMIES, this.currentWaveEntities.size());
    }

    @Override
    public boolean isCompleted() {
        return false; // Endless gateways are never completed, they continue until failure.
    }

    @Override
    protected void completeWave() {
        Player player = this.summonerOrClosest();
        this.undroppedItems.addAll(this.getCurrentWave().spawnRewards((ServerLevel) this.level(), this, player));
        executeModifiers(m -> m.rewards().forEach(r -> r.generateLoot((ServerLevel) this.level(), this, player, this.undroppedItems::add)));
        this.computeMaxEnemies(this.getWave() + 1); // Precompute max enemies for the next wave so it can be displayed.
    }

    @Override
    public EndlessGateway getGateway() {
        return (EndlessGateway) super.getGateway();
    }

    public int getMaxEnemies() {
        return this.entityData.get(MAX_ENEMIES);
    }

    public int getModifiersApplied() {
        return this.entityData.get(MODIFIERS);
    }

    /**
     * Executes all endless modifiers based on the number of applications they would have at the current wave.
     * 
     * @param wave The current wave.
     * @param func An operation to perform on the modifiers.
     * @return The total number of modifiers applied, counting re-applications.
     */
    public int executeModifiers(int wave, Consumer<EndlessModifier> func) {
        int totalApplications = 0;
        for (EndlessModifier m : this.getGateway().modifiers()) {
            int count = m.appMode().getApplicationCount(wave + 1);
            for (int i = 0; i < count; i++) {
                func.accept(m);
            }
            totalApplications += count;
        }
        return totalApplications;
    }

    public int executeModifiers(Consumer<EndlessModifier> func) {
        return this.executeModifiers(this.getWave(), func);
    }

    protected void computeMaxEnemies(int wave) {
        AtomicInteger count = new AtomicInteger(this.getGateway().baseWave().entities().stream().mapToInt(WaveEntity::getCount).sum());
        executeModifiers(wave, modif -> {
            count.set(count.get() + modif.entities().stream().mapToInt(WaveEntity::getCount).sum());
        });
        this.entityData.set(MAX_ENEMIES, count.get());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MAX_ENEMIES, -1);
        this.entityData.define(MODIFIERS, -1);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("max_enemies", this.getMaxEnemies());
        tag.putInt("modifiers_applied", this.getModifiersApplied());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("max_enemies")) {
            this.entityData.set(MAX_ENEMIES, tag.getInt("max_enemies"));
        }
        if (tag.contains("modifiers_applied")) {
            this.entityData.set(MODIFIERS, tag.getInt("modifiers_applied"));
        }
    }
}
