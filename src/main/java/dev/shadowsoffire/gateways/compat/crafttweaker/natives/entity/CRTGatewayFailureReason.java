package dev.shadowsoffire.gateways.compat.crafttweaker.natives.entity;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker_annotations.annotations.BracketEnum;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import com.blamejared.crafttweaker_annotations.annotations.NativeTypeRegistration;
import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import net.minecraft.network.chat.Component;

@ZenRegister
@Document("mods/Gateways/entity/GatewayFailureReason")
@NativeTypeRegistration(value = GatewayEntity.FailureReason.class, zenCodeName = "mods.gateways.entity.GatewayFailureReason")
@BracketEnum(Gateways.MODID + ":failure_reason")
public class CRTGatewayFailureReason {

    public static Component getMsg(GatewayEntity.FailureReason internal) {
        return internal.getMsg();
    }
}
