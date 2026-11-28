package dev.hypixelcustomrecipe.mixin;

import dev.hypixelcustomrecipe.ModArmorItems;
import dev.hypixelcustomrecipe.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    @Shadow public int repairItemCountCost;
    @Shadow @Final private DataSlot cost;

    @Unique
    private boolean hypixelcustomrecipe$starUpgrade;

    protected AnvilMenuMixin(int containerId, ContainerLevelAccess access) {
        super(null, containerId, null, access, null);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void hypixelcustomrecipe$createArmorUpgrade(CallbackInfo ci) {
        ItemStack left = this.inputSlots.getItem(0);
        if (left.isEmpty()) {
            return;
        }

        String currentId = null;
        for (Map.Entry<String, Item> entry : ModArmorItems.all().entrySet()) {
            if (entry.getValue() == left.getItem()) {
                currentId = entry.getKey();
                break;
            }
        }

        if (currentId == null) {
            hypixelcustomrecipe$starUpgrade = false;
            return;
        }

        hypixelcustomrecipe$starUpgrade = true;
        ItemStack output = ItemStack.EMPTY;
        String nextId = null;
        Item requiredShard = null;

        if (currentId.endsWith("_3star_void")) {
            // Void-upgraded armor is the final upgrade tier.
        } else if (currentId.endsWith("_3star")) {
            nextId = currentId + "_void";
            requiredShard = ModItems.PURE_VOID_SHARD;
        } else if (currentId.endsWith("_1star")) {
            nextId = currentId.substring(0, currentId.length() - "_1star".length()) + "_2star";
            requiredShard = ModItems.STAR_SHARD_2;
        } else if (currentId.endsWith("_2star")) {
            nextId = currentId.substring(0, currentId.length() - "_2star".length()) + "_3star";
            requiredShard = ModItems.STAR_SHARD_3;
        } else if (!currentId.endsWith("_3star")) {
            nextId = currentId + "_1star";
            requiredShard = ModItems.STAR_SHARD_1;
        }

        ItemStack right = this.inputSlots.getItem(1);
        if (nextId != null && !right.isEmpty() && right.is(requiredShard)) {
            Item nextArmor = ModArmorItems.get(nextId);
            if (nextArmor != null) {
                output = new ItemStack(nextArmor);
            }
        }

        this.resultSlots.setItem(0, output);
        this.repairItemCountCost = output.isEmpty() ? 0 : 1;
        this.cost.set(output.isEmpty() ? 0 : 1);
        ci.cancel();
    }

    @Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
    protected void hypixelcustomrecipe$takeStarUpgrade(Player player, ItemStack stack, CallbackInfo ci) {
        if (!hypixelcustomrecipe$starUpgrade) {
            return;
        }

        hypixelcustomrecipe$starUpgrade = false;
        this.inputSlots.setItem(0, ItemStack.EMPTY);

        ItemStack shard = this.inputSlots.getItem(1);
        if (!shard.isEmpty()) {
            if (shard.getCount() <= 1) {
                this.inputSlots.setItem(1, ItemStack.EMPTY);
            } else {
                shard.shrink(1);
                this.inputSlots.setItem(1, shard);
            }
        }

        this.repairItemCountCost = 0;
        this.cost.set(0);
        this.access.execute((level, pos) ->
                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F));
        ci.cancel();
    }
}
