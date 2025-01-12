package dev.asyncbanana.bettermultiplayer.mixin;

import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.asyncbanana.bettermultiplayer.LifeCounter;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "copyFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;setScore(I)V", shift = At.Shift.AFTER))
    public void onCopyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        LifeCounter.PlayerData playerState = LifeCounter.getPlayerState((ServerPlayerEntity) (Object) this);
        playerState.lives++;
    }
}
