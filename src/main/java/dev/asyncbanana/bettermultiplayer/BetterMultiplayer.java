package dev.asyncbanana.bettermultiplayer;

import java.util.Collection;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.Item.Settings;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.RandomChanceLootCondition;

public class BetterMultiplayer implements ModInitializer {
	public static final String MOD_ID = "bettermultiplayer";

	@Override
	public void onInitialize() {
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			LifeCounter.PlayerData playerState = LifeCounter.getPlayerState(newPlayer);
			playerState.lives--;
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			LifeCounter.PlayerData playerState = LifeCounter.getPlayerState(newPlayer);
			if (playerState.lives < 1) {
				newPlayer.networkHandler
						.disconnect(Text.literal("You ran out of lives *Whomp Whomp*"));
			}
		});
		// might be able to kick earlier in handshake?
		ServerPlayConnectionEvents.INIT.register((handler, server) -> {
			LifeCounter.PlayerData playerState = LifeCounter.getPlayerState(handler.player);
			if (playerState.lives < 1) {
				handler
						.disconnect(Text.literal("You ran out of lives *Whomp Whomp*"));
			}
		});
		CommandRegistrationCallback.EVENT
				.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager
						.literal("lives")
						.then(CommandManager.literal("set").requires(source -> source.hasPermissionLevel(2)).then(
								CommandManager.argument("targets", GameProfileArgumentType.gameProfile())
										.then(CommandManager
												.argument("lives", IntegerArgumentType.integer()).executes(context -> {
													Collection<GameProfile> players = GameProfileArgumentType
															.getProfileArgument(context, "targets");
													int lives = IntegerArgumentType.getInteger(context, "lives");
													for (GameProfile player : players) {
														LifeCounter.PlayerData playerState = LifeCounter
																.getPlayerState(player,
																		context.getSource().getServer());
														playerState.lives = lives;
													}

													context.getSource().sendFeedback(
															() -> Text.literal("Lives of players set to " + lives),
															false);
													return 1;
												}))))
						.then(CommandManager.literal("get").requires(source -> source.hasPermissionLevel(2)).then(
								CommandManager.argument("targets", GameProfileArgumentType.gameProfile())
										.executes(context -> {
											Collection<GameProfile> players = GameProfileArgumentType
													.getProfileArgument(context, "targets");
											for (GameProfile player : players) {
												LifeCounter.PlayerData playerState = LifeCounter
														.getPlayerState(player,
																context.getSource().getServer());
												context.getSource().sendFeedback(
														() -> Text.literal(player.getName() + " currently has "
																+ +playerState.lives
																+ (playerState.lives != 1 ? " lives" : " life")),
														false);
											}

											return 1;
										})))
						.executes(context -> {
							LifeCounter.PlayerData playerState = LifeCounter
									.getPlayerState(context.getSource().getPlayer());
							context.getSource().sendFeedback(
									() -> Text.literal("You currently have " + playerState.lives
											+ (playerState.lives != 1 ? " lives" : " life")),
									false);

							return 1;
						})));
		Settings ashSettings = new Item.Settings();
		ashSettings.maxCount(1);
		ashSettings.rarity(Rarity.EPIC);
		Item ashItem = Registry.register(Registries.ITEM, Identifier.of(BetterMultiplayer.MOD_ID, "phoenix_ash"),
				new PhoenixAsh(ashSettings));
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			// Let's only modify built-in loot tables and leave data pack loot tables
			// untouched by checking the source.
			// We also check that the loot table ID is equal to the ID we want.
			if (LootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_UNIQUE_CHEST.equals(key)
					|| LootTables.ANCIENT_CITY_ICE_BOX_CHEST.equals(key)) {
				LootPool.Builder poolBuilder = LootPool.builder().with(ItemEntry.builder(ashItem))
						.conditionally(RandomChanceLootCondition.builder(0.05f));
				tableBuilder.pool(poolBuilder);
			}
		});
	}

}
