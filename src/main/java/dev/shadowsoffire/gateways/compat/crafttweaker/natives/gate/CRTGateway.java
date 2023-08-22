package dev.shadowsoffire.gateways.compat.crafttweaker.natives.gate;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import com.blamejared.crafttweaker_annotations.annotations.NativeTypeRegistration;
import dev.shadowsoffire.gateways.gate.Failure;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.Reward;
import dev.shadowsoffire.gateways.gate.Wave;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import org.openzen.zencode.java.ZenCodeType;

import java.util.List;

@ZenRegister
@Document("mods/Gateways/gate/Gateway")
@NativeTypeRegistration(value = Gateway.class, zenCodeName = "mods.gateways.gate.Gateway")
public class CRTGateway {


    @ZenCodeType.Getter("color")
    public static TextColor getColor(Gateway internal) {
        return internal.getColor();
    }

    @ZenCodeType.Getter("waves")
    public static List<Wave> getWaves(Gateway internal) {
        return internal.getWaves();
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
        return internal.getRewards();
    }

    @ZenCodeType.Getter("failures")
    public static List<Failure> getFailures(Gateway internal) {
        return internal.getFailures();
    }

    @ZenCodeType.Getter("completionXp")
    public static int getCompletionXp(Gateway internal) {
        return internal.getCompletionXp();
    }

    @ZenCodeType.Getter("spawnRange")
    public static double getSpawnRange(Gateway internal) {
        return internal.getSpawnRange();
    }

    @ZenCodeType.Getter("leashRangeSq")
    public static double getLeashRangeSq(Gateway internal) {
        return internal.getLeashRangeSq();
    }

    @ZenCodeType.Getter("playerDamageOnly")
    public static boolean playerDamageOnly(Gateway internal) {
        return internal.playerDamageOnly();
    }

    @ZenCodeType.Getter("allowsDiscarding")
    public static boolean allowsDiscarding(Gateway internal) {
        return internal.allowsDiscarding();
    }

    @ZenCodeType.Getter("removeMobsOnFailure")
    public static boolean removeMobsOnFailure(Gateway internal) {
        return internal.removeMobsOnFailure();
    }

    @ZenCodeType.Getter("id")
    public static ResourceLocation getId(Gateway internal) {
        return internal.getId();
    }
}
