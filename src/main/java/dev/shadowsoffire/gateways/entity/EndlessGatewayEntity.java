package dev.shadowsoffire.gateways.entity;

import java.util.function.Consumer;

import dev.shadowsoffire.gateways.GatewayObjects;
import dev.shadowsoffire.gateways.gate.Wave;
import dev.shadowsoffire.gateways.gate.endless.EndlessGateway;
import dev.shadowsoffire.gateways.gate.endless.EndlessModifier;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class EndlessGatewayEntity extends GatewayEntity {

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
        return true; // No requirements for starting the next wave beyond the implicit "is the check being called"
    }

    @Override
    protected void startNextWave() {
        super.startNextWave();
        executeModifiers(m -> m.entities().forEach(waveEntity -> {
            LivingEntity entity = Wave.spawnWaveEntity((ServerLevel) this.level(), this.position(), this, getCurrentWave(), waveEntity);
            if (entity == null) this.onFailure(this.currentWaveEntities, FailureReason.SPAWN_FAILED);
            this.currentWaveEntities.add(entity);
        }));
        executeModifiers(m -> m.modifiers().forEach(modif -> this.currentWaveEntities.forEach(modif::apply)));
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
    }

    @Override
    public EndlessGateway getGateway() {
        return (EndlessGateway) super.getGateway();
    }

    /**
     * Executes all endless modifiers based on the number of applications they would have at the current wave.
     * 
     * @param func
     */
    public void executeModifiers(Consumer<EndlessModifier> func) {
        this.getGateway().modifiers().forEach(m -> {
            int count = m.appMode().getApplicationCount(this.getWave() + 1);
            for (int i = 0; i < count; i++) {
                func.accept(m);
            }
        });
    }
}
