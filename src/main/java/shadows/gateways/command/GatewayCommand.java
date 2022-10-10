package shadows.gateways.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.ResourceLocationArgument;
import net.minecraft.command.arguments.Vec3Argument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import shadows.gateways.entity.GatewayEntity;
import shadows.gateways.gate.GatewayManager;

public class GatewayCommand {

	public static final SuggestionProvider<CommandSource> SUGGEST_TYPE = (ctx, builder) -> {
		return ISuggestionProvider.suggestResource(GatewayManager.INSTANCE.getKeys(), builder);
	};

	public static void register(CommandDispatcher<CommandSource> pDispatcher) {
		LiteralArgumentBuilder<CommandSource> builder = Commands.literal("open_gateway").requires(s -> s.hasPermission(2));

		builder.then(Commands.argument("pos", Vec3Argument.vec3()).then(Commands.argument("type", ResourceLocationArgument.id()).suggests(SUGGEST_TYPE).executes(c -> {
			return openGateway(c, Vec3Argument.getVec3(c, "pos"), ResourceLocationArgument.getId(c, "type"));
		})));
		builder.then(Commands.argument("entity", EntityArgument.entity()).then(Commands.argument("type", ResourceLocationArgument.id()).suggests(SUGGEST_TYPE).executes(c -> {
			return openGateway(c, EntityArgument.getEntity(c, "entity").position(), ResourceLocationArgument.getId(c, "type"));
		})));
		pDispatcher.register(builder);
	}

	public static int openGateway(CommandContext<CommandSource> c, Vector3d pos, ResourceLocation type) {
		Entity nullableSummoner = c.getSource().getEntity();
		PlayerEntity summoner = nullableSummoner instanceof PlayerEntity ? (PlayerEntity) nullableSummoner : c.getSource().getLevel().getNearestPlayer(pos.x(), pos.y(), pos.z(), 64, false);
		GatewayEntity gate = new GatewayEntity(c.getSource().getLevel(), summoner, GatewayManager.INSTANCE.getValue(type));
		gate.moveTo(pos);
		c.getSource().getLevel().addFreshEntity(gate);
		return 0;
	}

}
