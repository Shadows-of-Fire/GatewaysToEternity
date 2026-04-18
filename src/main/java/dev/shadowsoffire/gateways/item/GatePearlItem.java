package dev.shadowsoffire.gateways.item;

import java.util.Comparator;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import dev.shadowsoffire.gateways.GatewayObjects;
import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import dev.shadowsoffire.placebo.tabs.ITabFiller;
import dev.shadowsoffire.placebo.util.SpecialTooltipItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public class GatePearlItem extends Item implements ITabFiller, SpecialTooltipItem {

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
        if (world.isClientSide()) return InteractionResult.SUCCESS;

        Component errMsg = gate.get().canOpen(ctx.getPlayer());
        if (errMsg != null) {
            ctx.getPlayer().sendSystemMessage(Component.translatable("%s", errMsg).withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        GatewayEntity entity = gate.get().createEntity(world, ctx.getPlayer());
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
        setGate(opener, GatewayRegistry.INSTANCE.holder(gate));
    }

    public static void setGate(ItemStack opener, DynamicHolder<Gateway> gate) {
        opener.set(GatewayObjects.GATEWAY_COMPONENT, gate);
    }

    public static DynamicHolder<Gateway> getGate(ItemStack opener) {
        return opener.getOrDefault(GatewayObjects.GATEWAY_COMPONENT, GatewayRegistry.INSTANCE.emptyHolder());
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            return super.getName(stack);
        }

        DynamicHolder<Gateway> gate = getGate(stack);
        if (gate.isBound()) return Component.translatable("gateways.gate_pearl", Component.translatable(gate.getId().toString().replace(':', '.'))).withStyle(Style.EMPTY.withColor(gate.get().color()));
        return super.getName(stack);
    }

    @Override
    public void fillItemCategory(CreativeModeTab group, BuildCreativeModeTabContentsEvent event) {
        generateGatePearlStacks(event::accept);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        DynamicHolder<Gateway> holder = GatePearlItem.getGate(stack);
        if (!holder.isBound()) {
            tooltip.accept(Gateways.lang("text", "errored_gate_pearl", holder.getId().toString()));
        }
        else if (FMLEnvironment.getDist().isClient()) {
            holder.get().appendPearlTooltip(ctx, tooltip, flag);
        }
    }

    @Nullable
    @Override
    public String getCreatorModId(HolderLookup.Provider registries, ItemStack stack) {
        DynamicHolder<Gateway> gate = getGate(stack);
        if (gate.isBound()) {
            return gate.getId().getNamespace();
        }
        return super.getCreatorModId(registries, stack);
    }

    public static void generateGatePearlStacks(Consumer<ItemStack> output) {
        GatewayRegistry.INSTANCE.getValues().stream().sorted(Comparator.comparing(Gateway::size).thenComparing(GatewayRegistry.INSTANCE::getKey)).forEach(gate -> {
            ItemStack stack = new ItemStack(GatewayObjects.GATE_PEARL);
            setGate(stack, gate);
            output.accept(stack);
        });
    }

}
