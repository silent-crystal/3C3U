/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.screens.ModulesScreen;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;


import java.util.HashMap;
import java.util.Map;

public class AutoArmor extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> noMove = sgGeneral.add(new BoolSetting.Builder().name("no-move").defaultValue(false)
        .description("晓骇没翻译").build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").defaultValue(3).min(0).sliderMax(10)
        .description("换上装备盔甲各部分的延迟").build());
    private final Setting<Boolean> autoElytra = sgGeneral.add(new BoolSetting.Builder().name("auto-elytra").defaultValue(false)
        .description("晓骇没翻译").build());
    private final Setting<Boolean> snowBug = sgGeneral.add(new BoolSetting.Builder().name("snow-bug").defaultValue(false)
        .description("晓骇没翻译").build());
    private final Setting<Boolean> invSync = sgGeneral.add(new BoolSetting.Builder().name("snow-bug").defaultValue(false)
        .description("晓骇没翻译").build());

    private int tickDelay = 0;
    private final Object2IntMap<RegistryEntry<Enchantment>> enchantments = new Object2IntOpenHashMap<>();

    public AutoArmor() {
        super(Categories.Player, "auto-armor", "Automatically equips armor.");
    }

    @EventHandler
    private void onUpdate(TickEvent.Post event){
        if (mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen) && !(mc.currentScreen instanceof InventoryScreen) && !(mc.currentScreen instanceof ModulesScreen)) return;

        if (mc.player.playerScreenHandler != mc.player.currentScreenHandler) return;

        if ((mc.player.input.movementForward != 0.0 || mc.player.input.movementSideways != 0.0) && noMove.get()) return;

        if (tickDelay > 0) {
            tickDelay--;
            return;
        }

        tickDelay = delay.get();

        Map<EquipmentSlot, int[]> armorMap = new HashMap<>(4);
        armorMap.put(EquipmentSlot.FEET, new int[]{36, getProtection(mc.player.getInventory().getStack(36)), -1, -1});
        armorMap.put(EquipmentSlot.LEGS, new int[]{37, getProtection(mc.player.getInventory().getStack(37)), -1, -1});
        armorMap.put(EquipmentSlot.CHEST, new int[]{38, getProtection(mc.player.getInventory().getStack(38)), -1, -1});
        armorMap.put(EquipmentSlot.HEAD, new int[]{39, getProtection(mc.player.getInventory().getStack(39)), -1, -1});
        for (int s = 0; s < 36; s++) {
            if (!(mc.player.getInventory().getStack(s).getItem() instanceof ArmorItem) && mc.player.getInventory().getStack(s).getItem() != Items.ELYTRA) continue;
            int protection = getProtection(mc.player.getInventory().getStack(s));
            EquipmentSlot slot = (mc.player.getInventory().getStack(s).getItem() instanceof ElytraItem ? EquipmentSlot.CHEST : ((ArmorItem) mc.player.getInventory().getStack(s).getItem()).getSlotType());
            for (Map.Entry<EquipmentSlot, int[]> e : armorMap.entrySet()) {
                if (e.getKey() == EquipmentSlot.FEET) {
                    if (mc.player.hurtTime > 1 && snowBug.get()) {
                        if (!mc.player.getInventory().getStack(36).isEmpty() && mc.player.getInventory().getStack(36).getItem() == Items.LEATHER_BOOTS) continue;
                        if (!mc.player.getInventory().getStack(s).isEmpty() && mc.player.getInventory().getStack(s).getItem() == Items.LEATHER_BOOTS) {
                            e.getValue()[2] = s;
                            continue;
                        }
                    }
                }
                if (autoElytra.get() && (ChestSwap.INSTANCE.isActive()) && e.getKey() == EquipmentSlot.CHEST) {
                    if (!mc.player.getInventory().getStack(38).isEmpty() && mc.player.getInventory().getStack(38).getItem() instanceof ElytraItem && ElytraItem.isUsable(mc.player.getInventory().getStack(38))) continue;
                    if (e.getValue()[2] != -1 && !mc.player.getInventory().getStack(e.getValue()[2]).isEmpty() && mc.player.getInventory().getStack(e.getValue()[2]).getItem() instanceof ElytraItem && ElytraItem.isUsable(mc.player.getInventory().getStack(e.getValue()[2]))) continue;
                    if (!mc.player.getInventory().getStack(s).isEmpty() && mc.player.getInventory().getStack(s).getItem() instanceof ElytraItem && ElytraItem.isUsable(mc.player.getInventory().getStack(s))) e.getValue()[2] = s;
                    continue;
                }
                if (protection > 0) {
                    if (e.getKey() == slot) {
                        if (protection > e.getValue()[1] && protection > e.getValue()[3]) {
                            e.getValue()[2] = s;
                            e.getValue()[3] = protection;
                        }
                    }
                }
            }
        }

        for (Map.Entry<EquipmentSlot, int[]> equipmentSlotEntry : armorMap.entrySet()) {
            if (equipmentSlotEntry.getValue()[2] != -1) {
                if (equipmentSlotEntry.getValue()[1] == -1 && equipmentSlotEntry.getValue()[2] < 9) {
                    Modules.get().get(InventoryTweaks.class).toggle();
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 36 + equipmentSlotEntry.getValue()[2], 1, SlotActionType.QUICK_MOVE, mc.player);
                    syncInv();
                } else if (mc.player.playerScreenHandler == mc.player.currentScreenHandler) {
                    Modules.get().get(InventoryTweaks.class).toggle();
                    int armorSlot = (equipmentSlotEntry.getValue()[0] - 34) + (39 - equipmentSlotEntry.getValue()[0]) * 2;
                    int newArmorSlot = equipmentSlotEntry.getValue()[2] < 9 ? 36 + equipmentSlotEntry.getValue()[2] : equipmentSlotEntry.getValue()[2];
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, newArmorSlot, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, armorSlot, 0, SlotActionType.PICKUP, mc.player);
                    if (equipmentSlotEntry.getValue()[1] != -1) mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, newArmorSlot, 0, SlotActionType.PICKUP, mc.player);
                    syncInv();
                }
                Modules.get().get(InventoryTweaks.class).toggle();
                return;
            }
        }
    }

    private int getProtection(ItemStack is) {
        if (is.getItem() instanceof ArmorItem || is.getItem() == Items.ELYTRA) {
            int prot = 0;

            if (is.getItem() instanceof ElytraItem) {
                if (!ElytraItem.isUsable(is))
                    return 0;
                prot = 1;
            }
            if (is.hasEnchantments()) {
                prot += Utils.getEnchantmentLevel(enchantments, Enchantments.PROTECTION);
            }
            return (is.getItem() instanceof ArmorItem ? ((ArmorItem) is.getItem()).getProtection() : 0) + prot;
        } else if (!is.isEmpty()) {
            return 0;
        }

        return -1;
    }

    private void syncInv(){
        if(invSync.get()) mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
    }
}
