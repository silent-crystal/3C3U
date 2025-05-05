/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.screens.ModulesScreen;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Timer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

import static meteordevelopment.meteorclient.systems.modules.player.SpeedMine.switchToSlot;

public class Offhand extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> mainHand = sgGeneral.add(new BoolSetting.Builder().name("mainhand").defaultValue(false)
        .description("求翻译").build());
    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder().name("health").defaultValue(0).min(0).sliderMax(20)
        .description("求翻译").build());
    private final Setting<Boolean> gapple = sgGeneral.add(new BoolSetting.Builder().name("gapple").defaultValue(false)
        .description("求翻译").build());
    private final Setting<Boolean> invSync = sgGeneral.add(new BoolSetting.Builder().name("inv-sync").defaultValue(false)
        .description("求翻译").build());
    int totems = 0;
    private final Timer timer = new Timer();

    @Override
    public String getInfoString() {
        return String.valueOf(totems);
    }

    public Offhand() {
        super(Categories.Player, "offhand", "Allows you to hold specified items in your offhand.");
    }

    @EventHandler
    private void onUpdate(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        totems = InvUtils.getItemCount(Items.TOTEM_OF_UNDYING);
        if (mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen) && !(mc.currentScreen instanceof InventoryScreen) && !(mc.currentScreen instanceof ModulesScreen) && !(mc.currentScreen instanceof GameMenuScreen)) {
            return;
        }
        if (!timer.passedMs(200)) return;
        if (gapple.get() && !mainHand.get() && mc.player.getMainHandStack().getItem() instanceof SwordItem && mc.options.useKey.isPressed() && (mc.player.getHealth() + mc.player.getAbsorptionAmount() >= health.get())) {
            if (mc.player.getOffHandStack().getItem() != Items.ENCHANTED_GOLDEN_APPLE && mc.player.getOffHandStack().getItem() != Items.GOLDEN_APPLE) {
                int itemSlot = findItemInventorySlot(Items.ENCHANTED_GOLDEN_APPLE);
                if (itemSlot == -1) {
                    itemSlot = findItemInventorySlot(Items.GOLDEN_APPLE);
                }
                if (itemSlot != -1) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
                    syncInv();
                    timer.reset();
                }
            }
            return;
        }
        if (mc.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING || mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;
        int itemSlot = findItemInventorySlot(Items.TOTEM_OF_UNDYING);
        if (itemSlot != -1) {
            if (mainHand.get()) {
                switchToSlot(0);
                if (mc.player.getInventory().getStack(0).getItem() != Items.TOTEM_OF_UNDYING) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 36, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
                }
            } else {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
            }
            syncInv();
            timer.reset();
        }
    }

    public static int findItemInventorySlot(Item item) {
        for (int i = 44; i >= 0; --i) {
            ItemStack stack = MeteorClient.mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) return i < 9 ? i + 36 : i;
        }
        return -1;
    }

    private void syncInv(){
        if(invSync.get()) mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
    }
}
