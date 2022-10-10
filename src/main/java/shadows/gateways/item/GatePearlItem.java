package shadows.gateways.item;

import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import shadows.gateways.entity.GatewayEntity;
import shadows.gateways.gate.Gateway;
import shadows.gateways.gate.GatewayManager;

public class GatePearlItem extends Item {

	public GatePearlItem(Properties props) {
		super(props);
	}

	@Override
	public ActionResultType useOn(ItemUseContext ctx) {
		World world = ctx.getLevel();
		ItemStack stack = ctx.getItemInHand();
		BlockPos pos = ctx.getClickedPos();

		if (world.isClientSide) return ActionResultType.SUCCESS;

		if (!world.getEntitiesOfClass(GatewayEntity.class, new AxisAlignedBB(pos).inflate(25, 25, 25)).isEmpty()) return ActionResultType.FAIL;

		GatewayEntity entity = new GatewayEntity(world, ctx.getPlayer(), getGate(stack));
		BlockState state = world.getBlockState(pos);
		entity.setPos(pos.getX() + 0.5, pos.getY() + state.getShape(world, pos).max(Axis.Y), pos.getZ() + 0.5);
		int y = 0;
		while (y++ < 4) {
			if (!world.noCollision(entity)) {
				entity.setPos(entity.getX(), entity.getY() + 1, entity.getZ());
			} else break;
		}
		if (!world.noCollision(entity)) {
			ctx.getPlayer().sendMessage(new TranslationTextComponent("error.gateways.no_space").withStyle(TextFormatting.RED), Util.NIL_UUID);
			return ActionResultType.FAIL;
		}
		world.addFreshEntity(entity);
		entity.onGateCreated();
		if (!ctx.getPlayer().isCreative()) stack.shrink(1);
		return ActionResultType.CONSUME;
	}

	public static void setGate(ItemStack opener, Gateway gate) {
		opener.getOrCreateTag().putString("gateway", gate.getId().toString());
	}

	public static Gateway getGate(ItemStack opener) {
		return GatewayManager.INSTANCE.getValue(new ResourceLocation(opener.getOrCreateTag().getString("gateway")));
	}

	@Override
	public ITextComponent getName(ItemStack stack) {
		if (stack.hasCustomHoverName()) return super.getName(stack);
		Gateway gate = getGate(stack);
		if (gate != null) return new TranslationTextComponent("gateways.gate_pearl", new TranslationTextComponent(gate.getId().toString().replace(':', '.'))).withStyle(Style.EMPTY.withColor(gate.getColor()));
		return super.getName(stack);
	}

	@Override
	public void fillItemCategory(ItemGroup group, NonNullList<ItemStack> items) {
		if (this.allowdedIn(group)) {
			GatewayManager.INSTANCE.getValues().stream().sorted((g1, g2) -> g1.getId().compareTo(g2.getId())).forEach(gate -> {
				ItemStack stack = new ItemStack(this);
				setGate(stack, gate);
				items.add(stack);
			});
		}
	}

}
