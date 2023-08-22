package dev.shadowsoffire.gateways.compat.crafttweaker.natives.event;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.event.ZenEvent;
import com.blamejared.crafttweaker.api.event.bus.ForgeEventBusWire;
import com.blamejared.crafttweaker.api.event.bus.IEventBus;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import com.blamejared.crafttweaker_annotations.annotations.NativeTypeRegistration;
import dev.shadowsoffire.gateways.event.GateEvent;

@ZenRegister
@ZenEvent
@Document("mods/Gateways/event/GateFailedEvent")
@NativeTypeRegistration(value = GateEvent.Failed.class, zenCodeName = "mods.gateways.events.GateFailedEvent")
public class CRTGateFailedEvent {

    @ZenEvent.Bus
    public static final IEventBus<GateEvent.Failed> BUS = IEventBus.direct(
            GateEvent.Failed.class,
            ForgeEventBusWire.of()
    );

}
