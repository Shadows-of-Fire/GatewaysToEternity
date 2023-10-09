package dev.shadowsoffire.gateways.entity;

import com.google.common.base.Preconditions;

import dev.shadowsoffire.gateways.GatewayObjects;
import dev.shadowsoffire.gateways.gate.Wave;
import dev.shadowsoffire.gateways.gate.normal.NormalGateway;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class NormalGatewayEntity extends GatewayEntity {

    public NormalGatewayEntity(Level level, Player placer, DynamicHolder<NormalGateway> gate) {
        super(GatewayObjects.NORMAL_GATEWAY.get(), level, placer, gate);
        this.summonerId = placer.getUUID();
        this.gate = gate;
        Preconditions.checkArgument(gate.isBound(), "A gateway may not be constructed for an unbound holder.");
        this.setCustomName(Component.translatable(gate.getId().toString().replace(':', '.')).withStyle(Style.EMPTY.withColor(gate.get().color())));
        this.bossEvent = this.createBossEvent();
        this.refreshDimensions();
    }

    public NormalGatewayEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public boolean isLastWave() {
        return this.getWave() >= this.getGateway().getNumWaves();
    }

    @Override
    public Wave getCurrentWave() {
        return this.getGateway().getWave(Math.min(this.getGateway().getNumWaves() - 1, this.getWave()));
    }

    @Override
    protected boolean canStartNextWave() {
        return this.getTicksActive() > this.getCurrentWave().setupTime() && !this.isLastWave();
    }

    @Override
    public boolean isCompleted() {
        return this.undroppedItems.isEmpty() && this.isLastWave();
    }

    @Override
    protected void completeWave() {
        Player player = this.summonerOrClosest();
        this.undroppedItems.addAll(this.getCurrentWave().spawnRewards((ServerLevel) this.level(), this, player));
    }

    @Override
    protected void completeGateway() {
        super.completeGateway();
        Player player = this.summonerOrClosest();
        this.getGateway().rewards().forEach(r -> {
            r.generateLoot((ServerLevel) this.level(), this, player, this::spawnCompletionItem);
        });
    }

    @Override
    public NormalGateway getGateway() {
        return (NormalGateway) super.getGateway();
    }

}
