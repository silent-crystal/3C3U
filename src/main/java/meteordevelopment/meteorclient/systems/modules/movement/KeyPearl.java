/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class KeyPearl extends Module {
    public KeyPearl() {
        super(Categories.Movement, "key-pearl", "一键珍珠");
    }

    private boolean isUsing;
    private boolean wasHeld;
    private int itemSlot;
    private int selectedSlot;

    @Override
    public void onDeactivate() {
        stopIfUsing();
    }

    @Override
    public void onActivate() {
        FindItemResult result = InvUtils.find(Items.ENDER_PEARL);

        selectedSlot = mc.player.getInventory().selectedSlot;
        itemSlot = result.slot();
        wasHeld = result.isMainHand();

        if (!wasHeld) {
            InvUtils.quickSwap().fromId(selectedSlot).to(itemSlot);
        }

        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        swapBack();
        toggle();
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
}
