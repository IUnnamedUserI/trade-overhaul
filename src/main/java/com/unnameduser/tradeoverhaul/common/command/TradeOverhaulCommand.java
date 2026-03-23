package com.unnameduser.tradeoverhaul.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.unnameduser.tradeoverhaul.common.VillagerTradeData;
import com.unnameduser.tradeoverhaul.common.config.ProfessionTradeFile;
import com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader;
import com.unnameduser.tradeoverhaul.common.config.TradeOverhaulSettings;
import com.unnameduser.tradeoverhaul.common.trade.TradeRestock;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class TradeOverhaulCommand {
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("tradeoverhaul")
			.requires(source -> source.hasPermissionLevel(2))
			.then(CommandManager.literal("refresh")
				.executes(ctx -> refreshAll(ctx.getSource()))
				.then(CommandManager.argument("player", EntityArgumentType.player())
					.executes(ctx -> refreshNearPlayer(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player")))
					.then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 200))
						.executes(ctx -> refreshNearPlayerWithRadius(
							ctx.getSource(),
							EntityArgumentType.getPlayer(ctx, "player"),
							IntegerArgumentType.getInteger(ctx, "radius")
						))
					)
				)
			)
		);
	}

	private static int refreshAll(ServerCommandSource source) {
		int count = 0;
		for (ServerWorld world : source.getServer().getWorlds()) {
			List<VillagerEntity> villagers = new ArrayList<>();
			for (Entity e : world.iterateEntities()) {
				if (e instanceof VillagerEntity) {
					villagers.add((VillagerEntity) e);
				}
			}
			for (VillagerEntity villager : villagers) {
				if (refreshVillager(villager, source)) {
					count++;
				}
			}
		}
		final int finalCount = count;
		source.sendFeedback(() -> Text.literal("Обновлено " + finalCount + " жителей(ей)"), true);
		return count;
	}

	private static int refreshNearPlayer(ServerCommandSource source, net.minecraft.entity.Entity player) {
		return refreshNearPlayerWithRadius(source, player, 50);
	}

	private static int refreshNearPlayerWithRadius(ServerCommandSource source, net.minecraft.entity.Entity player, int radius) {
		int count = 0;
		ServerWorld world = (ServerWorld) player.getWorld();
		List<VillagerEntity> villagers = new ArrayList<>();
		for (Entity e : world.iterateEntities()) {
			if (e instanceof VillagerEntity && e.getBoundingBox().intersects(player.getBoundingBox().expand(radius))) {
				villagers.add((VillagerEntity) e);
			}
		}
		for (VillagerEntity villager : villagers) {
			if (refreshVillager(villager, source)) {
				count++;
			}
		}
		final int finalCount = count;
		final String playerName = player.getName().getString();
		source.sendFeedback(() -> Text.literal("Обновлено " + finalCount + " жителей(ей) в радиусе " + radius + " блоков от " + playerName), true);
		return count;
	}

	private static boolean refreshVillager(VillagerEntity villager, ServerCommandSource source) {
		Identifier profId = Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession());
		if (profId == null) return false;
		
		ProfessionTradeFile file = TradeConfigLoader.getProfession(profId);
		if (file == null) return false;
		
		VillagerTradeData data = (VillagerTradeData) villager;
		TradeOverhaulSettings settings = TradeConfigLoader.getSettings();
		
		// Clear current offers
		data.tradeOverhaul$setOfferSlots(new int[0]);
		data.tradeOverhaul$setEmptySinceTick(-1L);
		
		// Trigger restock on next tick
		TradeRestock.tick(villager);
		
		return true;
	}
}
