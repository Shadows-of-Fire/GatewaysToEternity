package dev.shadowsoffire.gateways.compat.crafttweaker.natives.event;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.event.ZenEvent;
import com.blamejared.crafttweaker.api.event.bus.ForgeEventBusWire;
import com.blamejared.crafttweaker.api.event.bus.IEventBus;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import com.blamejared.crafttweaker_annotations.annotations.NativeTypeRegistration;
import dev.shadowsoffire.gateways.event.GateEvent;
import net.minecraft.world.entity.LivingEntity;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenEvent
@Document("mods/Gateways/event/GateWaveEntitySpawnedEvent")
@NativeTypeRegistration(value = GateEvent.WaveEntitySpawned.class, zenCodeName = "mods.gateways.events.GateWaveEntitySpawnedEvent")
public class CRTGateWaveEntitySpawnedEvent {

    @ZenEvent.Bus
    public static final IEventBus<GateEvent.WaveEntitySpawned> BUS = IEventBus.direct(
            GateEvent.WaveEntitySpawned.class,
            ForgeEventBusWire.of()
    );

    @ZenCodeType.Getter("waveEntity")
    public static LivingEntity getWaveEntity(final GateEvent.WaveEntitySpawned internal) {
        return internal.getWaveEntity();
    }

}
