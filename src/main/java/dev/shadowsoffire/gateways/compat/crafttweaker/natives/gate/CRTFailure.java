package dev.shadowsoffire.gateways.compat.crafttweaker.natives.gate;

import java.util.function.Consumer;

import org.openzen.zencode.java.ZenCodeType;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import com.blamejared.crafttweaker_annotations.annotations.NativeTypeRegistration;

import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.Failure;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

// TODO Maybe expose all failure types? not an issue, just tedious
@ZenRegister
@Document("mods/Gateways/gate/Failure")
@NativeTypeRegistration(value = Failure.class, zenCodeName = "mods.gateways.gate.Failure")
public class CRTFailure {

    @ZenCodeType.Method
    public static void onFailure(Failure internal, ServerLevel level, GatewayEntity gate, Player summoner, GatewayEntity.FailureReason reason) {
        internal.onFailure(level, gate, summoner, reason);
    }

    @ZenCodeType.Method
    public static void appendHoverText(Failure internal, Consumer<Component> list) {
        internal.appendHoverText(list);
    }
}
