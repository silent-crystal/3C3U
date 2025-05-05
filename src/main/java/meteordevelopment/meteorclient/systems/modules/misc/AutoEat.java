/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.entity.player.ItemUseCrosshairTargetEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.Aura;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

public class AutoEat extends Module {
    private static final Class<? extends Module>[] AURAS = new Class[]{Aura.class};

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgThreshold = settings.createGroup("Threshold");

    // General

    private final Setting<List<Item>> blacklist = sgGeneral.add(new ItemListSetting.Builder()
        .name("blacklist")
        .description("食物选择")
        .defaultValue(
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.GOLDEN_APPLE,
            Items.CHORUS_FRUIT,
            Items.POISONOUS_POTATO,
            Items.PUFFERFISH,
            Items.CHICKEN,
            Items.ROTTEN_FLESH,
            Items.SPIDER_EYE,
            Items.SUSPICIOUS_STEW
        )
        .filter(item -> item.getComponents().get(DataComponentTypes.FOOD) != null)
        .build()
    );

    private final Setting<Boolean> pauseAuras = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-auras")
        .description("进食时停止所有功能")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseBaritone = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-baritone")
        .description("进食时停止男中音的功能")
        .defaultValue(true)
        .build()
    );

    // Threshold

    private final Setting<ThresholdMode> thresholdMode = sgThreshold.add(new EnumSetting.Builder<ThresholdMode>()
        .name("threshold-mode")
        .description("自动进食间隔")
        .defaultValue(ThresholdMode.Any)
        .build()
    );

    private final Setting<Double> healthThreshold = sgThreshold.add(new DoubleSetting.Builder()
        .name("health-threshold")
        .description("饱食度选择")
        .defaultValue(10)
        .range(1, 19)
        .sliderRange(1, 19)
        .visible(() -> thresholdMode.get() != ThresholdMode.Hunger)
        .build()
    );

    private final Setting<Integer> hungerThreshold = sgThreshold.add(new IntSetting.Builder()
        .name("hunger-threshold")
        .description("饥饿等级")
        .defaultValue(16)
        .range(1, 19)
        .sliderRange(1, 19)
        .visible(() -> thresholdMode.get() != ThresholdMode.Health)
        .build()
    );

    public boolean eating;
    private int slot, prevSlot;

    private final List<Class<? extends Module>> wasAura = new ArrayList<>();
    private boolean wasBaritone = false;

    public AutoEat() {
        super(Categories.Misc, "auto-eat", "自动进食");
    }

    @Override
    public void onDeactivate() {
        if (eating) stopEating();
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onTick(TickEvent.Pre event) {

        if (eating) {
            // If we are eating check if we should still be eating
            if (shouldEat()) {
                // Check if the item in current slot is not food
                if (mc.player.getInventory().getStack(slot).get(DataComponentTypes.FOOD) != null) {
                    // If not try finding a new slot
                    int slot = findSlot();

                    // If no valid slot was found then stop eating
                    if (slot == -1) {
                        stopEating();
                        return;
                    }
                    // Otherwise change to the new slot
                    else {
                        changeSlot(slot);
                    }
                }

                // Continue eating
                eat();
            }
            // If we shouldn't be eating anymore then stop
            else {
                stopEating();
            }
        } else {
            // If we are not eating check if we should start eating
            if (shouldEat()) {
                // Try to find a valid slot
                slot = findSlot();

                // If slot was found then start eating
                if (slot != -1) startEating();
            }
        }
    }

    @EventHandler
    private void onItemUseCrosshairTarget(ItemUseCrosshairTargetEvent event) {
        if (eating) event.target = null;
    }

    private void startEating() {
        prevSlot = mc.player.getInventory().selectedSlot;
        eat();

        // Pause auras
        wasAura.clear();
        if (pauseAuras.get()) {
            for (Class<? extends Module> klass : AURAS) {
                Module module = Modules.get().get(klass);

                if (module.isActive()) {
                    wasAura.add(klass);
                    module.toggle();
                }
            }
        }

        // Pause baritone
        if (pauseBaritone.get() && PathManagers.get().isPathing() && !wasBaritone) {
            wasBaritone = true;
            PathManagers.get().pause();
        }
    }

    private void eat() {
        changeSlot(slot);
        setPressed(true);
        if (!mc.player.isUsingItem()) Utils.rightClick();

        eating = true;
    }

    private void stopEating() {
        changeSlot(prevSlot);
        setPressed(false);

        eating = false;

        // Resume auras
        if (pauseAuras.get()) {
            for (Class<? extends Module> klass : AURAS) {
                Module module = Modules.get().get(klass);

                if (wasAura.contains(klass) && !module.isActive()) {
                    module.toggle();
                }
            }
        }

        // Resume baritone
        if (pauseBaritone.get() && wasBaritone) {
            wasBaritone = false;
            PathManagers.get().resume();
        }
    }

    private void setPressed(boolean pressed) {
        mc.options.useKey.setPressed(pressed);
    }

    private void changeSlot(int slot) {
        InvUtils.swap(slot, false);
        this.slot = slot;
    }

    public boolean shouldEat() {
        boolean health = mc.player.getHealth() <= healthThreshold.get();
        boolean hunger = mc.player.getHungerManager().getFoodLevel() <= hungerThreshold.get();

        return thresholdMode.get().test(health, hunger);
    }

    private int findSlot() {
        int slot = -1;
        int bestHunger = -1;

        for (int i = 0; i < 9; i++) {
            // Skip if item isn't food
            Item item = mc.player.getInventory().getStack(i).getItem();
            FoodComponent foodComponent = item.getComponents().get(DataComponentTypes.FOOD);
            if (foodComponent == null) continue;

            // Check if hunger value is better
            int hunger = foodComponent.nutrition();
            if (hunger > bestHunger) {
                // Skip if item is in blacklist
                if (blacklist.get().contains(item)) continue;

                // Select the current item
                slot = i;
                bestHunger = hunger;
            }
        }

        Item offHandItem = mc.player.getOffHandStack().getItem();
        if (offHandItem.getComponents().get(DataComponentTypes.FOOD) != null && !blacklist.get().contains(offHandItem) && offHandItem.getComponents().get(DataComponentTypes.FOOD).nutrition() > bestHunger)
            slot = SlotUtils.OFFHAND;

        return slot;
    }

    public enum ThresholdMode {
        Health((health, hunger) -> health),
        Hunger((health, hunger) -> hunger),
        Any((health, hunger) -> health || hunger),
        Both((health, hunger) -> health && hunger);

        private final BiPredicate<Boolean, Boolean> predicate;

        ThresholdMode(BiPredicate<Boolean, Boolean> predicate) {
            this.predicate = predicate;
        }

        public boolean test(boolean health, boolean hunger) {
            return predicate.test(health, hunger);
        }
    }
}
