package dev.shadowsoffire.gateways.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.GatewayManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class GatewayCommand {

	public static final SuggestionProvider<CommandSourceStack> SUGGEST_TYPE = (ctx, builder) -> {
		return SharedSuggestionProvider.suggest(GatewayManager.INSTANCE.getKeys().stream().map(ResourceLocation::toString), builder);
	};

	public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("open_gateway").requires(s -> s.hasPermission(2));

		builder.then(Commands.argument("pos", Vec3Argument.vec3()).then(Commands.argument("type", ResourceLocationArgument.id()).suggests(SUGGEST_TYPE).executes(c -> {
			return openGateway(c, Vec3Argument.getVec3(c, "pos"), ResourceLocationArgument.getId(c, "type"));
		})));
		builder.then(Commands.argument("entity", EntityArgument.entity()).then(Commands.argument("type", ResourceLocationArgument.id()).suggests(SUGGEST_TYPE).executes(c -> {
			return openGateway(c, EntityArgument.getEntity(c, "entity").position(), ResourceLocationArgument.getId(c, "type"));
		})));
		pDispatcher.register(builder);
	}

	public static int openGateway(CommandContext<CommandSourceStack> c, Vec3 pos, ResourceLocation type) {
		try {
			Entity nullableSummoner = c.getSource().getEntity();
			Player summoner = nullableSummoner instanceof Player ? (Player) nullableSummoner : c.getSource().getLevel().getNearestPlayer(pos.x(), pos.y(), pos.z(), 64, false);
			GatewayEntity gate = new GatewayEntity(c.getSource().getLevel(), summoner, GatewayManager.INSTANCE.getValue(type));
			gate.moveTo(pos);
			c.getSource().getLevel().addFreshEntity(gate);
			gate.onGateCreated();
		} catch (Exception ex) {	
			c.getSource().sendFailure(Component.literal("Exception thrown - see log"));
			ex.printStackTrace();
		}
		return 0;
	}

}
