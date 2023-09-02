package dev.shadowsoffire.gateways.item;

import java.util.Comparator;

import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import dev.shadowsoffire.placebo.tabs.ITabFiller;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GatePearlItem extends Item implements ITabFiller {

    public GatePearlItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        ItemStack stack = ctx.getItemInHand();
        BlockPos pos = ctx.getClickedPos();
        DynamicHolder<Gateway> gate = getGate(stack);

        if (!gate.isBound()) return InteractionResult.FAIL;
        if (world.isClientSide) return InteractionResult.SUCCESS;

        GatewayEntity entity = new GatewayEntity(world, ctx.getPlayer(), gate);
        BlockState state = world.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(world, pos);
        entity.setPos(pos.getX() + 0.5, pos.getY() + (shape.isEmpty() ? 0 : shape.max(Axis.Y)), pos.getZ() + 0.5);

        double spacing = Math.max(0, gate.get().rules().spacing());
        if (!world.getEntitiesOfClass(GatewayEntity.class, entity.getBoundingBox().inflate(spacing)).isEmpty()) return InteractionResult.FAIL;

        int y = 0;
        while (y++ < 4) {
            if (!world.noCollision(entity)) {
                entity.setPos(entity.getX(), entity.getY() + 1, entity.getZ());
            }
            else break;
        }
        if (!world.noCollision(entity)) {
            ctx.getPlayer().sendSystemMessage(Component.translatable("error.gateways.no_space").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        world.addFreshEntity(entity);
        entity.onGateCreated();
        if (!ctx.getPlayer().isCreative()) stack.shrink(1);
        return InteractionResult.CONSUME;
    }

    public static void setGate(ItemStack opener, Gateway gate) {
        opener.getOrCreateTag().putString("gateway", GatewayRegistry.INSTANCE.getKey(gate).toString());
    }

    public static DynamicHolder<Gateway> getGate(ItemStack opener) {
        return GatewayRegistry.INSTANCE.holder(new ResourceLocation(opener.getOrCreateTag().getString("gateway")));
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.hasCustomHoverName()) return super.getName(stack);
        DynamicHolder<Gateway> gate = getGate(stack);
        if (gate.isBound()) return Component.translatable("gateways.gate_pearl", Component.translatable(gate.getId().toString().replace(':', '.'))).withStyle(Style.EMPTY.withColor(gate.get().color()));
        return super.getName(stack);
    }

    public static interface IGateSupplier {
        GatewayEntity createGate(Level world, Player player, Gateway gate);
    }

    @Override
    public void fillItemCategory(CreativeModeTab group, CreativeModeTab.Output out) {
        GatewayRegistry.INSTANCE.getValues().stream().sorted(Comparator.comparing(GatewayRegistry.INSTANCE::getKey)).forEach(gate -> {
            ItemStack stack = new ItemStack(this);
            setGate(stack, gate);
            out.accept(stack);
        });
    }

}
