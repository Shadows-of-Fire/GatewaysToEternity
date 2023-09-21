package dev.shadowsoffire.gateways.compat.crafttweaker.natives.gate;

import java.util.List;

import org.openzen.zencode.java.ZenCodeType;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import com.blamejared.crafttweaker_annotations.annotations.NativeTypeRegistration;

import dev.shadowsoffire.gateways.gate.Failure;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.gateways.gate.Wave;
import dev.shadowsoffire.gateways.gate.normal.NormalGateway;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

@ZenRegister
@Document("mods/Gateways/gate/Gateway")
@NativeTypeRegistration(value = NormalGateway.class, zenCodeName = "mods.gateways.gate.Gateway")
public class CRTGateway {

    @ZenCodeType.Getter("color")
    public static TextColor getColor(NormalGateway internal) {
        return internal.color();
    }

    @ZenCodeType.Getter("waves")
    public static List<Wave> getWaves(NormalGateway internal) {
        return internal.waves();
    }

    @ZenCodeType.Getter("numWaves")
    public static int getNumWaves(NormalGateway internal) {
        return internal.getNumWaves();
    }

    @ZenCodeType.Method
    public static Wave getWave(NormalGateway internal, int n) {
        return internal.getWave(n);
    }

    @ZenCodeType.Getter("failures")
    public static List<Failure> getFailures(NormalGateway internal) {
        return internal.failures();
    }

    @ZenCodeType.Getter("spawnRange")
    public static double getSpawnRange(NormalGateway internal) {
        return internal.rules().spawnRange();
    }

    @ZenCodeType.Getter("leashRangeSq")
    public static double getLeashRangeSq(NormalGateway internal) {
        return internal.getLeashRangeSq();
    }

    @ZenCodeType.Getter("playerDamageOnly")
    public static boolean playerDamageOnly(NormalGateway internal) {
        return internal.rules().playerDamageOnly();
    }

    @ZenCodeType.Getter("allowsDiscarding")
    public static boolean allowsDiscarding(NormalGateway internal) {
        return internal.rules().allowDiscarding();
    }

    @ZenCodeType.Getter("removeMobsOnFailure")
    public static boolean removeMobsOnFailure(NormalGateway internal) {
        return internal.rules().removeOnFailure();
    }

    @ZenCodeType.Getter("id")
    public static ResourceLocation getId(NormalGateway internal) {
        return GatewayRegistry.INSTANCE.getKey(internal);
    }
}
