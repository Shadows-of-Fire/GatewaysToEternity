package dev.shadowsoffire.gateways;

import java.util.UUID;

import dev.shadowsoffire.gateways.command.GatewayCommand;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent.AllowDespawn;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class GatewayEvents {

    public void commands(RegisterCommandsEvent e) {
        GatewayCommand.register(e.getDispatcher());
    }

    @SubscribeEvent
    public void teleport(EntityTeleportEvent e) {
        Entity entity = e.getEntity();
        if (entity.getPersistentData().contains("gateways.owner")) {
            UUID id = entity.getPersistentData().getUUID("gateways.owner");
            if (entity.level() instanceof ServerLevel sl && sl.getEntity(id) instanceof GatewayEntity gate && gate.isValid()) {
                if (gate.distanceToSqr(e.getTargetX(), e.getTargetY(), e.getTargetZ()) >= gate.getGateway().getLeashRangeSq()) {
                    e.setTargetX(gate.getX() + 0.5 * gate.getBbWidth());
                    e.setTargetY(gate.getY() + 0.5 * gate.getBbHeight());
                    e.setTargetZ(gate.getZ() + 0.5 * gate.getBbWidth());
                }
            }
        }
    }

    @SubscribeEvent
    public void convert(LivingConversionEvent.Post e) {
        Entity entity = e.getEntity();
        GatewayEntity gate = GatewayEntity.getOwner(entity);
        if (gate != null) {
            gate.handleConversion(entity, e.getOutcome());
        }
    }

    @SubscribeEvent
    public void hurt(LivingHurtEvent e) {
        GatewayEntity gate = GatewayEntity.getOwner(e.getEntity());
        if (gate != null) {
            boolean isPlayerDamage = e.getSource().getEntity() instanceof Player p && !(p instanceof FakePlayer);
            if (!isPlayerDamage && gate.getGateway().playerDamageOnly()) e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void despawn(AllowDespawn e) {
        if (GatewayEntity.getOwner(e.getEntity()) != null) e.setResult(Result.DENY);
    }

}
