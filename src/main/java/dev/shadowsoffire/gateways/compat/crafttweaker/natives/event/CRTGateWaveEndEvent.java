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
@Document("mods/Gateways/event/GateWaveEndEvent")
@NativeTypeRegistration(value = GateEvent.WaveEnd.class, zenCodeName = "mods.gateways.events.GateWaveEndEvent")
public class CRTGateWaveEndEvent {

    @ZenEvent.Bus
    public static final IEventBus<GateEvent.WaveEnd> BUS = IEventBus.direct(
            GateEvent.WaveEnd.class,
            ForgeEventBusWire.of()
    );

}
