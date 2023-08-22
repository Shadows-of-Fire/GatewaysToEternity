package dev.shadowsoffire.gateways.compat.crafttweaker.natives.gate;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.item.IItemStack;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import com.blamejared.crafttweaker_annotations.annotations.NativeTypeRegistration;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.Reward;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.openzen.zencode.java.ZenCodeType;

import java.util.function.Consumer;

// TODO Maybe expose all reward types? not an issue, just tedious
@ZenRegister
@Document("mods/Gateways/gate/Reward")
@NativeTypeRegistration(value = Reward.class, zenCodeName = "mods.gateways.gate.Reward")
public class CRTReward {

    @ZenCodeType.Method
    public static void generateLoot(Reward internal, ServerLevel level, GatewayEntity gate, Player summoner, Consumer<IItemStack> list) {
        internal.generateLoot(level, gate, summoner, stack -> list.accept(IItemStack.of(stack)));
    }

    @ZenCodeType.Method
    public static void appendHoverText(Reward internal, Consumer<Component> list) {
        internal.appendHoverText(list);
    }
}
