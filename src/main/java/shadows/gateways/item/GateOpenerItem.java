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

public class GateOpenerItem extends Item {

	protected final IGateSupplier factory;

	public GateOpenerItem(Properties props, IGateSupplier factory) {
		super(props);
		this.factory = factory;
	}

	@Override
	public ActionResultType useOn(ItemUseContext ctx) {
		World world = ctx.getLevel();
		ItemStack stack = ctx.getItemInHand();
		BlockPos pos = ctx.getClickedPos();

		if (world.isClientSide) return ActionResultType.SUCCESS;

		if (!world.getEntitiesOfClass(AbstractGatewayEntity.class, new AxisAlignedBB(pos).inflate(25, 25, 25)).isEmpty()) return ActionResultType.FAIL;

		AbstractGatewayEntity entity = factory.createGate(world, ctx.getPlayer(), stack);
		BlockState state = world.getBlockState(pos);
		entity.setPos(pos.getX() + 0.5, pos.getY() + state.getShape(world, pos).max(Axis.Y), pos.getZ() + 0.5);
		if (!world.noCollision(entity)) return ActionResultType.FAIL;
		world.addFreshEntity(entity);
		entity.onGateCreated();
		if (!ctx.getPlayer().isCreative()) stack.shrink(1);
		return ActionResultType.CONSUME;
	}

	@Override
	public ITextComponent getName(ItemStack stack) {
		if (stack.hasCustomHoverName()) return super.getName(stack);
		if (stack.hasTag() && stack.getTag().contains("opener_name")) {
			return ITextComponent.Serializer.fromJson(stack.getTag().getString("opener_name"));
		}
		return super.getName(stack);
	}

	public static interface IGateSupplier {
		AbstractGatewayEntity createGate(World world, PlayerEntity player, ItemStack stack);
	}

	@Override
	public void fillItemCategory(ItemGroup group, NonNullList<ItemStack> items) {
		if (this.allowdedIn(group)) {
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
			mgr.getRecipes().stream().map(r -> r.getResultItem()).filter(s -> s.getItem() == this).forEach(items::add);
		}
	}

}
