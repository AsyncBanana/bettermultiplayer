package dev.asyncbanana.bettermultiplayer;

import java.util.List;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class PhoenixAsh extends Item {
    public PhoenixAsh(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            return TypedActionResult.pass(user.getStackInHand(hand));
        }

        LifeCounter.PlayerData playerState = LifeCounter.getPlayerState(user);
        if (playerState.lives > 2) {
            user.sendMessage(Text.literal("You already have three lives!"));
            return TypedActionResult.pass(user.getStackInHand(hand));
        }
        playerState.lives++;
        user.getStackInHand(hand).decrementUnlessCreative(1, user);
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("It still glows with the essence of rebirth").formatted(Formatting.GOLD)
                .formatted(Formatting.ITALIC));
    }
}
