package shadows.gateways.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.BossInfo;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerBossInfo;
import shadows.gateways.GatewayObjects;
import shadows.placebo.util.AttributeHelper;

public class SmallGatewayEntity extends AbstractGatewayEntity {

	public SmallGatewayEntity(World world, PlayerEntity placer, ItemStack source) {
		super(GatewayObjects.SMALL_GATEWAY, world, placer, source);
	}

	public SmallGatewayEntity(EntityType<?> type, World world) {
		super(type, world);
	}

	@Override
	protected ServerBossInfo createBossInfo() {
		ServerBossInfo info = new ServerBossInfo(this.getName(), BossInfo.Color.BLUE, BossInfo.Overlay.NOTCHED_6);
		info.setCreateFog(true);
		return info;
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
		if (wave == 1) return;
		wave--;
		AttributeHelper.multiplyFinal(entity, Attributes.MAX_HEALTH, "gateways_gate", Math.pow(1.33F, wave) - 1);
		AttributeHelper.addToBase(entity, Attributes.ARMOR, "gateways_gate", wave * 3);
		AttributeHelper.multiplyFinal(entity, Attributes.ATTACK_DAMAGE, "gateways_gate", Math.pow(1.33F, wave) - 1);
		AttributeHelper.multiplyFinal(entity, Attributes.KNOCKBACK_RESISTANCE, "gateways_gate", wave * 0.05F);
		AttributeHelper.multiplyFinal(entity, Attributes.MOVEMENT_SPEED, "gateways_gate", wave * 0.01F);
		entity.setHealth(entity.getMaxHealth());
	}

}
