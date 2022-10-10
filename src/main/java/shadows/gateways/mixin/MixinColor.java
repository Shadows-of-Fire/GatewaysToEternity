package shadows.gateways.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.util.text.Color;

@Mixin(Color.class)
public interface MixinColor {

	@Accessor
	public int getValue();
}