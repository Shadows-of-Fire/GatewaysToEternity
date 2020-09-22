package shadows.gateways.util;

import java.util.EnumMap;

import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfo.Color;

public class BossColorMap {

	public static final EnumMap<BossInfo.Color, Integer> COLOR_MAP = new EnumMap<>(BossInfo.Color.class);

	static {
		COLOR_MAP.put(Color.PINK, 0xB90090);
		COLOR_MAP.put(Color.BLUE, 0x008FB9);
		COLOR_MAP.put(Color.RED, 0xB92A00);
		COLOR_MAP.put(Color.GREEN, 0x16B900);
		COLOR_MAP.put(Color.YELLOW, 0xB6B900);
		COLOR_MAP.put(Color.PURPLE, 0x6100B9);
		COLOR_MAP.put(Color.WHITE, 0xB9B9B9);
	}

	public static int getColor(BossInfo info) {
		return COLOR_MAP.get(info.getColor());
	}

}
