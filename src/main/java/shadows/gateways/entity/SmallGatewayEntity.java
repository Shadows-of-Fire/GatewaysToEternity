package shadows.gateways.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.BossInfo;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerBossInfo;
import shadows.gateways.GatewaysToEternity;
import shadows.placebo.util.AttributeHelper;

public class SmallGatewayEntity extends AbstractGatewayEntity {

	public SmallGatewayEntity(World world, PlayerEntity placer, ItemStack source) {
		super(GatewaysToEternity.SMALL_GATEWAY, world, placer, source);
	}

	public SmallGatewayEntity(EntityType<?> type, World world) {
		super(type, world);
	}

	@Override
	protected ServerBossInfo createBossInfo() {
		return new ServerBossInfo(this.getDefaultName(), BossInfo.Color.BLUE, BossInfo.Overlay.NOTCHED_6);
	}

	@Override
	protected GatewayStats createStats() {
		return new GatewayStats(6, 3, 5, 100);
	}

	@Override
	protected GatewaySize getSize() {
		return GatewaySize.SMALL;
	}

	@Override
	protected void modifyEntityForWave(int wave, LivingEntity entity) {
		AttributeHelper.multiplyFinal(entity, SharedMonsterAttributes.MAX_HEALTH, "gateways_gate", (wave - 1) * 0.75F);
		AttributeHelper.addToBase(entity, SharedMonsterAttributes.ARMOR, "gateways_gate", (wave - 1) * 0.35F);
		AttributeHelper.multiplyFinal(entity, SharedMonsterAttributes.ATTACK_DAMAGE, "gateways_gate", (wave - 1) * 0.35F);
		AttributeHelper.multiplyFinal(entity, SharedMonsterAttributes.KNOCKBACK_RESISTANCE, "gateways_gate", (wave - 1) * 0.05F);
		AttributeHelper.multiplyFinal(entity, SharedMonsterAttributes.MOVEMENT_SPEED, "gateways_gate", (wave - 1) * 0.002F);
		entity.setHealth(entity.getMaxHealth());
	}

}
