package io.github.alkyaly.timeinabottle.item;

import io.github.alkyaly.timeinabottle.ModConfig;
import io.github.alkyaly.timeinabottle.entity.AcceleratorEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

public class TimeInABottleItem extends Item {

    public TimeInABottleItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        ItemStack stack = player.getStackInHand(context.getHand());
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        Box box = new Box(new BlockPos(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d)).shrink(.2f, .2f, .2f);

        if (!world.isClient) {
            List<AcceleratorEntity> list = world.getEntitiesByClass(AcceleratorEntity.class, box, (e) -> true);
            if (!list.isEmpty()) {
                AcceleratorEntity eta = list.get(0);
                int currentRate = eta.getTimeRate();
                int usedUpTime = 20 * 30 - eta.getRemainingTime();

                if (currentRate < 32) {
                    int nextRate = currentRate * 2;
                    int timeRequired = nextRate / 2 * 20 * 30;
                    NbtCompound timeData = stack.getSubTag("timeData");
                    int timeAvailable = timeData.getInt("storedTime");

                    if (timeAvailable >= timeRequired) {
                        int timeAdded = (nextRate * usedUpTime - currentRate * usedUpTime) / nextRate;

                        if (!player.getAbilities().creativeMode) {
                            timeData.putInt("storedTime", timeAvailable - timeRequired);
                        }

                        eta.setTimeRate(nextRate);
                        eta.setRemainingTime(eta.getRemainingTime() + timeAdded);

                        getRespectiveSoundEvent(world, pos, nextRate);
                        return ActionResult.SUCCESS;
                    }
                }
            } else {
                NbtCompound timeData = stack.getSubTag("timeData");
                int timeAvailable = timeData.getInt("storedTime");

                if (timeAvailable >= 20 * 30) {
                    if (!player.getAbilities().creativeMode) {
                        timeData.putInt("storedTime", timeAvailable - 20 * 30);
                    }

                    AcceleratorEntity accelerator = new AcceleratorEntity(world, pos);
                    accelerator.setTimeRate(1);
                    accelerator.setRemainingTime(20 * 30);
                    accelerator.setBoundingBox(new Box(pos));
                    world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_HARP, SoundCategory.BLOCKS, 0.5f, 0.749154f);
                    world.spawnEntity(accelerator);

                    return ActionResult.SUCCESS;
                }
            }
        }
        return super.useOnBlock(context);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient) {
            int time = Math.abs(ModConfig.TIME_SECOND);
            if (world.getTime() % time == 0) {
                NbtCompound timeData = stack.getOrCreateSubTag("timeData");
                if (timeData.getInt("storedTime") < Math.abs(ModConfig.MAX_TIME)) {
                    timeData.putInt("storedTime", timeData.getInt("storedTime") + time);
                }
            }

            if (world.getTime() % 60 == 0 && entity instanceof ServerPlayerEntity) {
                ServerPlayerEntity playerEntity = (ServerPlayerEntity) entity;
                for (int i = 0; i < playerEntity.getInventory().size(); i++) {
                    ItemStack itemStack = playerEntity.getInventory().getStack(i);
                    if (itemStack.getItem() == this && itemStack != stack) {
                        NbtCompound duplicateTimeData = itemStack.getOrCreateSubTag("timeData");
                        NbtCompound originalTimeData = stack.getOrCreateSubTag("timeData");

                        int originalTime = originalTimeData.getInt("storedTime");
                        int duplicateTime = duplicateTimeData.getInt("storedTime");

                        if (originalTime < duplicateTime) {
                            originalTimeData.putInt("storedTime", 0);
                            duplicateTimeData.putInt("storedTime", duplicateTime - time * 3);
                        }
                    }
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        int storedTime = stack.getOrCreateSubTag("timeData").getInt("storedTime");

        int storedSeconds = storedTime / 20;

        int hours = storedSeconds / 3600;
        int minutes = (storedSeconds % 3600) / 60;
        int seconds = storedSeconds % 60;
        tooltip.add(new TranslatableText("item.timeinabottle.time_in_a_bottle.tooltip", hours, minutes, seconds).formatted(Formatting.AQUA));
    }

    private void getRespectiveSoundEvent(World world, BlockPos pos, int nextRate) {
        switch (nextRate) {
            case 2:
                world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_HARP, SoundCategory.BLOCKS, 0.5F, 0.793701F);
                break;
            case 4:
            case 32:
                world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_HARP, SoundCategory.BLOCKS, 0.5F, 0.890899F);
                break;
            case 8:
                world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_HARP, SoundCategory.BLOCKS, 0.5F, 1.059463F);
                break;
            case 16:
                world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_HARP, SoundCategory.BLOCKS, 0.5F, 0.943874F);
                break;
            default:
                world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_HARP, SoundCategory.BLOCKS, 0.5F, 1F);
        }
    }
}
