package dev.shadowsoffire.gateways.compat.crafttweaker.natives.entity;

import org.openzen.zencode.java.ZenCodeType;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.item.IItemStack;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import com.blamejared.crafttweaker_annotations.annotations.NativeTypeRegistration;

import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.Wave;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

@ZenRegister
@Document("mods/Gateways/entity/GatewayEntity")
@NativeTypeRegistration(value = GatewayEntity.class, zenCodeName = "mods.gateways.entity.GatewayEntity")
public class CRTGatewayEntity {

    @ZenCodeType.Getter("isLastWave")
    public static boolean isLastWave(GatewayEntity internal) {
        return internal.isLastWave();
    }

    @ZenCodeType.Getter("currentWave")
    public static Wave getCurrentWave(GatewayEntity internal) {
        return internal.getCurrentWave();
    }

    @ZenCodeType.Method
    public static void spawnWave(GatewayEntity internal) {
        internal.spawnWave();
    }

    @ZenCodeType.Method
    public static void onGateCreated(GatewayEntity internal) {
        internal.onGateCreated();
    }

    @ZenCodeType.Getter("summonerOrClosest")
    public static Player summonerOrClosest(GatewayEntity internal) {
        return internal.summonerOrClosest();
    }

    @ZenCodeType.Getter("ticksActive")
    public static int getTicksActive(GatewayEntity internal) {
        return internal.getTicksActive();
    }

    @ZenCodeType.Getter("isWaveActive")
    public static boolean isWaveActive(GatewayEntity internal) {
        return internal.isWaveActive();
    }

    @ZenCodeType.Getter("getWave")
    public static int getWave(GatewayEntity internal) {
        return internal.getWave();
    }

    @ZenCodeType.Getter("activeEnemies")
    public static int getActiveEnemies(GatewayEntity internal) {
        return internal.getActiveEnemies();
    }

    @ZenCodeType.Getter("gateway")
    public static Gateway getGateway(GatewayEntity internal) {
        return internal.getGateway();
    }

    @ZenCodeType.Getter("isValid")
    public static boolean isValid(GatewayEntity internal) {
        return internal.isValid();
    }

    @ZenCodeType.Getter("clientScale")
    public static float getClientScale(GatewayEntity internal) {
        return internal.getClientScale();
    }

    @ZenCodeType.Method
    public static void spawnItem(GatewayEntity internal, IItemStack stack) {
        internal.spawnItem(stack.getInternal());
    }

    @ZenCodeType.Getter("failureReason")
    public static GatewayEntity.@ZenCodeType.Nullable FailureReason getFailureReason(GatewayEntity internal) {
        return internal.getFailureReason();
    }

    @ZenCodeType.Method
    public static boolean isOutOfRange(GatewayEntity internal, Entity entity) {
        return internal.isOutOfRange(entity);
    }

}
