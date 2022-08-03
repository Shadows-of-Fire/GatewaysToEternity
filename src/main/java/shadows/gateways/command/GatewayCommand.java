package shadows.gateways.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import shadows.gateways.entity.GatewayEntity;
import shadows.gateways.gate.GatewayManager;

public class GatewayCommand {

	public static final SuggestionProvider<CommandSourceStack> SUGGEST_TYPE = (ctx, builder) -> {
		return SharedSuggestionProvider.suggest(GatewayManager.INSTANCE.getKeys().stream().map(ResourceLocation::toString), builder);
	};

	public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("open_gateway").requires(s -> s.hasPermission(2));

		builder.then(Commands.argument("pos", BlockPosArgument.blockPos()).then(Commands.argument("type", ResourceLocationArgument.id()).suggests(SUGGEST_TYPE).executes(c -> {
			BlockPos pos = BlockPosArgument.getLoadedBlockPos(c, "pos");
			ResourceLocation type = ResourceLocationArgument.getId(c, "type");
			Entity nullableSummoner = c.getSource().getEntity();
			Player summoner = nullableSummoner instanceof Player ? (Player) nullableSummoner : c.getSource().getLevel().getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 64, true);
			GatewayEntity gate = new GatewayEntity(c.getSource().getLevel(), summoner, GatewayManager.INSTANCE.getValue(type));
			gate.moveTo(pos, 0, 0);
			c.getSource().getLevel().addFreshEntity(gate);
			return 0;
		})));
		pDispatcher.register(builder);
	}

}
