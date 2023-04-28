package shadows.gateways.gate;

import java.util.Collections;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.TextColor;
import shadows.gateways.entity.GatewayEntity.GatewaySize;
import shadows.placebo.json.PSerializer;
import shadows.placebo.json.TypeKeyed.TypeKeyedBase;

public class Gateway extends TypeKeyedBase<Gateway> {

	//Formatter::off
	public static Codec<Gateway> CODEC = RecordCodecBuilder.create(inst -> inst
		.group(
			GatewaySize.CODEC.fieldOf("size").forGetter(Gateway::getSize),
			TextColor.CODEC.fieldOf("color").forGetter(Gateway::getColor),
			Wave.CODEC.listOf().fieldOf("waves").forGetter(Gateway::getWaves),
			Reward.CODEC.listOf().optionalFieldOf("rewards", Collections.emptyList()).forGetter(Gateway::getRewards),
			Failure.CODEC.listOf().optionalFieldOf("failures", Collections.emptyList()).forGetter(Gateway::getFailures),
			Codec.INT.fieldOf("completion_xp").forGetter(Gateway::getCompletionXp),
			Codec.DOUBLE.fieldOf("spawn_range").forGetter(Gateway::getSpawnRange),
			Codec.DOUBLE.optionalFieldOf("leash_range", 32D).forGetter(g -> g.leashRange))
			.apply(inst, Gateway::new)
		);
	//Formatter::on

	public static final PSerializer<Gateway> SERIALIZER = PSerializer.fromCodec("Gateway", CODEC);

	protected final GatewaySize size;
	protected final TextColor color;
	protected final List<Wave> waves;
	protected final List<Reward> rewards;
	protected final List<Failure> failures;
	protected final int completionXp;
	protected final double spawnRange;
	protected final double leashRange;

	Gateway(GatewaySize size, TextColor color, List<Wave> waves, List<Reward> rewards, List<Failure> failures, int completionXp, double spawnRange, double leashRange) {
		this.size = size;
		this.color = color;
		this.waves = waves;
		this.rewards = rewards;
		this.failures = failures;
		this.completionXp = completionXp;
		this.spawnRange = spawnRange;
		this.leashRange = leashRange;
	}

	public GatewaySize getSize() {
		return size;
	}

	public TextColor getColor() {
		return color;
	}

	public List<Wave> getWaves() {
		return waves;
	}

	public int getNumWaves() {
		return waves.size();
	}

	public Wave getWave(int n) {
		return this.waves.get(n);
	}

	public List<Reward> getRewards() {
		return rewards;
	}

	public List<Failure> getFailures() {
		return failures;
	}

	public int getCompletionXp() {
		return this.completionXp;
	}

	public double getSpawnRange() {
		return this.spawnRange;
	}

	public double getLeashRangeSq() {
		return this.leashRange * this.leashRange;
	}

	@Override
	public PSerializer<? extends Gateway> getSerializer() {
		return SERIALIZER;
	}

}
