package dev.shadowsoffire.gateways.compat.crafttweaker.natives.gate;

import org.openzen.zencode.java.ZenCodeType;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import com.blamejared.crafttweaker_annotations.annotations.NativeTypeRegistration;

import dev.shadowsoffire.gateways.gate.WaveEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

@ZenRegister
@Document("mods/Gateways/gate/WaveEntity")
@NativeTypeRegistration(value = WaveEntity.class, zenCodeName = "mods.gateways.gate.WaveEntity")
public class CRTWaveEntity {

    @ZenCodeType.Method
    public static LivingEntity createEntity(WaveEntity internal, Level level) {
        return internal.createEntity(level);
    }

    @ZenCodeType.Getter("description")
    public static Component getDescription(WaveEntity internal) {
        return internal.getDescription();
    }

    @ZenCodeType.Getter("shouldFinalizeSpawn")
    public static boolean shouldFinalizeSpawn(WaveEntity internal) {
        return internal.shouldFinalizeSpawn();
    }
}
