package shadows.gateways.item;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import shadows.gateways.GatewaysToEternityClient;
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
		BlockState state = world.getBlockState(pos);
		entity.setPosition(pos.getX() + 0.5, pos.getY() + state.getShape(world, pos).getEnd(Axis.Y), pos.getZ() + 0.5);
		if (!world.hasNoCollisions(entity)) return ActionResultType.FAIL;
		world.addEntity(entity);
		entity.onGateCreated();
		if (!ctx.getPlayer().isCreative()) stack.shrink(1);
		return ActionResultType.CONSUME;
	}

	@Override
	public ITextComponent getDisplayName(ItemStack stack) {
		if (stack.hasDisplayName()) return super.getDisplayName(stack);
		if (stack.hasTag() && stack.getTag().contains("opener_name")) {
			return ITextComponent.Serializer.getComponentFromJson(stack.getTag().getString("opener_name"));
		}
		return super.getDisplayName(stack);
	}

	public static interface IGateSupplier {
		AbstractGatewayEntity createGate(World world, PlayerEntity player, ItemStack stack);
	}

	@Override
	public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
		if (this.isInGroup(group)) {
			RecipeManager mgr = DistExecutor.unsafeRunForDist(() -> () -> {
				MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
				if (server != null) return server.getRecipeManager(); //Integrated Server
				return GatewaysToEternityClient.getClientRecipeManager(); //Dedicated Client
			}, () -> () -> {
				MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
				if (server == null) return null;
				return server.getRecipeManager(); //Dedicated Server
			});
			if (mgr == null) return;
			mgr.getRecipes().stream().map(r -> r.getRecipeOutput()).filter(s -> s.getItem() == this).forEach(items::add);
		}
	}

}
