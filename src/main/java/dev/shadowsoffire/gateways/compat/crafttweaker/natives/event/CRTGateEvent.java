package dev.shadowsoffire.gateways.compat.crafttweaker.natives.event;

import org.openzen.zencode.java.ZenCodeType;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import com.blamejared.crafttweaker_annotations.annotations.NativeTypeRegistration;

import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.event.GateEvent;

@ZenRegister
@Document("mods/Gateways/event/GateEvent")
@NativeTypeRegistration(value = GateEvent.class, zenCodeName = "mods.gateways.events.GateEvent")
public class CRTGateEvent {

    @ZenCodeType.Getter("entity")
    public static GatewayEntity getEntity(GateEvent internal) {
        return internal.getEntity();
    }
}
