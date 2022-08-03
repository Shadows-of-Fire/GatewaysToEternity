package shadows.gateways.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import shadows.gateways.entity.GatewayEntity;
import shadows.gateways.gate.Gateway;
import shadows.gateways.gate.GatewayManager;

public class GateOpenerItem extends Item {

	protected final IGateSupplier factory;

	public GateOpenerItem(Properties props, IGateSupplier factory) {
		super(props);
		this.factory = factory;
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level world = ctx.getLevel();
		ItemStack stack = ctx.getItemInHand();
		BlockPos pos = ctx.getClickedPos();

		if (world.isClientSide) return InteractionResult.SUCCESS;

		if (!world.getEntitiesOfClass(GatewayEntity.class, new AABB(pos).inflate(25, 25, 25)).isEmpty()) return InteractionResult.FAIL;

		GatewayEntity entity = factory.createGate(world, ctx.getPlayer(), getGate(stack));
		BlockState state = world.getBlockState(pos);
		entity.setPos(pos.getX() + 0.5, pos.getY() + state.getShape(world, pos).max(Axis.Y), pos.getZ() + 0.5);
		if (!world.noCollision(entity)) return InteractionResult.FAIL;
		world.addFreshEntity(entity);
		entity.onGateCreated();
		if (!ctx.getPlayer().isCreative()) stack.shrink(1);
		return InteractionResult.CONSUME;
	}

	public static void setGate(ItemStack opener, Gateway gate) {
		opener.getOrCreateTag().putString("gateway", gate.getId().toString());
	}

	public static Gateway getGate(ItemStack opener) {
		return GatewayManager.INSTANCE.getValue(new ResourceLocation(opener.getOrCreateTag().getString("gateway")));
	}

	@Override
	public Component getName(ItemStack stack) {
		if (stack.hasCustomHoverName()) return super.getName(stack);
		Gateway gate = getGate(stack);
		if (gate != null) return new TranslatableComponent("gateways.gate_opener", new TranslatableComponent(gate.getId().toString().replace(':', '.'))).withStyle(Style.EMPTY.withColor(gate.getColor()));
		return super.getName(stack);
	}

	public static interface IGateSupplier {
		GatewayEntity createGate(Level world, Player player, Gateway gate);
	}

	@Override
	public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> items) {
		if (this.allowdedIn(group)) {
			for (Gateway gate : GatewayManager.INSTANCE.getValues()) {
				ItemStack stack = new ItemStack(this);
				setGate(stack, gate);
				items.add(stack);
			}
		}
	}

}
