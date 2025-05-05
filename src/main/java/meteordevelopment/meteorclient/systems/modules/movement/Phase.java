/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.Aura;
import meteordevelopment.meteorclient.systems.modules.player.InventoryTweaks;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class Phase extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> pitch = sgGeneral.add(new IntSetting.Builder().name("pitch").defaultValue(88).min(-90).sliderMax(90)
        .description("晓骇没汉化").build());

    public Phase() {
        super(Categories.Movement, "phase", "珍珠卡墙");
    }

    private boolean isUsing;
    private boolean wasHeld;
    private int itemSlot;
    private int selectedSlot;
    private float prevPitch;
    private boolean auraActive;

    @Override
    public void onDeactivate() {
        stopIfUsing();
        MeteorClient.mc.player.setPitch(prevPitch);
    }

    @Override
    public void onActivate() {
        prevPitch = MeteorClient.mc.player.getPitch();
        FindItemResult result = InvUtils.find(Items.ENDER_PEARL);

        selectedSlot = mc.player.getInventory().selectedSlot;
        itemSlot = result.slot();
        wasHeld = result.isMainHand();

        if (!wasHeld) {
            InvUtils.quickSwap().fromId(selectedSlot).to(itemSlot);
        }

        if(Modules.get().get(Aura.class).isActive()) {
            Modules.get().get(Aura.class).toggle();
            auraActive = true;
        }
        MeteorClient.mc.player.setPitch(pitch.get());
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        swapBack();
        if(auraActive){
            Modules.get().get(Aura.class).toggle();
            auraActive = false;
        }
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
