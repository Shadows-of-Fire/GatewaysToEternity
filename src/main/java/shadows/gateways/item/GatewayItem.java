package shadows.gateways.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import shadows.gateways.entity.GatewayEntity;

public class GatewayItem extends Item {

	public GatewayItem(Properties props) {
		super(props);
	}

	@Override
	public ActionResultType onItemUse(ItemUseContext ctx) {
		World world = ctx.getWorld();
		ItemStack stack = ctx.getItem();
		BlockPos pos = ctx.getPos();

		if (world.isRemote) return ActionResultType.SUCCESS;

		if (!world.getEntitiesWithinAABB(GatewayEntity.class, new AxisAlignedBB(pos).grow(10, 10, 10)).isEmpty()) return ActionResultType.FAIL;

		GatewayEntity entity = new GatewayEntity(world, ctx.getPlayer(), stack);
		entity.setPosition(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
		world.addEntity(entity);
		if (!ctx.getPlayer().isCreative()) stack.shrink(1);
		return ActionResultType.CONSUME;
	}

	@Override
	public ITextComponent getDisplayName(ItemStack stack) {
		if (stack.hasDisplayName()) return super.getDisplayName(stack);
		if(stack.hasTag() && stack.getTag().contains("opener_name")) {
			return ITextComponent.Serializer.fromJson(stack.getTag().getString("opener_name"));
		}
		return super.getDisplayName(stack);
	}

}
