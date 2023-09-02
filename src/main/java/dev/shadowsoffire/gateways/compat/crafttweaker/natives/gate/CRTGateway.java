package dev.shadowsoffire.gateways.compat.crafttweaker.natives.gate;

import java.util.List;

import org.openzen.zencode.java.ZenCodeType;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import com.blamejared.crafttweaker_annotations.annotations.NativeTypeRegistration;

import dev.shadowsoffire.gateways.gate.Failure;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.gateways.gate.Reward;
import dev.shadowsoffire.gateways.gate.Wave;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity.RemovalReason;

@ZenRegister
@Document("mods/Gateways/gate/Gateway")
@NativeTypeRegistration(value = Gateway.class, zenCodeName = "mods.gateways.gate.Gateway")
public class CRTGateway {

    @ZenCodeType.Getter("color")
    public static TextColor getColor(Gateway internal) {
        return internal.color();
    }

    @ZenCodeType.Getter("waves")
    public static List<Wave> getWaves(Gateway internal) {
        return internal.waves();
    }

    @ZenCodeType.Getter("numWaves")
    public static int getNumWaves(Gateway internal) {
        return internal.getNumWaves();
    }

    @ZenCodeType.Method
    public static Wave getWave(Gateway internal, int n) {
        return internal.getWave(n);
    }

    @ZenCodeType.Getter("rewards")
    public static List<Reward> getRewards(Gateway internal) {
        return internal.rewards();
    }

    @ZenCodeType.Getter("failures")
    public static List<Failure> getFailures(Gateway internal) {
        return internal.failures();
    }

    @ZenCodeType.Getter("spawnRange")
    public static double getSpawnRange(Gateway internal) {
        return internal.rules().spawnRange();
    }

    @ZenCodeType.Getter("leashRangeSq")
    public static double getLeashRangeSq(Gateway internal) {
        return internal.getLeashRangeSq();
    }

    @ZenCodeType.Getter("playerDamageOnly")
    public static boolean playerDamageOnly(Gateway internal) {
        return internal.rules().playerDamageOnly();
    }

    @ZenCodeType.Getter("allowsDiscarding")
    public static boolean allowsDiscarding(Gateway internal) {
        return internal.rules().validRemovals().contains(RemovalReason.DISCARDED);
    }

    @ZenCodeType.Getter("removeMobsOnFailure")
    public static boolean removeMobsOnFailure(Gateway internal) {
        return internal.rules().removeOnFailure();
    }

    @ZenCodeType.Getter("id")
    public static ResourceLocation getId(Gateway internal) {
        return GatewayRegistry.INSTANCE.getKey(internal);
    }
}
