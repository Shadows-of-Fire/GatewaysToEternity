package dev.shadowsoffire.gateways.compat.crafttweaker.natives.gate;

import java.util.List;

import org.openzen.zencode.java.ZenCodeType;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.item.IItemStack;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import com.blamejared.crafttweaker_annotations.annotations.NativeTypeRegistration;

import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.Reward;
import dev.shadowsoffire.gateways.gate.Wave;
import dev.shadowsoffire.gateways.gate.WaveEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@ZenRegister
@Document("mods/Gateways/gate/Wave")
@NativeTypeRegistration(value = Wave.class, zenCodeName = "mods.gateways.gate.Wave")
public class CRTWave {

    @ZenCodeType.Getter("entities")
    public static List<WaveEntity> entities(Wave internal) {
        return internal.entities();
    }

    @ZenCodeType.Getter("rewards")
    public static List<Reward> rewards(Wave internal) {
        return internal.rewards();
    }

    @ZenCodeType.Getter("maxWaveTime")
    public static int maxWaveTime(Wave internal) {
        return internal.maxWaveTime();
    }

    @ZenCodeType.Getter("setupTime")
    public static int setupTime(Wave internal) {
        return internal.setupTime();
    }

    @ZenCodeType.Method
    public static List<LivingEntity> spawnWave(Wave internal, ServerLevel level, Vec3 pos, GatewayEntity gate) {
        return internal.spawnWave(level, pos, gate);
    }

    @ZenCodeType.Method
    public static List<IItemStack> spawnRewards(Wave internal, ServerLevel level, GatewayEntity gate, Player summoner) {
        return internal.spawnRewards(level, gate, summoner).stream().map(IItemStack::of).toList();
    }
}
