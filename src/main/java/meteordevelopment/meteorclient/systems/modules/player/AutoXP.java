/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.render.Ambience;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.player.Timer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;

import java.util.Map;

import static meteordevelopment.meteorclient.systems.modules.player.SpeedMine.*;

public class AutoXP extends Module {
    public AutoXP() {
        super(Categories.Player, "auto-xp", "自动经验瓶");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> down = sgGeneral.add(new BoolSetting.Builder().name("down").defaultValue(false)
        .description("晓骇没汉化").build());
    public final Setting<Boolean> onlyBroken = sgGeneral.add(new BoolSetting.Builder().name("only-broken").defaultValue(false)
        .description("晓骇没汉化").build());
    private final Setting<Boolean> usingPause = sgGeneral.add(new BoolSetting.Builder().name("using-pause").defaultValue(false)
        .description("晓骇没汉化").build());

    private boolean isUsing;
    private boolean wasHeld;
    private int itemSlot;
    private int selectedSlot;
    private float prevPitch;

    @Override
    public void onActivate() {
        prevPitch = mc.player.getPitch();
    }

    @Override
    public void onDeactivate() {
        mc.player.setPitch(prevPitch);
        stopIfUsing();
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if(checkThrow()) {
            if(down.get()) mc.player.setPitch(90);
            FindItemResult result = InvUtils.find(Items.EXPERIENCE_BOTTLE);

            selectedSlot = mc.player.getInventory().selectedSlot;
            itemSlot = result.slot();
            wasHeld = result.isMainHand();

            if (!wasHeld) {
                InvUtils.quickSwap().fromId(selectedSlot).to(itemSlot);
            }

            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            swapBack();
        }
    }

    private void stopIfUsing() {
        if (isUsing) {
            swapBack();
            mc.options.useKey.setPressed(false);
            isUsing = false;
        }
    }

    void swapBack() {
        if (wasHeld) return;
        InvUtils.quickSwap().fromId(selectedSlot).to(itemSlot);
    }

    public boolean checkThrow() {
        if (!isActive()) return false;
        if (mc.currentScreen != null) return false;
        if (usingPause.get() && mc.player.isUsingItem()) {
            return false;
        }
        if (onlyBroken.get()) {
            DefaultedList<ItemStack> armors = mc.player.getInventory().armor;
            for (ItemStack armor : armors) {
                if (armor.isEmpty()) continue;
                if (getDamagePercent(armor) < 100) return true;
                if (EnchantmentHelper.getLevel(getEntry(Enchantments.MENDING),armor) > 0) return false;
                return false;
            }
        } else {
            return true;
        }
        return false;
    }

    private static int getDamagePercent(ItemStack stack) {
        if (stack.getDamage() == stack.getMaxDamage()) return 100;
        return (int) ((stack.getMaxDamage() - stack.getDamage()) / Math.max(0.1, stack.getMaxDamage()) * 100.0f);
    }
}
