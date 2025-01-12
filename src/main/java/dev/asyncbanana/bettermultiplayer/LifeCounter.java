package dev.asyncbanana.bettermultiplayer;

import java.util.HashMap;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public class LifeCounter extends PersistentState {

    public HashMap<UUID, PlayerData> players = new HashMap<>();

    public static class PlayerData {
        public int lives = 3;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup registryLookup) {

        NbtCompound playersNbt = new NbtCompound();
        players.forEach((uuid, playerData) -> {
            NbtCompound playerNbt = new NbtCompound();

            playerNbt.putInt("lives", playerData.lives);

            playersNbt.put(uuid.toString(), playerNbt);
        });
        nbt.put("players", playersNbt);

        return nbt;
    }

    public static LifeCounter createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        LifeCounter state = new LifeCounter();

        NbtCompound playersNbt = tag.getCompound("players");
        playersNbt.getKeys().forEach(key -> {
            PlayerData playerData = new PlayerData();

            playerData.lives = playersNbt.getCompound(key).getInt("lives");

            UUID uuid = UUID.fromString(key);
            state.players.put(uuid, playerData);
        });

        return state;
    }

    private static Type<LifeCounter> type = new Type<>(
            LifeCounter::new, // If there's no 'LifeCounter' yet create one
            LifeCounter::createFromNbt, // If there is a 'LifeCounter' NBT, parse it with 'createFromNbt'
            null // Supposed to be an 'DataFixTypes' enum, but we can just pass null
    );

    public static LifeCounter getServerState(MinecraftServer server) {
        // (Note: arbitrary choice to use 'World.OVERWORLD' instead of 'World.END' or
        // 'World.NETHER'. Any work)
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();

        // The first time the following 'getOrCreate' function is called, it creates a
        // brand new 'LifeCounter' and
        // stores it inside the 'PersistentStateManager'. The subsequent calls to
        // 'getOrCreate' pass in the saved
        // 'LifeCounter' NBT on disk to our function 'LifeCounter::createFromNbt'.
        LifeCounter state = persistentStateManager.getOrCreate(type, BetterMultiplayer.MOD_ID);

        // If state is not marked dirty, when Minecraft closes, 'writeNbt' won't be
        // called and therefore nothing will be saved.
        // Technically it's 'cleaner' if you only mark state as dirty when there was
        // actually a change, but the vast majority
        // of mod writers are just going to be confused when their data isn't being
        // saved, and so it's best just to 'markDirty' for them.
        // Besides, it's literally just setting a bool to true, and the only time
        // there's a 'cost' is when the file is written to disk when
        // there were no actual change to any of the mods state (INCREDIBLY RARE).
        state.markDirty();

        return state;
    }

    public static PlayerData getPlayerState(LivingEntity player) {
        LifeCounter serverState = getServerState(player.getWorld().getServer());

        PlayerData playerState = serverState.players.computeIfAbsent(player.getUuid(), uuid -> new PlayerData());

        return playerState;
    }

    public static PlayerData getPlayerState(GameProfile player, MinecraftServer server) {
        LifeCounter serverState = getServerState(server);

        PlayerData playerState = serverState.players.computeIfAbsent(player.getId(), uuid -> new PlayerData());

        return playerState;
    }
}
