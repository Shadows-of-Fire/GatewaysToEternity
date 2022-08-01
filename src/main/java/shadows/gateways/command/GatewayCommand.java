package shadows.gateways.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.command.arguments.ResourceLocationArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import shadows.gateways.entity.SmallGatewayEntity;
import shadows.gateways.item.GateOpenerItem;

public class GatewayCommand {

	public static final SuggestionProvider<CommandSource> SUGGEST_TYPE = (ctx, builder) -> {
		return ISuggestionProvider.suggest(ctx.getSource().getServer().getRecipeManager().getRecipes().stream().filter(r -> r.getResultItem().getItem() instanceof GateOpenerItem).map(r -> r.getId().toString()), builder);
	};

	public static void register(CommandDispatcher<CommandSource> pDispatcher) {
		LiteralArgumentBuilder<CommandSource> builder = Commands.literal("open_gateway").requires(s -> s.hasPermission(2));

		builder.then(Commands.argument("pos", BlockPosArgument.blockPos()).then(Commands.argument("type", ResourceLocationArgument.id()).suggests(SUGGEST_TYPE).executes(c -> {
			BlockPos pos = BlockPosArgument.getLoadedBlockPos(c, "pos");
			ResourceLocation type = ResourceLocationArgument.getId(c, "type");
			IRecipe<?> recipe = c.getSource().getServer().getRecipeManager().byKey(type).get();
			Entity nullableSummoner = c.getSource().getEntity();
			PlayerEntity summoner = nullableSummoner instanceof PlayerEntity ? (PlayerEntity) nullableSummoner : c.getSource().getLevel().getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 64, true);

			SmallGatewayEntity gate = new SmallGatewayEntity(c.getSource().getLevel(), summoner, recipe.getResultItem().copy());
			gate.moveTo(pos, 0, 0);
			c.getSource().getLevel().addFreshEntity(gate);
			return 0;
		})));
		pDispatcher.register(builder);
	}

}
