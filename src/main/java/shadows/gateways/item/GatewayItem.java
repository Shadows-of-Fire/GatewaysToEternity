package shadows.gateways.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import shadows.gateways.entity.AbstractGatewayEntity;

public class GatewayItem extends Item {

	protected final IGateSupplier factory;

	public GatewayItem(Properties props, IGateSupplier factory) {
		super(props);
		this.factory = factory;
	}

	@Override
	public ActionResultType onItemUse(ItemUseContext ctx) {
		World world = ctx.getWorld();
		ItemStack stack = ctx.getItem();
		BlockPos pos = ctx.getPos();

		if (world.isRemote) return ActionResultType.SUCCESS;

		if (!world.getEntitiesWithinAABB(AbstractGatewayEntity.class, new AxisAlignedBB(pos).grow(25, 25, 25)).isEmpty()) return ActionResultType.FAIL;

		AbstractGatewayEntity entity = factory.createGate(world, ctx.getPlayer(), stack);
		entity.setPosition(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
		if (!world.doesNotCollide(entity)) return ActionResultType.FAIL;
		world.addEntity(entity);
		entity.onGateCreated();
		if (!ctx.getPlayer().isCreative()) stack.shrink(1);
		return ActionResultType.CONSUME;
	}

	@Override
	public ITextComponent getDisplayName(ItemStack stack) {
		if (stack.hasDisplayName()) return super.getDisplayName(stack);
		if (stack.hasTag() && stack.getTag().contains("opener_name")) {
			return ITextComponent.Serializer.fromJson(stack.getTag().getString("opener_name"));
		}
		return super.getDisplayName(stack);
	}

	public static interface IGateSupplier {
		AbstractGatewayEntity createGate(World world, PlayerEntity player, ItemStack stack);
	}

}
